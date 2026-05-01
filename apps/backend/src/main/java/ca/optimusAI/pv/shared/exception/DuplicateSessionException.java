package ca.optimusAI.pv.shared.exception;

public class DuplicateSessionException extends RuntimeException {
    public DuplicateSessionException(String message) {
        super(message);
    }
}
