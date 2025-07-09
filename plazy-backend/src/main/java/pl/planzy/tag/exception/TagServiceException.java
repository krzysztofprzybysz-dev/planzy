package pl.planzy.tag.exception;

/**
 * General exception for tag service operations.
 * <p>
 * This exception is the base exception type for the tag service layer
 * and is used to indicate problems in tag-related business operations.
 * It typically wraps more specific exceptions from the data access layer
 * or other subsystems while providing context specific to tag operations.
 * <p>
 * This exception helps to translate low-level technical exceptions into
 * more meaningful business exceptions relevant to the tag domain.
 */
public class TagServiceException extends RuntimeException {
    /**
     * Creates a new exception with a message.
     *
     * @param message Detailed explanation of the tag service issue
     */
    public TagServiceException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message Detailed explanation of the tag service issue
     * @param cause The underlying exception that caused this problem
     */
    public TagServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}