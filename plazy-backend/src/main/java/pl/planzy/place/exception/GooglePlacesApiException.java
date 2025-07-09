package pl.planzy.place.exception;

/**
 * Exception thrown when there are issues with Google Places API integration.
 * <p>
 * This specialized exception is used for problems that occur during interaction
 * with the Google Places API, such as:
 * <ul>
 *   <li>API key issues</li>
 *   <li>Connection problems</li>
 *   <li>Rate limiting</li>
 *   <li>Unexpected response formats</li>
 *   <li>Service unavailability</li>
 * </ul>
 * <p>
 * This exception helps isolate Google Places API issues from other types of
 * place-related problems, making it easier to implement specific recovery
 * strategies for external service failures.
 */
public class GooglePlacesApiException extends RuntimeException {
    /**
     * The HTTP status code from Google API, if available.
     */
    private final Integer statusCode;

    /**
     * Creates a new exception with a message.
     *
     * @param message Detailed explanation of the Google Places API issue
     */
    public GooglePlacesApiException(String message) {
        super(message);
        this.statusCode = null;
    }

    /**
     * Creates a new exception with a message, status code, and cause.
     *
     * @param message Detailed explanation of the Google Places API issue
     * @param statusCode HTTP status code if available
     * @param cause The underlying exception that caused this problem
     */
    public GooglePlacesApiException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message Detailed explanation of the Google Places API issue
     * @param cause The underlying exception that caused this problem
     */
    public GooglePlacesApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    /**
     * Gets the HTTP status code associated with this exception, if available.
     *
     * @return The HTTP status code, or null if not available
     */
    public Integer getStatusCode() {
        return statusCode;
    }
}