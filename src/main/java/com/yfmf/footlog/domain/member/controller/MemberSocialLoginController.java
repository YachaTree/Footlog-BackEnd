package com.yfmf.footlog.domain.member.controller;

import com.yfmf.footlog.domain.member.dto.MemberResponseDTO;
import com.yfmf.footlog.domain.member.service.MemberSocialLoginService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Tag(name = "카카오 회원 API", description = "소셜 로그인 및 회원가입 기능을 제공하는 API")
public class MemberSocialLoginController {

    private final MemberSocialLoginService memberSocialLoginService;

    // https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=262c56061ee06d4004d2f9b94db133a4&redirect_uri=http://192.168.0.32:8080/api/auth/kakao/login
    // https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=262c56061ee06d4004d2f9b94db133a4&redirect_uri=http://172.16.17.66:8181//api/auth/kakao/login
    /*
        카카오 로그인
     */
    @GetMapping("/kakao/login")
    public ResponseEntity<?> kakaoLogin(@RequestParam(name = "code") String code, HttpServletResponse response, HttpServletRequest request) {
        // 로그인 후 토큰 발급
        MemberResponseDTO.authTokenDTO tokenDTO = memberSocialLoginService.kakaoLogin(code, request);

//        // JWT Access Token을 HttpOnly 쿠키에 저장
//        Cookie accessTokenCookie = new Cookie("accessToken", tokenDTO.accessToken());
//        accessTokenCookie.setHttpOnly(true); // HttpOnly로 설정하여 클라이언트에서 접근하지 못하도록 설정
//        accessTokenCookie.setMaxAge((int) (tokenDTO.accessTokenValidTime() / 1000));  // Access 토큰 유효 기간 설정 (초 단위)
//        accessTokenCookie.setPath("/"); // 루트 경로에서 모든 페이지에서 접근 가능
//        accessTokenCookie.setSecure(false); // HTTPS 환경에서는 true로 설정
//
//        // JWT Refresh Token을 HttpOnly 쿠키에 저장
//        Cookie refreshTokenCookie = new Cookie("refreshToken", tokenDTO.refreshToken());
//        refreshTokenCookie.setHttpOnly(true); // HttpOnly로 설정하여 클라이언트에서 접근하지 못하도록 설정
//        refreshTokenCookie.setMaxAge((int) (tokenDTO.refreshTokenValidTime() / 1000)); // Refresh 토큰 유효 기간 설정 (초 단위)
//        refreshTokenCookie.setPath("/"); // 모든 경로에서 접근 가능
//        refreshTokenCookie.setSecure(false); // HTTPS 환경에서는 true로 설정
//        // 응답에 쿠키 추가
//        response.addCookie(accessTokenCookie);
//        response.addCookie(refreshTokenCookie);

        // JWT Access Token을 HttpOnly ResponseCookie에 저장
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", tokenDTO.accessToken())
                .httpOnly(true)  // HttpOnly로 설정하여 클라이언트에서 접근하지 못하도록 설정
                .maxAge(tokenDTO.accessTokenValidTime() / 1000)  // Access 토큰 유효 기간 설정 (초 단위)
                .path("/")  // 루트 경로에서 모든 페이지에서 접근 가능
                .secure(false)  // HTTPS 환경에서는 true로 설정
                .sameSite("Lax")  // sameSite 설정
                .build();


        // JWT Refresh Token을 HttpOnly ResponseCookie에 저장
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", tokenDTO.refreshToken())
                .httpOnly(true)  // HttpOnly로 설정하여 클라이언트에서 접근하지 못하도록 설정
                .maxAge(tokenDTO.refreshTokenValidTime() / 1000)  // Refresh 토큰 유효 기간 설정 (초 단위)
                .path("/")  // 모든 경로에서 접근 가능
                .secure(false)  // HTTPS 환경에서는 true로 설정
                .sameSite("Lax")  // sameSite 설정 더 강력하게 하려면 "Strict"
                .build();



        // 응답 헤더에 쿠키 추가
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());


        headers.add("Location", "http://192.168.0.32:3000/match");  // React 클라이언트에서 처리할 수 있도록 리다이렉트


        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

}
