package com.regionalai.floatingball.server.common.exception;

import com.regionalai.floatingball.server.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("business exception. method={}, uri={}, requestId={}, code={}, message={}",
            request.getMethod(),
            request.getRequestURI(),
            resolveRequestId(request),
            ex.getCode(),
            ex.getMessage());
        return ApiResponse.error(ex.getCode(), ex.getMessage(), resolveRequestId(request));
    }

    @ExceptionHandler(UpdateRequiredException.class)
    @ResponseStatus(HttpStatus.UPGRADE_REQUIRED)
    public ApiResponse<Void> handleUpdateRequired(UpdateRequiredException ex, HttpServletRequest request) {
        log.warn("update required. method={}, uri={}, requestId={}, minSupportedVersion={}",
            request.getMethod(),
            request.getRequestURI(),
            resolveRequestId(request),
            ex.getMinSupportedVersion());
        return ApiResponse.error("UPDATE-REQUIRED", ex.getMessage(), resolveRequestId(request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
            ? "参数校验失败"
            : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        log.warn("validation exception. method={}, uri={}, requestId={}, message={}",
            request.getMethod(),
            request.getRequestURI(),
            resolveRequestId(request),
            message);
        return ApiResponse.error("VALIDATION-001", message, resolveRequestId(request));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("unexpected exception. method={}, uri={}, requestId={}",
            request.getMethod(),
            request.getRequestURI(),
            resolveRequestId(request),
            ex);
        return ApiResponse.error("SYS-500", ex.getMessage(), resolveRequestId(request));
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        return requestId == null || requestId.trim().isEmpty() ? "N/A" : requestId;
    }
}
