package com.ticket.ops.exception;

import com.ticket.ops.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * 统一异常处理。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IncidentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(IncidentNotFoundException error,
                                                        HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, error.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException error,
                                                          HttpServletRequest request) {
        String message = error.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException error,
                                                               HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, error.getMessage(), request);
    }

    /**
     * 静态资源/路径不存在属于 404，不是服务端故障。
     * 缺少这个分支时会被下面的 Exception 兜底成 500「服务内部错误」。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException error,
                                                          HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "资源不存在: " + request.getRequestURI(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception error, HttpServletRequest request) {
        log.error("未处理异常: {}", request.getRequestURI(), error);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "服务内部错误", request);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
