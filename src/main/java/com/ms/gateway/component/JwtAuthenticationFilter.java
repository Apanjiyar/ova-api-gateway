package com.ms.gateway.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.gateway.dto.ApiResponse;
import com.ms.gateway.enums.ErrorCode;
import com.ms.gateway.util.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GatewayFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private static final List<String> PUBLIC_ROUTES = List.of(
            "/auth/login",
            "/auth/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        if (PUBLIC_ROUTES.stream().anyMatch(path::contains)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return buildUnauthorizedResponse(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        if (jwtUtil.isTokenBlacklisted(token)) {
            return buildUnauthorizedResponse(exchange, "Token has been revoked");
        }

        try {
            if (!jwtUtil.validateToken(token)) {
                return buildUnauthorizedResponse(exchange, "Token is expired or invalid");
            }

        } catch (Exception e) {
            return buildUnauthorizedResponse(exchange, "Invalid token");
        }

        return chain.filter(exchange);
    }

    private Mono<Void> buildUnauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        ApiResponse<Object> apiResponse = ApiResponseUtil.failure(
                ErrorCode.UNAUTHORIZED.name(),
                message,
                null
        );

        try {
            return response.writeWith(Mono.just(response.bufferFactory().wrap(
                    objectMapper.writeValueAsBytes(apiResponse)
            )));
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response.setComplete();
        }
    }
}