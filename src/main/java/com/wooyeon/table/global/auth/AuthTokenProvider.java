package com.wooyeon.table.global.auth;

import com.wooyeon.table.domain.user.domain.model.UserRole;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * 로그인 토큰 발급·검증. 형식: base64url("userId:role:만료epoch초") + "." + base64url(HMAC-SHA256 서명).
 * 서버에 세션을 두지 않아 서버를 다시 띄워도 토큰이 유지된다 (데모 중 재배포 대비)
 */
@Component
public class AuthTokenProvider {

    private static final String ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final Duration ttl;
    private final Clock clock;

    public AuthTokenProvider(AuthProperties properties, Clock clock) {
        this.secret = properties.tokenSecret().getBytes(StandardCharsets.UTF_8);
        this.ttl = Duration.ofHours(properties.tokenTtlHours());
        this.clock = clock;
    }

    public String issue(Long userId, UserRole role) {
        long expiresAt = clock.instant().plus(ttl).getEpochSecond();
        String payload = ENCODER.encodeToString((userId + ":" + role + ":" + expiresAt)
                .getBytes(StandardCharsets.UTF_8));
        return payload + "." + sign(payload);
    }

    public AuthUser parse(String token) {
        int dot = token.indexOf('.');
        if (dot < 0 || !MessageDigest.isEqual(sign(token.substring(0, dot)).getBytes(StandardCharsets.UTF_8),
                token.substring(dot + 1).getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "토큰이 올바르지 않습니다");
        }
        try {
            String[] parts = new String(DECODER.decode(token.substring(0, dot)), StandardCharsets.UTF_8).split(":");
            if (Long.parseLong(parts[2]) < clock.instant().getEpochSecond()) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 만료됐습니다");
            }
            return new AuthUser(Long.parseLong(parts[0]), UserRole.valueOf(parts[1]));
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "토큰이 올바르지 않습니다");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return ENCODER.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("토큰 서명 실패", e);
        }
    }
}
