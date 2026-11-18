package com.cinnamon.service;

import com.cinnamon.enums.OtpResult;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;

/**
 * OtpTokenService - OTP không lưu ở đâu cả (stateless), dựa trên JWT.
 *
 * Luồng:
 * 1. {@link #issue(String)} sinh mã 6 số và một "otpToken" (JWT) chứa email + HMAC của (email + mã) + hạn 5 phút.
 *    Mã được gửi qua email, token trả về cho frontend giữ.
 * 2. Khi đăng ký, frontend gửi lại token cùng mã người dùng nhập; {@link #verify} kiểm tra chữ ký,
 *    hạn dùng, email khớp và HMAC khớp.
 *
 * An toàn: token không chứa mã gốc, chỉ chứa HMAC có khóa bí mật của server nên không thể dò ngược mã từ token.
 * Khóa ký OTP được tách riêng khỏi khóa JWT đăng nhập, nên otpToken không thể dùng để đăng nhập.
 */
@Component
public class OtpTokenService {

    public static final Duration TTL = Duration.ofMinutes(5);

    /** Kết quả phát hành: mã gửi qua email, token trả cho client. */
    public record Issued(String otp, String token) {}

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public OtpTokenService(@Value("${app.jwt.secret}") String jwtSecret) throws Exception {
        // Khóa riêng cho OTP, dẫn xuất từ secret hiện có
        byte[] derived = MessageDigest.getInstance("SHA-256")
                .digest((jwtSecret + ":otp").getBytes(StandardCharsets.UTF_8));
        this.key = Keys.hmacShaKeyFor(derived);
    }

    public Issued issue(String email) {
        String normalized = normalize(email);
        String otp = String.format("%06d", random.nextInt(1_000_000));
        String token = Jwts.builder()
                .subject(normalized)
                .claim("h", hash(normalized, otp))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TTL.toMillis()))
                .signWith(key)
                .compact();
        return new Issued(otp, token);
    }

    public OtpResult verify(String token, String email, String otp) {
        if (token == null || token.isBlank()) return OtpResult.INVALID;
        try {
            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String normalized = normalize(email);
            if (!normalized.equals(claims.getSubject())) return OtpResult.INVALID;

            String expected = claims.get("h", String.class);
            String actual = hash(normalized, otp == null ? "" : otp.trim());
            boolean match = expected != null && MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
            return match ? OtpResult.OK : OtpResult.WRONG;
        } catch (ExpiredJwtException e) {
            return OtpResult.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            return OtpResult.INVALID;
        }
    }

    private String hash(String email, String otp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getEncoded(), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((email + ":" + otp).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tính HMAC cho OTP", e);
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}

