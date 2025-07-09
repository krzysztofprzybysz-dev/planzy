package pl.planzy.tag.exception;

/**
 * Exception thrown when there are issues with tag-event relationships.
 * <p>
 * This exception is used specifically for operations involving the
 * many-to-many relationship between tags and events, such as:
 * <ul>
 *   <li>Linking tags to events</li>
 *   <li>Checking if a tag is associated with an event</li>
 *   <li>Batch operations for multiple tag-event relationships</li>
 * </ul>
 * <p>
 * The exception provides context about which relationship operation
 * failed and why, to facilitate troubleshooting.
 */
public class TagRelationshipException extends RuntimeException {
    /**
     * Creates a new exception with a message.
     *
     * @param message Detailed explanation of the relationship issue
     */
    public TagRelationshipException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message Detailed explanation of the relationship issue
     * @param cause The underlying exception that caused this problem
     */
    public TagRelationshipException(String message, Throwable cause) {
        super(message, cause);
    }
}