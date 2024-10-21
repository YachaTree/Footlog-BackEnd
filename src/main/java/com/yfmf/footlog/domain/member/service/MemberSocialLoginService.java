package com.yfmf.footlog.domain.member.service;


import com.yfmf.footlog.domain.auth.jwt.JWTTokenProvider;
import com.yfmf.footlog.domain.auth.refreshToken.domain.RefreshToken;
import com.yfmf.footlog.domain.auth.refreshToken.service.RefreshTokenService;
import com.yfmf.footlog.domain.auth.utils.ClientUtils;
import com.yfmf.footlog.domain.member.domain.Authority;
import com.yfmf.footlog.domain.member.domain.Gender;
import com.yfmf.footlog.domain.member.domain.Member;
import com.yfmf.footlog.domain.member.domain.SocialType;
import com.yfmf.footlog.domain.member.dto.MemberResponseDTO;
import com.yfmf.footlog.domain.member.property.KakaoProviderProperties;
import com.yfmf.footlog.domain.member.property.KakaoRegistrationProperties;
import com.yfmf.footlog.domain.member.repository.MemberRepository;
import com.yfmf.footlog.error.ApplicationException;
import com.yfmf.footlog.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.UUID;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class MemberSocialLoginService {

    private final MemberRepository memberRepository;
    private final MemberService memberService;

    private final PasswordEncoder passwordEncoder;
    private final JWTTokenProvider jwtTokenProvider;

    private final RestTemplate restTemplate = new RestTemplate();
    private final KakaoProviderProperties KakaoProviderProperties;
    private final KakaoRegistrationProperties kakaoRegistrationProperties;
    private final RefreshTokenService refreshTokenService;


    /*
        카카오 로그인
     */
    // 카카오로부터 받은 최신 사용자 정보로 데이터베이스 내의 사용자 정보를 갱신할 필요가 있을까?
    @Transactional
    public MemberResponseDTO.authTokenDTO kakaoLogin(String code, HttpServletRequest httpServletRequest) {
        // 토큰 발급
        String accessToken = generateAccessToken(code);

        // 사용자 정보
        MemberResponseDTO.KakaoInfoDTO profile = getKakaoProfile(accessToken);

        // 회원 확인
        Member member = memberService.findMemberByEmail(profile.kakaoAccount().email())
                .orElseGet(() -> kakaoSignUp(profile));

        // Access Token 및 Refresh Token 발급
        MemberResponseDTO.authTokenDTO authTokenDTO = jwtTokenProvider.generateToken(
                member.getEmail(),
                member.getId(),
                member.getName(),
                Collections.singletonList(new SimpleGrantedAuthority(member.getAuthority().name()))
        );

        return authTokenDTO;  // **클라이언트에 Access Token만 반환**
    }

    private String generateAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", kakaoRegistrationProperties.getAuthorizationGrantType());
        params.add("client_id", kakaoRegistrationProperties.getClientId());
        params.add("redirect_uri", kakaoRegistrationProperties.getRedirectUri());
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);
        ResponseEntity<MemberResponseDTO.KakaoTokenDTO> response = restTemplate.postForEntity(
                KakaoProviderProperties.getTokenUri(),
                httpEntity,
                MemberResponseDTO.KakaoTokenDTO.class
        );

        if(!response.getStatusCode().is2xxSuccessful()) {
            throw new ApplicationException(ErrorCode.FAILED_GET_ACCESS_TOKEN, "[MemberSocialLoginService] fail generateAccessToken");
        }

        return response.getBody().accessToken();
    }

    private MemberResponseDTO.KakaoInfoDTO getKakaoProfile(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(accessToken);

        ResponseEntity<MemberResponseDTO.KakaoInfoDTO> response = restTemplate.postForEntity(
                KakaoProviderProperties.getUserInfoUri(),
                new HttpEntity<>(headers),
                MemberResponseDTO.KakaoInfoDTO.class
        );

        if(!response.getStatusCode().is2xxSuccessful()) {
            throw new ApplicationException(ErrorCode.FAILED_GET_KAKAO_PROFILE, "[MemberSocialLoginService] fail getKakaoProfile");
        }

        return response.getBody();
    }

    protected Member kakaoSignUp(MemberResponseDTO.KakaoInfoDTO profile) {
        log.info("카카오 회원 생성 : " + profile.kakaoAccount().email());

        Member member = Member.builder()
                .name(profile.properties().nickname())
                .email(profile.kakaoAccount().email())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .gender(Gender.fromString(profile.kakaoAccount().gender()))
                .socialType(SocialType.KAKAO)
                .authority(Authority.ROLE_USER)
                .build();

        memberRepository.save(member);

        return member;
    }


    // Redis에 Refresh Token 저장 메서드 수정
    public void saveRefreshTokenInRedis(String userId, String refreshToken, HttpServletRequest httpServletRequest) {
        String ipAddress = ClientUtils.getClientIp(httpServletRequest);

        // userId와 IP 주소를 함께 Redis에 저장
        refreshTokenService.saveRefreshToken(userId, refreshToken, /* expirationTime */ 7L * 24 * 60 * 60 * 1000); // 7일 설정
        refreshTokenService.saveIp(userId, ipAddress);
    }
}
