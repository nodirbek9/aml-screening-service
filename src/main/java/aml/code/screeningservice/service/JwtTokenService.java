package aml.code.screeningservice.service;

import aml.code.screeningservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtUtil jwtUtil;

    @Value("${jwt.token.secret}")
    private String tokenSecret;

    public Boolean isValid(String token) {
        return jwtUtil.isTokenValid(token, getTokenSecret());
    }

    public String generateToken(String subject) {
        return jwtUtil.jwt(new HashMap<>(), subject, getTokenSecret());
    }

    public String generateRefreshToken(String subject) {
        return jwtUtil.refreshJwt(new HashMap<>(), subject, getTokenSecret());
    }

    public String subject(String token) {
        return jwtUtil.getSubject(token, getTokenSecret());
    }

    private String getTokenSecret() {
        return tokenSecret;
    }
}