package com.yoga.backend.common.handler;

import com.google.gson.Gson;
import com.yoga.backend.common.constants.SecurityConstants;
import com.yoga.backend.common.entity.Users;
import com.yoga.backend.common.util.JwtUtil;
import com.yoga.backend.members.UsersRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import org.springframework.stereotype.Component;

/**
 * 인증 성공 시 처리하는 핸들러
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    public CustomAuthenticationSuccessHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException {

        String email = authentication.getName();
        String role = authentication.getAuthorities().stream()
            .findFirst()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .orElse("");

        String accessToken = jwtUtil.generateAccessToken(email, role);
        String refreshToken = jwtUtil.generateRefreshToken(email);

        // 새 토큰으로 업데이트하고 이전 세션 로그아웃
        jwtUtil.updateUserTokenAndLogoutOthers(email, accessToken);

        response.setHeader(SecurityConstants.JWT_HEADER, accessToken);
        response.setHeader(SecurityConstants.REFRESH_TOKEN_HEADER, refreshToken);

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(getResponseBody(role));
    }

    private String getResponseBody(String role) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "로그인 성공");
        responseBody.put("data", role.equals("TEACHER"));

        return new Gson().toJson(responseBody);
    }
}