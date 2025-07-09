package pl.planzy.place.exception;

/**
 * General exception for place service operations.
 * <p>
 * This exception is the base exception type for the place service layer
 * and is used to indicate problems in place-related business operations.
 * It typically wraps more specific exceptions from the data access layer,
 * external services, or other subsystems while providing context specific
 * to place operations.
 * <p>
 * This exception helps to translate low-level technical exceptions into
 * more meaningful business exceptions relevant to the place domain.
 */
public class PlaceServiceException extends RuntimeException {
    /**
     * Creates a new exception with a message.
     *
     * @param message Detailed explanation of the place service issue
     */
    public PlaceServiceException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message Detailed explanation of the place service issue
     * @param cause The underlying exception that caused this problem
     */
    public PlaceServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}