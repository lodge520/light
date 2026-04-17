package com.genius.smartlight.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtTokenService {

    private static final String SECRET = "smart-light-secret-2026";
    private static final long EXPIRE_MILLIS = 7L * 24 * 60 * 60 * 1000;

    public String createToken(Long userId, String username) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + EXPIRE_MILLIS);

        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withIssuedAt(now)
                .withExpiresAt(expireAt)
                .sign(Algorithm.HMAC256(SECRET));
    }

    public LoginUser parseToken(String token) {
        var decoded = JWT.require(Algorithm.HMAC256(SECRET)).build().verify(token);
        Long userId = decoded.getClaim("userId").asLong();
        String username = decoded.getClaim("username").asString();
        return new LoginUser(userId, username);
    }
}