package com.ms.gateway.component;

import com.ms.gateway.properties.JwtProperties;
import com.ms.gateway.properties.RedisPrefixProperties;
import com.ms.gateway.util.RedisUtilService;
import com.ms.gateway.util.RedisUtilServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.apache.el.parser.Token;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final RedisPrefixProperties redisPrefixProperties;
    private final RedisUtilService redisUtilService;

    public Claims extractClaims(String token) {
        final String SECRET = jwtProperties.secret();
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public boolean validateToken(String token) {
        return !isTokenExpired(token);
    }

    public boolean isTokenBlacklisted(String token) {
        final String AUTH_TOKEN_BLACKLIST = redisPrefixProperties.blacklistToken();
        String key = RedisUtilServiceImpl.buildKey(AUTH_TOKEN_BLACKLIST, token);
        return redisUtilService.exists(key);
    }
}