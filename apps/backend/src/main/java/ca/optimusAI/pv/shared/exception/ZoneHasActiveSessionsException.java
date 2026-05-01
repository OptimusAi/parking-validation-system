package ca.optimusAI.pv.shared.exception;

public class ZoneHasActiveSessionsException extends RuntimeException {
    public ZoneHasActiveSessionsException(String message) {
        super(message);
    }
}
