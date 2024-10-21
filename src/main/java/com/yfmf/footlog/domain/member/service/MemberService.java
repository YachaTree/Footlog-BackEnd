package com.yfmf.footlog.domain.member.service;

import com.yfmf.footlog.domain.auth.dto.LoginedInfo;
import com.yfmf.footlog.domain.auth.jwt.JWTTokenProvider;
import com.yfmf.footlog.domain.auth.refreshToken.domain.RefreshToken;
import com.yfmf.footlog.domain.auth.refreshToken.service.RefreshTokenService;
import com.yfmf.footlog.domain.auth.utils.ClientUtils;
import com.yfmf.footlog.domain.member.domain.Authority;
import com.yfmf.footlog.domain.member.domain.Gender;
import com.yfmf.footlog.domain.member.domain.Member;
import com.yfmf.footlog.domain.member.domain.SocialType;
import com.yfmf.footlog.domain.member.dto.MemberRequestDTO;
import com.yfmf.footlog.domain.member.dto.MemberResponseDTO;
import com.yfmf.footlog.domain.member.repository.MemberRepository;
import com.yfmf.footlog.error.ApplicationException;
import com.yfmf.footlog.error.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Transactional(readOnly = false)
@RequiredArgsConstructor
@Service
public class MemberService {
    
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JWTTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    /*
        기본 회원 가입
     */
    @Transactional
    public void signUp(MemberRequestDTO.signUpDTO requestDTO) {


        // 비밀번호 확인
        checkValidPassword(requestDTO.password(), passwordEncoder.encode(requestDTO.confirmPassword()));

        // 회원 생성
        Member member = newMember(requestDTO);

        // 회원 저장
        memberRepository.save(member);

    }

    /**
       기본 로그인 - 쿠키에 토큰 저장
    */
    public MemberResponseDTO.authTokenDTO login(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, MemberRequestDTO.loginDTO requestDTO) {

        // 1. 이메일 확인
        Member member = findMemberByEmail(requestDTO.email())
                .orElseThrow(() -> new ApplicationException(ErrorCode.EMPTY_EMAIL_MEMBER, "[MemberService] not exited email"));

        // 2. 비밀번호 확인
        checkValidPassword(requestDTO.password(), member.getPassword());

        // 3. Access Token 발급
        MemberResponseDTO.authTokenDTO authTokenDTO = getAuthTokenDTO(requestDTO.email(), requestDTO.password(), httpServletRequest);

        // 4. Refresh Token을 HttpOnly 쿠키에 저장
        addTokenToCookie(httpServletResponse, "refreshToken", authTokenDTO.refreshToken());


        // 5. Refresh Token은 클라이언트에 응답하지 않고, Redis에 저장
        refreshTokenService.saveRefreshToken(member.getId().toString(), authTokenDTO.refreshToken(), authTokenDTO.refreshTokenValidTime());

        // 6. Access Token은 클라이언트가 로컬 스토리지에 저장할 수 있도록 응답 바디로 반환
        return authTokenDTO; // 여기에서는 Access Token만 클라이언트에 응답
    }

    // 쿠키에 토큰 추가
    private void addTokenToCookie(HttpServletResponse response, String tokenName, String tokenValue) {
        Cookie cookie = new Cookie(tokenName, tokenValue);
        cookie.setHttpOnly(true);  // JavaScript에서 쿠키 접근을 차단
        cookie.setSecure(false);   // HTTPS에서만 전송되도록 설정 (필요 시 true로 설정)
        cookie.setPath("/");       // 애플리케이션의 모든 경로에서 쿠키가 유효하도록 설정
        cookie.setMaxAge(7 * 24 * 60 * 60); // 쿠키 만료 시간 설정 (1주일)

        response.addCookie(cookie);
    }

    // 비밀번호 확인
    private void checkValidPassword(String rawPassword, String encodedPassword) {

        log.info("{} {}", rawPassword, encodedPassword);

        if(!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new ApplicationException(ErrorCode.INVALID_PASSWORD, "[MemberService] checkValidPassword");
        }
    }

    protected Optional<Member> findMemberByEmail(String email) {
        log.info("회원 확인 : {}", email);

        return memberRepository.findByEmail(email);
    }

    // 회원 생성
    protected Member newMember(MemberRequestDTO.signUpDTO requestDTO) {
        return Member.builder()
                .name(requestDTO.name())
                .email(requestDTO.email())
                .password(passwordEncoder.encode(requestDTO.password()))
                .gender(Gender.fromString(requestDTO.gender()))
                .socialType(SocialType.NONE)
                .authority(Authority.ROLE_USER)
                .build();
    }

    // 토큰 발급
    protected MemberResponseDTO.authTokenDTO getAuthTokenDTO(String email, String password, HttpServletRequest httpServletRequest) {

        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                = new UsernamePasswordAuthenticationToken(email, password);
        AuthenticationManager manager = authenticationManagerBuilder.getObject();
        Authentication authentication = manager.authenticate(usernamePasswordAuthenticationToken);

        // 회원 정보 조회 후 LoginedInfo 생성
        Member member = findMemberByEmail(email).orElseThrow(() -> new ApplicationException(ErrorCode.EMPTY_EMAIL_MEMBER, "[MemberService] not exited email"));
        LoginedInfo loginedInfo = new LoginedInfo(member.getId(), member.getName(), member.getEmail(), member.getAuthority());

        // 인증 객체에서 LoginedInfo로 교체
        Authentication newAuth = new UsernamePasswordAuthenticationToken(loginedInfo, authentication.getCredentials(), authentication.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        return jwtTokenProvider.generateToken(loginedInfo.getEmail(), loginedInfo.getUserId(), loginedInfo.getName(), authentication.getAuthorities());
    }


    // 토큰 재발급
    public MemberResponseDTO.authTokenDTO reissueToken(HttpServletRequest httpServletRequest) {

        // Request 쿠키에서 Refresh Token 추출
        String refreshToken = jwtTokenProvider.resolveToken(httpServletRequest, "refreshToken");

        // 리프레시 토큰 유효성 검사
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new ApplicationException(ErrorCode.FAILED_VALIDATE_REFRESH_TOKEN, "[MemberService] 리프레시 토큰이 유효하지 않거나 존재하지 않습니다.");
        }

        // type 확인: 리프레시 토큰인지 확인
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new ApplicationException(ErrorCode.IS_NOT_REFRESH_TOKEN, "[MemberService] 제공된 토큰은 리프레시 토큰이 아닙니다.");
        }

        // 저장된 리프레시 토큰 가져오기
        String storedRefreshToken = refreshTokenService.getRefreshToken(refreshToken);
        if (storedRefreshToken == null) {
            throw new ApplicationException(ErrorCode.FAILED_GET_REFRESH_TOKEN, "[MemberService] 저장된 리프레시 토큰을 찾을 수 없습니다.");
        }

        // IP 주소 확인
        String currentIp = ClientUtils.getClientIp(httpServletRequest);
        String storedIp = refreshTokenService.getStoredIp(refreshToken);
        if (!currentIp.equals(storedIp)) {
            throw new ApplicationException(ErrorCode.DIFFERENT_IP_ADDRESS, "[MemberService] 로그인한 IP와 다릅니다.");
        }

        // 저장된 리프레시 토큰과 일치하는 사용자 찾기
        Member member = refreshTokenService.getMemberByToken(storedRefreshToken)
                .orElseThrow(() -> new ApplicationException(ErrorCode.EMPTY_EMAIL_MEMBER, "[MemberService] 리프레시 토큰에 해당하는 사용자를 찾을 수 없습니다."));

        // 새로운 엑세스 토큰 발급
        MemberResponseDTO.authTokenDTO authTokenDTO = jwtTokenProvider.generateToken(
                member.getEmail(), member.getId(), member.getName(), Collections.singletonList(new SimpleGrantedAuthority(member.getAuthority().name()))
        );

        // 리프레시 토큰 갱신 후 저장
        refreshTokenService.saveRefreshToken(member.getEmail(), authTokenDTO.refreshToken(), authTokenDTO.refreshTokenValidTime());

        // 새로운 토큰 반환
        return authTokenDTO;
    }


    /*
        로그아웃
     */
    public void logout(HttpServletRequest httpServletRequest) {
        log.info("로그아웃 요청 수신");  // 로그아웃 시도 시 로그 기록

        String token = jwtTokenProvider.resolveToken(httpServletRequest, "accessToken");
        if(token == null) {
            log.error("토큰이 없습니다."); // 로그에 토큰이 없을 경우를 기록
            throw new ApplicationException(ErrorCode.FAILED_VALIDATE_ACCESS_TOKEN, "액세스 토큰이 존재하지 않습니다.");
        }
        if(!jwtTokenProvider.validateToken(token)) {
            log.error("유효하지 않은 액세스 토큰: {}", token);  // 유효하지 않은 토큰일 경우 기록
            throw new ApplicationException(ErrorCode.FAILED_VALIDATE_ACCESS_TOKEN, "유효하지 않은 액세스 토큰입니다.");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        log.info("사용자 ID: {}의 리프레시 토큰 삭제 요청", userId);  // 유저 정보와 함께 로그 출력
        refreshTokenService.deleteRefreshToken(userId.toString());

        log.info("로그아웃 성공 - 사용자 ID: {}", userId);  // 성공적인 로그아웃 후 기록
    }

    public List<MemberResponseDTO.MemberInfoDTO> getAllMembers() {
        List<Member> members = memberRepository.findAll();

        // Member 엔티티를 DTO로 변환
        return members.stream()
                .map(member -> new MemberResponseDTO.MemberInfoDTO(
                        member.getId(),
                        member.getName(),
                        member.getEmail(),
                        member.getGender(),
                        member.getAuthority())
                )
                .collect(Collectors.toList());
    }
}
