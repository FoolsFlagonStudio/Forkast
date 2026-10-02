package com.forkast.backend.auth;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.forkast.backend.common.TokenHasher;
import com.forkast.backend.common.exception.ApiException;
import com.forkast.backend.user.User;

@Service
public class AuthCodeService {

    private final AuthCodeRepository authCodeRepository;
    private final AuthProperties authProperties;

    public AuthCodeService(AuthCodeRepository authCodeRepository, AuthProperties authProperties) {
        this.authCodeRepository = authCodeRepository;
        this.authProperties = authProperties;
    }

    /**
     * Creates a fresh code, replacing any earlier one of the same type. Returns the
     * raw code to email.
     */
    @Transactional
    public String issue(User user, AuthCodeType type, String newEmail) {
        authCodeRepository.deleteByUserIdAndType(user.getId(), type);

        String rawCode = TokenHasher.randomDigits(6);
        Instant expiresAt = Instant.now().plus(authProperties.codeTtl());
        authCodeRepository.save(new AuthCode(user, type, TokenHasher.sha256(rawCode), expiresAt, newEmail));

        return rawCode;
    }

    /**
     * Checks a submitted code. On success the code is deleted and returned.
     * On failure the attempt is counted and an ApiException is thrown.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)
    public AuthCode consume(User user, AuthCodeType type, String submittedCode) {
        AuthCode code = authCodeRepository.findByUserIdAndType(user.getId(), type)
                .orElseThrow(AuthCodeService::invalidCode);

        if (code.isExpired(Instant.now()) || code.isLocked(authProperties.codeMaxAttempts())) {
            throw invalidCode();
        }

        if (!TokenHasher.matches(submittedCode.trim(), code.getCodeHash())) {
            code.recordFailedAttempt();
            throw invalidCode();
        }

        authCodeRepository.delete(code);
        return code;
    }

    private static ApiException invalidCode() {
        return ApiException.badRequest("That code is invalid or has expired. Request a new one.");
    }
}