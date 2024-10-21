package com.yfmf.footlog.domain.auth.jwt;

import com.yfmf.footlog.domain.auth.dto.LoginedInfo;
import com.yfmf.footlog.domain.member.domain.Authority;
import com.yfmf.footlog.domain.member.domain.Member;
import com.yfmf.footlog.domain.member.dto.MemberResponseDTO;
import com.yfmf.footlog.domain.member.repository.MemberRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JWTTokenProvider {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORITIES_KEY = "auth";
    private static final String BEARER_TYPE = "Bearer";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final String CLAIM_TYPE = "type";

    private static final long ACCESS_TOKEN_LIFETIME = 60 * 60 * 1000L; // 60 분
    private static final long REFRESH_TOKEN_LIFETIME = 3 * 24 * 60 * 60 * 1000L; // 3 days

    private final Key secretKey;
    private final MemberRepository memberRepository;

    public JWTTokenProvider(@Value("${jwt.secret}") String secretKey, MemberRepository memberRepository) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.memberRepository = memberRepository;
    }

    // 권한 정보를 추출하는 공통 메서드
    private String extractAuthorities(Collection<? extends GrantedAuthority> grantedAuthorities) {
        return grantedAuthorities.stream()
                .map(authority -> {
                    String role = authority.getAuthority();
                    if (!role.startsWith("ROLE_")) {
                        return "ROLE_" + role; // ROLE_ 접두사 없을 때만 추가
                    }
                    return role; // 이미 ROLE_가 있을 경우 그대로 반환
                })
                .collect(Collectors.joining(","));
    }

    // Principal에서 LoginedInfo 객체를 생성하는 메서드
    private LoginedInfo createLoginedInfo(Authentication authentication) {
        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            org.springframework.security.core.userdetails.User springUser =
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

            String email = springUser.getUsername();
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("회원이 존재하지 않습니다."));

            return new LoginedInfo(member.getId(), member.getName(), member.getEmail(), member.getAuthority());
        }

        throw new RuntimeException("알 수 없는 principal 타입입니다.");
    }

    public MemberResponseDTO.authTokenDTO generateToken(Authentication authentication) {
        LoginedInfo loginedInfo = createLoginedInfo(authentication);
        return generateToken(loginedInfo.getEmail(), loginedInfo.getUserId(), loginedInfo.getName(), authentication.getAuthorities());
    }

    public MemberResponseDTO.authTokenDTO generateToken(String email, Long userId, String name, Collection<? extends GrantedAuthority> grantedAuthorities) {
        String authorities = extractAuthorities(grantedAuthorities);
        Date now = new Date();

        String accessToken = Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("name", name)
                .claim(AUTHORITIES_KEY, authorities)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_LIFETIME))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_LIFETIME))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        // userId를 추가하여 반환
        return new MemberResponseDTO.authTokenDTO(
                BEARER_TYPE,
                accessToken,
                ACCESS_TOKEN_LIFETIME,
                refreshToken,
                REFRESH_TOKEN_LIFETIME,
                userId // userId 전달
        );
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.info("JWT Token 검증 실패: {}", e.getMessage());
        }
        return false;
    }

    public Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT Token 입니다.", e);
            return e.getClaims();
        }
    }

    // 토큰에서 권한 정보를 추출하고, 권한 문자열을 다루는 부분 수정
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String authoritiesClaim = claims.get(AUTHORITIES_KEY, String.class);
        if (authoritiesClaim == null || authoritiesClaim.isEmpty()) {
            throw new RuntimeException("권한 정보가 없는 Token 입니다.");
        }

        // authoritiesClaim에서 권한을 추출하고 SimpleGrantedAuthority로 변환
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(authoritiesClaim.split(","))
                .map(SimpleGrantedAuthority::new) // SimpleGrantedAuthority에 권한 추가
                .collect(Collectors.toList());

        String email = claims.getSubject();
        Long userId = claims.get("userId", Long.class);
        String name = claims.get("name", String.class);

        LoginedInfo loginedInfo = new LoginedInfo(userId, name, email, Authority.ROLE_USER); // 기본 권한 부여

        return new UsernamePasswordAuthenticationToken(loginedInfo, "", authorities);
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody().get(CLAIM_TYPE));
    }

    public String resolveToken(HttpServletRequest request, String tokenType) {
        if ("accessToken".equals(tokenType)) {
            // Access Token은 헤더에서 가져옴
            return resolveAccessTokenFromHeader(request);
        } else if ("refreshToken".equals(tokenType)) {
            // Refresh Token은 쿠키에서 가져옴
            return resolveRefreshTokenFromCookies(request);
        }
        return null;
    }

    // 헤더에서 Access Token 추출
    private String resolveAccessTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            log.info("Access Token 추출 성공: {}", bearerToken.substring(7));  // 디버그용 로그 추가
            return bearerToken.substring(7); // "Bearer " 이후의 토큰 값만 추출
        } else {
            log.warn("Authorization 헤더에 Access Token이 없습니다.");
        }
        return null;
    }

    // 쿠키에서 Refresh Token 추출
    private String resolveRefreshTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    log.info("Refresh Token 추출 성공: {}", cookie.getValue());  // 디버그용 로그 추가
                    return cookie.getValue();
                }
            }
        }
        log.warn("쿠키에 Refresh Token이 없습니다.");
        return null;
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token); // 토큰을 파싱하여 Claims 추출
        return claims.get("userId", Long.class); // Claims에서 userId 추출
    }
}