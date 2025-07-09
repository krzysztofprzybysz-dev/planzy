package pl.planzy.artist.exception;

/**
 * Exception thrown when an artist cannot be found.
 * <p>
 * This exception is used in situations where an artist is expected to exist
 * but cannot be located, such as:
 * <ul>
 *   <li>When retrieving an artist by ID that doesn't exist</li>
 *   <li>When an artist in the cache can't be found in the database</li>
 *   <li>When referencing an artist by name that doesn't exist</li>
 * </ul>
 * <p>
 * The exception includes context information to help diagnose the issue.
 */
public class ArtistNotFoundException extends RuntimeException {
    /**
     * Creates a new exception with a message.
     *
     * @param message Detailed explanation of why the artist wasn't found
     */
    public ArtistNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message Detailed explanation of why the artist wasn't found
     * @param cause The underlying exception that caused this problem
     */
    public ArtistNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception for an artist not found by ID.
     *
     * @param id The artist ID that was not found
     */
    public ArtistNotFoundException(Long id) {
        super("Artist not found with ID: " + id);
    }

    /**
     * Creates a new exception for an artist not found by name with additional context.
     *
     * @param artistName The artist name that was not found
     * @param reason Additional explanation of why the lookup failed
     */
    public ArtistNotFoundException(String artistName, String reason) {
        super("Artist not found with name: " + artistName + " Reason: " + reason);
    }
}