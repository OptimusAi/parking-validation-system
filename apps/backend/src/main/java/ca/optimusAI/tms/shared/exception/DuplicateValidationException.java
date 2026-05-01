package ca.optimusAI.tms.shared.exception;

public class DuplicateValidationException extends RuntimeException {
    public DuplicateValidationException(String message) {
        super(message);
    }
}
