package com.example.genz.security.annotation;

import com.example.genz.security.jwt.JwtUtil;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireStepUp {
    String scope() default JwtUtil.FINANCIAL_TRANSACTION_SCOPE;
}
