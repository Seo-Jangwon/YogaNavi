package com.yoga.backend.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoga.backend.common.constants.SecurityConstants;
import com.yoga.backend.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class JWTTokenValidatorFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> data = new HashMap<>();

    private final JwtUtil jwtUtil;

    public JWTTokenValidatorFilter( JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String jwt = request.getHeader(SecurityConstants.JWT_HEADER);
        String refreshToken = request.getHeader(SecurityConstants.REFRESH_TOKEN_HEADER);

        if (null != jwt && jwt.startsWith("Bearer ")) {
            jwt = jwtUtil.extractToken(jwt);
            try {
                JwtUtil.TokenStatus tokenStatus = jwtUtil.isTokenValid(jwt);
                switch (tokenStatus) {
                    case VALID:
                        Claims claims = jwtUtil.validateToken(jwt);
                        String email = String.valueOf(claims.get("email"));
                        String authorities = (String) claims.get("role");
                        Authentication auth = new UsernamePasswordAuthenticationToken(email, null,
                            AuthorityUtils.commaSeparatedStringToAuthorityList(authorities));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        filterChain.doFilter(request, response);
                        break;
                    case INVALID:
                        sendUnauthorizedResponse(response, "다른 기기에서 로그인되어 세션이 만료되었습니다.");
                        break;
                    case NOT_FOUND:
                        sendUnauthorizedResponse(response, "세션이 만료되었습니다. 다시 로그인해주세요.");
                        break;
                }

            } catch (ExpiredJwtException e) {
                if (null == refreshToken) {
                    sendUnauthorizedResponse(response, "리프레시 토큰 요청");
                } else {
                    handleRefreshToken(request, response, filterChain, refreshToken);
                }
            } catch (JwtException e) {
                log.error("액세스 토큰 처리 불가 {}",e);
                sendUnauthorizedResponse(response, "액세스 토큰 처리 불가");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private void handleRefreshToken(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain, String refreshToken) throws IOException, ServletException {
        if (refreshToken != null) {
            try {
                Claims refreshClaims = jwtUtil.validateToken(refreshToken);

                String email = (String) refreshClaims.get("email");
                String role = (String) refreshClaims.get("role");

                String newAccessToken = jwtUtil.generateAccessToken(email, role);

                response.setStatus(HttpServletResponse.SC_CREATED);
                response.setHeader(SecurityConstants.JWT_HEADER, newAccessToken);
                filterChain.doFilter(request, response);

            } catch (ExpiredJwtException e) {
                sendUnauthorizedResponse(response, "리프레시 토큰 만료. 재로그인 필요");
            } catch (JwtException e) {
                sendUnauthorizedResponse(response, "리프레시 토큰 처리 실패");
            }
        } else {
            sendUnauthorizedResponse(response, "리프레시 토큰 없음");
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message)
        throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        data.put("message", message);
        data.put("data", new Object[]{});
        response.getWriter().write(objectMapper.writeValueAsString(data));
        response.getWriter().flush();
    }
}
