package com.example.ordering_lecture.token;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;
@Component
@RequiredArgsConstructor
@Log4j2
public class JwtTokenProvider {
    @Qualifier("2")
    private final RedisTemplate<String, String> redisTemplate2;

    @Value("${jwt.secretKey}")
    private String secretKey;
    @Value("${jwt.token.recommend-expiration-time}")
    private long recommendExpirationTime;
    public String createRecommandToken(String email, String role) {
        Date now = new Date();
        String recommandToken = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + recommendExpirationTime))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .claim("role", role)
                .compact();

        // Redis에 리프레시 토큰 저장 (이메일을 키로 사용)
        String RecommandKey = "RC:" + email;
        redisTemplate2.opsForValue().set(RecommandKey, recommandToken, recommendExpirationTime, TimeUnit.MILLISECONDS);

        return recommandToken;
    }
}
