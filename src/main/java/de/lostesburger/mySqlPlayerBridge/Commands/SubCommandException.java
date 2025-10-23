package de.lostesburger.mySqlPlayerBridge.Commands;

public class SubCommandException extends RuntimeException {
    public enum Type {
        UNKNOWN_SUBCOMMAND,
        NO_ARGS_ERROR
    }
    
    private final Type errorType;
    private final String details;

    public SubCommandException(Type errorType, String details) {
        this.errorType = errorType;
        this.details = details;
    }

    public Type getErrorType() {
        return errorType;
    }

    public String getDetails() {
        return details;
    }
}