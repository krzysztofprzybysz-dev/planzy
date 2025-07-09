package pl.planzy.artist.exception;

/**
 * General exception for artist service operations.
 * <p>
 * This exception is the base exception type for the artist service layer
 * and is used to indicate problems in artist-related business operations.
 * It typically wraps more specific exceptions from the data access layer
 * or other subsystems while providing context specific to artist operations.
 * <p>
 * This exception helps to translate low-level technical exceptions into
 * more meaningful business exceptions relevant to the artist domain.
 */
public class ArtistServiceException extends RuntimeException {
    /**
     * Creates a new exception with a message.
     *
     * @param message Detailed explanation of the artist service issue
     */
    public ArtistServiceException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message Detailed explanation of the artist service issue
     * @param cause The underlying exception that caused this problem
     */
    public ArtistServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}