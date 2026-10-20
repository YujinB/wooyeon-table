package com.wooyeon.table.global.auth;

import com.wooyeon.table.global.audit.ActorContext;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** /api/app/** 는 로그인, /api/admin/** 는 운영자만 (경로는 WebConfig에서 지정) */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String AUTH_USER_ATTRIBUTE = AuthUser.class.getName();
    private static final String BEARER = "Bearer ";
    private static final String ADMIN_PATH = "/api/admin/";

    private final AuthTokenProvider tokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (RequestMethod.OPTIONS.name().equals(request.getMethod())) {
            return true;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        AuthUser user = tokenProvider.parse(header.substring(BEARER.length()).trim());
        if (request.getRequestURI().startsWith(ADMIN_PATH) && !user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        request.setAttribute(AUTH_USER_ATTRIBUTE, user);
        ActorContext.set((user.isAdmin() ? "운영자 #" : "참가자 #") + user.userId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception exception) {
        ActorContext.clear();
    }
}
