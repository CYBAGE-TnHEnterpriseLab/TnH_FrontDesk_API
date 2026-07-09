package com.pms.auth.service.impl;

import com.pms.auth.dto.AuthResponse;
import com.pms.auth.dto.LoginRequest;
import com.pms.auth.dto.LogoutRequest;
import com.pms.auth.dto.RefreshTokenRequest;
import com.pms.auth.dto.RegisterRequest;
import com.pms.auth.entity.RefreshToken;
import com.pms.auth.entity.Role;
import com.pms.auth.entity.UserAccount;
import com.pms.auth.exception.AuthException;
import com.pms.auth.mapper.AuthMapper;
import com.pms.auth.repository.RefreshTokenRepository;
import com.pms.auth.repository.RoleRepository;
import com.pms.auth.repository.UserAccountRepository;
import com.pms.auth.service.AuthService;
import com.pms.auth.util.JwtUtils;
import com.pms.auth.util.TokenHashUtils;
import com.pms.auth.validator.PasswordPolicyValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthMapper authMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!PasswordPolicyValidator.isStrong(request.getPassword())) {
            throw new AuthException("Password does not meet policy requirements");
        }
        if (userAccountRepository.existsByUsername(request.getUsername().trim())) {
            throw new AuthException("Username already exists");
        }
        if (userAccountRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new AuthException("Email already exists");
        }

        Role adminRole = roleRepository.findByNameIgnoreCase(ADMIN_ROLE)
                .orElseThrow(() -> new AuthException("Admin role is not configured"));

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        UserAccount account = authMapper.toEntity(request, encodedPassword, Set.of(adminRole));
        userAccountRepository.save(account);

        return issueAuthTokens(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount account = userAccountRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (!account.isEnabled() || !passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new AuthException("Invalid credentials");
        }

        Set<String> roleNames = extractRoleNames(account);
        if (!roleNames.contains(ADMIN_ROLE)) {
            throw new AuthException("Access denied for non-admin user");
        }

        return issueAuthTokens(account);
    }

    @Override
    @Transactional
    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {
        Claims refreshClaims;
        try {
            refreshClaims = jwtUtils.parseRefreshTokenClaims(request.getRefreshToken());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AuthException("Refresh token is invalid");
        }
        String currentTokenHash = TokenHashUtils.sha256(request.getRefreshToken());

        RefreshToken persistedToken = refreshTokenRepository.findByTokenHash(currentTokenHash)
                .orElseThrow(() -> new AuthException("Refresh token is invalid"));

        if (persistedToken.isRevoked()) {
            throw new AuthException("Refresh token has been revoked");
        }

        if (persistedToken.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new AuthException("Refresh token has expired");
        }

        UserAccount account = persistedToken.getUserAccount();
        if (!account.getUsername().equals(refreshClaims.getSubject())) {
            throw new AuthException("Refresh token subject mismatch");
        }

        Set<String> roleNames = extractRoleNames(account);
        if (!roleNames.contains(ADMIN_ROLE)) {
            throw new AuthException("Access denied for non-admin user");
        }

        String newRefreshToken = jwtUtils.generateRefreshToken(account.getUsername());
        String newRefreshHash = TokenHashUtils.sha256(newRefreshToken);
        persistedToken.setRevoked(true);
        persistedToken.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
        persistedToken.setReplacedByTokenHash(newRefreshHash);
        refreshTokenRepository.save(persistedToken);

        saveRefreshToken(account, newRefreshToken, jwtUtils.parseRefreshTokenClaims(newRefreshToken));

        return AuthResponse.builder()
                .accessToken(jwtUtils.generateAccessToken(account.getUsername(), roleNames))
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresInSeconds(jwtUtils.getAccessTokenExpirationSeconds())
                .roles(roleNames)
                .build();
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        String currentTokenHash = TokenHashUtils.sha256(request.getRefreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(currentTokenHash)
                .orElseThrow(() -> new AuthException("Refresh token is invalid"));

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
        refreshTokenRepository.save(refreshToken);
    }

    private Set<String> extractRoleNames(UserAccount account) {
        return account.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    private AuthResponse issueAuthTokens(UserAccount account) {
        Set<String> roleNames = extractRoleNames(account);
        String accessToken = jwtUtils.generateAccessToken(account.getUsername(), roleNames);
        String refreshToken = jwtUtils.generateRefreshToken(account.getUsername());

        saveRefreshToken(account, refreshToken, jwtUtils.parseRefreshTokenClaims(refreshToken));

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresInSeconds(jwtUtils.getAccessTokenExpirationSeconds())
                .roles(roleNames)
                .build();
    }

    private void saveRefreshToken(UserAccount account, String refreshToken, Claims claims) {
        RefreshToken tokenRecord = new RefreshToken();
        tokenRecord.setUserAccount(account);
        tokenRecord.setTokenHash(TokenHashUtils.sha256(refreshToken));
        tokenRecord.setExpiresAt(OffsetDateTime.ofInstant(jwtUtils.extractExpiration(claims), ZoneOffset.UTC));
        tokenRecord.setRevoked(false);
        refreshTokenRepository.save(tokenRecord);
    }
}

