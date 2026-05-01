package ca.optimusAI.tms.shared.exception;

public class ZoneHasActiveSessionsException extends RuntimeException {
    public ZoneHasActiveSessionsException(String message) {
        super(message);
    }
}
