package com.telemedicina.shared.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        long start = System.currentTimeMillis();

        chain.doFilter(req, res);

        long duration = System.currentTimeMillis() - start;
        log.info("{} {} → {} ({}ms)",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);
    }
}