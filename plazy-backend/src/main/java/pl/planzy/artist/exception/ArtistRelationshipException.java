package pl.planzy.artist.exception;

/**
 * Exception thrown when there are issues with artist-event relationships.
 * <p>
 * This exception is used specifically for operations involving the
 * many-to-many relationship between artists and events, such as:
 * <ul>
 *   <li>Linking artists to events</li>
 *   <li>Checking if an artist is associated with an event</li>
 *   <li>Batch operations for multiple artist-event relationships</li>
 * </ul>
 * <p>
 * The exception provides context about which relationship operation
 * failed and why, to facilitate troubleshooting.
 */
public class ArtistRelationshipException extends RuntimeException {
    /**
     * Creates a new exception with a message.
     *
     * @param message Detailed explanation of the relationship issue
     */
    public ArtistRelationshipException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message Detailed explanation of the relationship issue
     * @param cause The underlying exception that caused this problem
     */
    public ArtistRelationshipException(String message, Throwable cause) {
        super(message, cause);
    }
}