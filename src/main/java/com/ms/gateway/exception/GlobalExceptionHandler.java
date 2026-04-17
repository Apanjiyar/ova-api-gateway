package com.ms.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.gateway.dto.ApiResponse;
import com.ms.gateway.enums.ErrorCode;
import com.ms.gateway.util.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Something went wrong";
        String code = ErrorCode.INTERNAL_SERVER_ERROR.name();

        if (ex instanceof ResponseStatusException e) {
            status = HttpStatus.valueOf(e.getStatusCode().value());
            message = e.getReason() != null ? e.getReason() : "Method not supported";
            code = ErrorCode.BAD_REQUEST.name();
        } else {
            log.error("Unhandled Exception", ex);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Object> apiResponse = ApiResponseUtil.failure(code, message, null);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(
                writeValueAsBytes(apiResponse)
        )));
    }

    private byte[] writeValueAsBytes(ApiResponse<Object> apiResponse) {
        try {
            return objectMapper.writeValueAsBytes(apiResponse);
        } catch (Exception e) {
            return "{\"success\":false,\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Something went wrong\"}".getBytes();
        }
    }
}