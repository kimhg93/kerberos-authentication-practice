package com.security.kerberos.config;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request
            , HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        // AuthenticationFailure 발생시 처리 로직
        if (exception instanceof BadCredentialsException) { // BadCredentialsException 만 처리하기 위함
            response.sendRedirect("/login");
        }
    }
}