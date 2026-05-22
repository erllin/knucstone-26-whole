package com.example.spring.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import java.io.IOException;
import java.util.ArrayList;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    /**
     * 이 부분은 JWT 토큰 필터입니다. (SecurityConfig 부분과 관련이 있습니다.)
     * firebase가 많은 부분을 처리해주다보니, 기존 JWT 방법과는 다르게 간략하게 처리된 부분이 많습니다.
     *
     * REACT의 UserProvider의 맨 위쪽에 배치된 곳에서
     * 유저에 대한 토큰이 "Bearer "와 함께 들어오면,
     * "Bearer " 부분을 잘라내고 얻은 프론트엔드쪽 토큰을
     * Firebase로 넘겨 인증을 하고, 정보를 얻어 스프링쪽에 등록하는 필터입니다.
     */

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(req, res);
            return;
        }
        // "Bearer " 부분 삭제한 나머지 부분. (즉 토큰값)
        String token = header.substring(7);

        try {
            // Firebase에서 토큰, 정보 추출
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String uid = decodedToken.getUid();

            // 스프링 시큐리티에 사용자 등록
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(uid, null, new ArrayList<>());

            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(req, res);

        } catch (Exception e) {
            // 401
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("Invalid token" + e.getMessage());
            return;
        }
    }
}
