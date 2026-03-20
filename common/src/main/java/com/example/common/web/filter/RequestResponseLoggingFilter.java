package com.example.common.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Request and Response Logging Filter
 * Logs all HTTP requests and responses with timing information
 */
@Component
@Order(1)
public class RequestResponseLoggingFilter implements Filter {

    private static final Logger logger = LogManager.getLogger(RequestResponseLoggingFilter.class);
    private static final Logger requestLogger = LogManager.getLogger("com.wifiin.newsay.ai.web.filter");

    private static final ConcurrentMap<String, Long> startTimeMap = new ConcurrentHashMap<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Initializing RequestResponseLoggingFilter");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestId = generateRequestId(httpRequest);
        String requestUri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        String queryString = httpRequest.getQueryString();
        String remoteAddr = getClientIp(httpRequest);

        long startTime = System.currentTimeMillis();
        startTimeMap.put(requestId, startTime);

        // Log Request
        requestLogger.info("[{}] --> {} {} {} | Client: {} | Content-Type: {} | Content-Length: {}",
                requestId,
                method,
                requestUri,
                queryString != null ? "?" + queryString : "",
                remoteAddr,
                httpRequest.getContentType(),
                httpRequest.getContentLengthLong()
        );

        // Log Request Headers (debug level)
        if (logger.isDebugEnabled()) {
            httpRequest.getHeaderNames().asIterator().forEachRemaining(headerName ->
                    requestLogger.debug("[{}] Request Header: {} = {}",
                            requestId, headerName, httpRequest.getHeader(headerName))
            );
        }

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();

            // Log Response
            requestLogger.info("[{}] <-- {} {} | Status: {} | Duration: {}ms",
                    requestId,
                    method,
                    requestUri,
                    status,
                    duration
            );

            // Log Response Headers (debug level)
            if (logger.isDebugEnabled()) {
                httpResponse.getHeaderNames().forEach(headerName ->
                        requestLogger.debug("[{}] Response Header: {} = {}",
                                requestId, headerName, httpResponse.getHeader(headerName))
                );
            }

            startTimeMap.remove(requestId);
        }
    }

    @Override
    public void destroy() {
        logger.info("Destroying RequestResponseLoggingFilter");
    }

    private String generateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || requestId.isEmpty()) {
            requestId = String.format("%s-%d",
                    request.getSession().getId(),
                    System.nanoTime());
        }
        return requestId;
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
