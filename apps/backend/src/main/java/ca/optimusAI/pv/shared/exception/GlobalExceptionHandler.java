package ca.optimusAI.pv.shared.exception;

import ca.optimusAI.pv.shared.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(QuotaExceededException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError handleQuota(QuotaExceededException e) {
        return new ApiError("QUOTA_EXCEEDED", e.getMessage(), Map.of(), Instant.now());
    }

    @ExceptionHandler(DuplicateSessionException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError handleDuplicateSession(DuplicateSessionException e) {
        return new ApiError("ALREADY_VALIDATED", e.getMessage(), Map.of(), Instant.now());
    }

    @ExceptionHandler(DuplicateValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError handleDuplicateValidation(DuplicateValidationException e) {
        return new ApiError("ALREADY_VALIDATED", e.getMessage(), Map.of(), Instant.now());
    }

    @ExceptionHandler(LinkExpiredException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError handleLinkExpired(LinkExpiredException e) {
        return new ApiError("LINK_EXPIRED", e.getMessage(), Map.of(), Instant.now());
    }

    @ExceptionHandler(ZoneHasActiveSessionsException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError handleZoneActive(ZoneHasActiveSessionsException e) {
        return new ApiError("ZONE_HAS_ACTIVE_SESSIONS", e.getMessage(), Map.of(), Instant.now());
    }

    @ExceptionHandler(TenantNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleTenantNotFound(TenantNotFoundException e) {
        return new ApiError("TENANT_NOT_FOUND", e.getMessage(), Map.of(), Instant.now());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(ResourceNotFoundException e) {
        return new ApiError("NOT_FOUND", e.getMessage(), Map.of(), Instant.now());
    }

    @ExceptionHandler(UnauthorizedTenantAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleUnauthorized(UnauthorizedTenantAccessException e) {
        return new ApiError("FORBIDDEN", "Access denied", Map.of(), Instant.now());
    }

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleInvalidToken(InvalidTokenException e) {
        return new ApiError("INVALID_TOKEN", e.getMessage(), Map.of(), Instant.now());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleAccessDenied(AccessDeniedException e) {
        return new ApiError("FORBIDDEN", "Access denied", Map.of(), Instant.now());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> details = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "invalid",
                        (a, b) -> a));
        return new ApiError("VALIDATION_ERROR", "Validation failed", details, Instant.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleGeneric(Exception e) {
        log.error("Unhandled exception", e);
        return new ApiError("INTERNAL_ERROR", "An unexpected error occurred", Map.of(), Instant.now());
    }
}
