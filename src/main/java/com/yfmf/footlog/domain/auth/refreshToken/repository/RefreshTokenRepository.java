package com.yfmf.footlog.domain.auth.refreshToken.repository;

import com.yfmf.footlog.domain.auth.refreshToken.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByRefreshToken(String refreshToken);
}