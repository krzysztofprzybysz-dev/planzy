package pl.planzy.place.exception;

/**
 * Exception thrown when a place cannot be found.
 * <p>
 * This exception is used in situations where a place is expected to exist
 * but cannot be located, such as:
 * <ul>
 *   <li>When retrieving a place by ID that doesn't exist</li>
 *   <li>When a place in the cache can't be found in the database</li>
 *   <li>When a Google Place ID doesn't match any record</li>
 * </ul>
 * <p>
 * The exception includes context information to help diagnose the issue.
 */
public class PlaceNotFoundException extends RuntimeException {
    /**
     * Creates a new exception with a message.
     *
     * @param message Detailed explanation of why the place wasn't found
     */
    public PlaceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message Detailed explanation of why the place wasn't found
     * @param cause The underlying exception that caused this problem
     */
    public PlaceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }


    /**
     * Creates a new exception for a place not found with additional context.
     *
     * @param identifier The place identifier (name, ID, etc.)
     * @param reason Additional explanation of why the lookup failed
     */
    public PlaceNotFoundException(String identifier, String reason) {
        super("Place not found with identifier: " + identifier + ". Reason: " + reason);
    }
}