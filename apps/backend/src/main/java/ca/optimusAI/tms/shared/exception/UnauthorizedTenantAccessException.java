package ca.optimusAI.tms.shared.exception;

public class UnauthorizedTenantAccessException extends RuntimeException {
    public UnauthorizedTenantAccessException(String message) {
        super(message);
    }
}
