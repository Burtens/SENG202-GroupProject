package seng202.group8.io;

/**
 * An error that is thrown when constraints have been violated
 */
public class ConstraintsError extends Error {

    /**
     * Creates the constraints error
     *
     * @param message The message of the error
     */
    public ConstraintsError(String message) {
        super(message);
    }
}
