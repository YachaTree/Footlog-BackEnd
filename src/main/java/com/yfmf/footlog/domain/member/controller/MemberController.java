package com.yfmf.footlog.domain.member.controller;


import com.yfmf.footlog.domain.auth.jwt.JWTTokenProvider;
import com.yfmf.footlog.domain.auth.refreshToken.service.RefreshTokenService;
import com.yfmf.footlog.domain.auth.utils.ApiUtils;
import com.yfmf.footlog.domain.member.dto.MemberDeleteResponseDTO;
import com.yfmf.footlog.domain.member.dto.MemberRequestDTO;
import com.yfmf.footlog.domain.member.dto.MemberResponseDTO;
import com.yfmf.footlog.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Tag(name = "회원 인증 API", description = "회원 가입, 로그인, 토큰 재발급, 로그아웃 기능을 제공하는 API")
public class MemberController {

    private final MemberService memberService;
    private final RefreshTokenService refreshTokenService;
    private final JWTTokenProvider jwtTokenProvider;

    /**
          기본 회원 가입
       */
    @Operation(summary = "회원 가입", description = "회원 가입을 처리합니다.")
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody MemberRequestDTO.signUpDTO requestDTO) {

        memberService.signUp(requestDTO);

        return ResponseEntity.ok().body(ApiUtils.success(null));
    }


    /**
         기본 로그인
      */
    @Operation(summary = "로그인", description = "회원 로그인을 처리하고 인증 토큰을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody MemberRequestDTO.loginDTO loginDTO, HttpServletRequest request, HttpServletResponse response) {
        MemberResponseDTO.authTokenDTO authTokenDTO = memberService.login(request, response, loginDTO);
        log.info("{}님이 로그인에 성공했습니다.", authTokenDTO.userId());

        // Access Token은 로컬 스토리지에 저장할 수 있도록 응답으로 반환
        return ResponseEntity.ok(authTokenDTO);  // Access Token만 바디에 반환 (Refresh Token은 HttpOnly 쿠키로 설정)
    }

    /**
       Access Token 재발급 - Refresh Token 필요
    */
    @Operation(summary = "토큰 재발급", description = "Refresh Token을 사용하여 Access Token을 재발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<?> reissueToken(HttpServletRequest httpServletRequest) {

        MemberResponseDTO.authTokenDTO responseDTO = memberService.reissueToken(httpServletRequest);

        return ResponseEntity.ok().body(ApiUtils.success(responseDTO));
    }

    /**
        로그아웃
     */
    @Operation(summary = "로그아웃", description = "Refresh Token을 사용하여 로그아웃을 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        log.info("로그아웃 시도");

        // 로그아웃 처리 (Refresh Token 확인 및 제거)
        memberService.logout(httpServletRequest);

        // Redis에서 Refresh Token 삭제
        String refreshToken = jwtTokenProvider.resolveToken(httpServletRequest, "refreshToken");
        if (refreshToken != null) {
            refreshTokenService.deleteRefreshToken(refreshToken);
        }

        // Access Token 쿠키 삭제
        Cookie accessTokenCookie = new Cookie("accessToken", null);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setMaxAge(0);  // 쿠키 만료
        accessTokenCookie.setPath("/");

        // Refresh Token 쿠키 삭제
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setMaxAge(0);  // 쿠키 만료
        refreshTokenCookie.setPath("/");

        // 쿠키 응답에 추가
        httpServletResponse.addCookie(accessTokenCookie);
        httpServletResponse.addCookie(refreshTokenCookie);

        return ResponseEntity.ok().body(ApiUtils.success(null));
    }

    /**
     * 전체회원 조회
     */
    @Operation(summary = "회원 전체 조회", description = "등록된 모든 회원을 조회합니다. (관리자만 가능)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/members")
    public ResponseEntity<?> getAllMembers() {
        List<MemberResponseDTO.MemberInfoDTO> memberList = memberService.getAllMembers();
        return ResponseEntity.ok().body(ApiUtils.success(memberList));
    }

    /**
     * Admin 계정이 회원 삭제
     * */

    @Operation(summary = "회원 삭제", description = "등록된 멤버를 삭제합니다.(관리자만 가능")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{memberId}/delete")
    public ResponseEntity<MemberDeleteResponseDTO> deleteMember(@PathVariable Long memberId) {
        memberService.deleteMember(memberId);

        MemberDeleteResponseDTO response = new MemberDeleteResponseDTO("회원 삭제가 완료되었습니다.", memberId);
        return ResponseEntity.ok(response);
    }
}
