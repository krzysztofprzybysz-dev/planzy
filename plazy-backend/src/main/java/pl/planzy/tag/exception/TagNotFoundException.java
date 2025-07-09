package pl.planzy.tag.exception;

/**
 * Exception thrown when a tag cannot be found.
 * <p>
 * This exception is used in situations where a tag is expected to exist
 * but cannot be located, such as:
 * <ul>
 *   <li>When retrieving a tag by ID that doesn't exist</li>
 *   <li>When a tag in the cache can't be found in the database</li>
 *   <li>When referencing a tag by name that doesn't exist</li>
 * </ul>
 * <p>
 * The exception includes context information to help diagnose the issue.
 */
public class TagNotFoundException extends RuntimeException {
    /**
     * Creates a new exception with a message.
     *
     * @param message Detailed explanation of why the tag wasn't found
     */
    public TagNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message Detailed explanation of why the tag wasn't found
     * @param cause The underlying exception that caused this problem
     */
    public TagNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception for a tag not found by ID.
     *
     * @param id The tag ID that was not found
     */
    public TagNotFoundException(Long id) {
        super("Tag not found with ID: " + id);
    }

    /**
     * Creates a new exception for a tag not found by name with additional context.
     *
     * @param tagName The tag name that was not found
     * @param reason Additional explanation of why the lookup failed
     */
    public TagNotFoundException(String tagName, String reason) {
        super("Tag not found with name: " + tagName + " Reason: " + reason);
    }
}