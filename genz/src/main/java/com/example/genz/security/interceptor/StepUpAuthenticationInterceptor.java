package com.example.genz.security.interceptor;

import com.example.genz.common.exception.BadRequestException;
import com.example.genz.security.annotation.RequireStepUp;
import com.example.genz.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class StepUpAuthenticationInterceptor implements HandlerInterceptor {

    public static final String STEP_UP_HEADER = "X-Step-Up-Token";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireStepUp requireStepUp = handlerMethod.getMethodAnnotation(RequireStepUp.class);
        if (requireStepUp == null) {
            requireStepUp = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequireStepUp.class);
        }

        if (requireStepUp == null) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            throw new AccessDeniedException("Authentication is required for sensitive operation");
        }

        String stepUpToken = request.getHeader(STEP_UP_HEADER);
        if (stepUpToken == null || stepUpToken.isBlank()) {
            throw new BadRequestException("Missing " + STEP_UP_HEADER + " header");
        }

        String authenticatedPhoneNumber = authentication.getName();
        String requiredScope = requireStepUp.scope();

        boolean isValid = jwtUtil.validateStepUpToken(stepUpToken, authenticatedPhoneNumber, requiredScope);
        if (!isValid) {
            throw new BadRequestException("Invalid or expired step-up token");
        }

        return true;
    }
}
