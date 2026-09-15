package com.jobsight.company.auth;

import com.jobsight.company.common.ApiPaths;
import com.jobsight.company.user.AppUser;
import com.jobsight.company.user.AppUserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BrowserRefreshTokenService {
    static final String COOKIE_NAME = "JOBSIGHT_REFRESH";
    private static final Duration LIFETIME = Duration.ofDays(30);

    private final BrowserRefreshTokenRepository tokens;
    private final AppUserRepository users;
    private final boolean secureCookie;
    private final Clock clock;
    private final SecureRandom random;

    @Autowired
    public BrowserRefreshTokenService(BrowserRefreshTokenRepository tokens, AppUserRepository users,
                                      @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookie) {
        this(tokens, users, secureCookie, Clock.systemUTC(), new SecureRandom());
    }

    BrowserRefreshTokenService(BrowserRefreshTokenRepository tokens, AppUserRepository users,
                               boolean secureCookie, Clock clock, SecureRandom random) {
        this.tokens = tokens;
        this.users = users;
        this.secureCookie = secureCookie;
        this.clock = clock;
        this.random = random;
    }

    /** Google 로그인 성공 시 새 절대 만료를 시작하고 이미 만료된 행을 정리한다. */
    @Transactional
    public void issue(HttpServletResponse response, UUID userId) {
        Instant now = clock.instant();
        tokens.deleteByExpiresAtBefore(now);

        AppUser user = users.findById(userId).orElseThrow();
        String raw = randomToken();
        Instant expiresAt = now.plus(LIFETIME);
        tokens.save(new BrowserRefreshToken(user, hash(raw), now, expiresAt));
        write(response, raw, expiresAt, now);
    }

    /** 성공하면 같은 절대 만료를 보존한 새 토큰을 반환 쿠키에 쓴다. */
    @Transactional
    public Optional<AppUser> rotate(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> raw = read(request);
        if (raw.isEmpty()) {
            return Optional.empty();
        }

        Optional<BrowserRefreshToken> found = tokens.findByTokenHashForUpdate(hash(raw.get()));
        if (found.isEmpty()) {
            // 다른 탭이 먼저 회전했을 수 있으므로, 뒤늦은 요청이 새 쿠키까지 지우지 않는다.
            return Optional.empty();
        }

        BrowserRefreshToken token = found.get();
        Instant now = clock.instant();
        Optional<AppUser> user = users.findById(token.ownerId())
                .filter(candidate -> candidate.getStatus().canLogIn());
        if (token.isExpired(now) || user.isEmpty()) {
            tokens.delete(token);
            clear(response);
            return Optional.empty();
        }

        String next = randomToken();
        token.rotate(hash(next), now);
        tokens.save(token);
        write(response, next, token.expiresAt(), now);
        return user;
    }

    @Transactional
    public void revoke(HttpServletRequest request, HttpServletResponse response) {
        read(request).ifPresent(raw -> tokens.deleteByTokenHash(hash(raw)));
        clear(response);
    }

    private Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void write(HttpServletResponse response, String raw, Instant expiresAt, Instant now) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE_NAME, raw)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(ApiPaths.AUTH)
                .maxAge(Duration.between(now, expiresAt))
                .build().toString());
    }

    private void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(ApiPaths.AUTH)
                .maxAge(Duration.ZERO)
                .build().toString());
    }
}
