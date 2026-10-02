package com.forkast.backend.auth;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.common.TokenHasher;
import com.forkast.backend.common.exception.ApiException;
import com.forkast.backend.user.User;

@Service
public class TokenService {

    /**
     * What a login hands back: the signed JWT, when it expires, and the raw refresh
     * token.
     */
    public record IssuedTokens(String accessToken, Instant accessTokenExpiresAt, String refreshToken) {
    }

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProperties authProperties;

    public TokenService(JwtEncoder jwtEncoder,
            RefreshTokenRepository refreshTokenRepository,
            AuthProperties authProperties) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authProperties = authProperties;
    }

    /** Signs a new access token and stores a new refresh token for one device. */
    @Transactional
    public IssuedTokens issueTokens(User user) {
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(authProperties.accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(accessExpiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        String rawRefreshToken = TokenHasher.randomHex(32);
        Instant refreshExpiresAt = now.plus(authProperties.refreshTokenTtl());
        refreshTokenRepository.save(new RefreshToken(user, TokenHasher.sha256(rawRefreshToken), refreshExpiresAt));

        return new IssuedTokens(accessToken, accessExpiresAt, rawRefreshToken);
    }

    /**
     * Validates a refresh token and deletes it (rotation). Returns its user so new
     * tokens can be issued.
     */
    @Transactional
    public User consumeRefreshToken(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawRefreshToken))
                .filter(token -> !token.isExpired(Instant.now()))
                .orElseThrow(() -> ApiException.unauthorized("Your session has expired. Please log in again."));

        refreshTokenRepository.delete(stored);
        return stored.getUser();
    }

    /** Ends one device's session. Unknown tokens are ignored. */
    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawRefreshToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    /** Ends every session for a user (password reset/update, account deletion). */
    @Transactional
    public void revokeAllSessions(User user) {
        refreshTokenRepository.deleteAllByUserId(user.getId());
    }
}