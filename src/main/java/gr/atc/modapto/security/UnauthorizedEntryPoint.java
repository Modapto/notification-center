package gr.atc.modapto.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.atc.modapto.controller.BaseAppResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        BaseAppResponse<String> responseMessage = BaseAppResponse.error("Unauthorized request. Check token and try again.", "Invalid Token");

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getWriter(), responseMessage);
    }
}