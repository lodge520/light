package com.genius.smartlight.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtTokenService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-millis}")
    private long expireMillis;

    public String createToken(Long userId, String username) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireMillis);

        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withIssuedAt(now)
                .withExpiresAt(expireAt)
                .sign(Algorithm.HMAC256(secret));
    }

    public LoginUser parseToken(String token) {
        var decoded = JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
        Long userId = decoded.getClaim("userId").asLong();
        String username = decoded.getClaim("username").asString();
        return new LoginUser(userId, username);
    }
}
