package seng202.group8.datacontroller;

import java.util.HashMap;

/**
 * Class of exceptions for when updating the data fails due to some constraint not being met
 * (e.g. invalid value, lack of uniqueness). Caller must note the column which has the invalid value
 * <p>
 * Has ability to merge multiple exceptions:
 * <pre>
 * {@code
 *         DataConstraintsException e = null;
 *         e = DataConstraintsException.attempt(e, () -> {this.setterThatMayReturnDataConstraintsException1(); });
 *         e = DataConstraintsException.attempt(e, () -> {this.setterThatMayReturnDataConstraintsException2(); });
 *         e = DataConstraintsException.attempt(e, () -> {this.setterThatMayReturnDataConstraintsException3(); });
 * }
 * </pre>
 */
public class DataConstraintsException extends Exception {
    /**
     * HashMap mapping the name of the property who's value is invalid and an error message
     */
    public HashMap<String, String> errors;

    /**
     * Initializes the exception with a single invalid property attribute and error message
     *
     * @param propertyName name of the property with the bad attribute
     * @param errorMessage error message for that attribute
     */
    public DataConstraintsException(String propertyName, String errorMessage) {
        super();
        errors = new HashMap<String, String>();
        errors.put(propertyName, errorMessage);
    }

    /**
     * Gets the error message for the given property. Wrapper around `this.errors.get()`
     *
     * @param propertyName name of the property
     * @return error message, if an error for that property exists, null otherwise
     */
    public String error(String propertyName) {
        return errors.get(propertyName);
    }

    @Override
    public String getMessage() {
        String message = String.format("The following data constraints (%d) were violated:", errors.size());
        for (String key : errors.keySet()) {
            message += String.format("\n'%s': %s", key, errors.get(key));
        }
        return message;
    }

    /**
     * Merges current object with another DataConstraintsException object
     *
     * @param exception exception object to merge the current one with, or null
     */
    public void mergeExceptions(DataConstraintsException exception) {
        if (exception == null) {
            return;
        }
        for (String property : exception.errors.keySet()) {
            if (errors.containsKey(property)) {
                errors.put(property, errors.get(property) + " AND " + exception.errors.get(property));
            } else {
                errors.put(property, exception.errors.get(property));
            }
        }
    }

    /**
     * Interface used when merging constraint exceptions
     */
    public interface DataConstraintsExceptionMerger {
        /**
         * Method that is called when passed to `DataConstraintsException.attempt`
         * @throws DataConstraintsException if constraints are violated
         */
        void attempt() throws DataConstraintsException;
    }

    /**
     * Runs a lambda and if an DataConstraintsException occurs, merges it with itself
     *
     * @param coalesce lambda to run
     */
    public void attempt(DataConstraintsExceptionMerger coalesce) {
        try {
            coalesce.attempt();
        } catch (DataConstraintsException e) {
            mergeExceptions(e);
        }
    }

    /**
     * Runs a lambda which may cause a DataConstraintsException
     *
     * @param existingException existing exception object, or null
     * @param coalesce          lambda which may throw the exception
     * @return null if existing is null and the lambda does not throw the exception, or the exception. If the existing exception is not null, that one will be modified
     */
    public static DataConstraintsException attempt(DataConstraintsException existingException, DataConstraintsExceptionMerger coalesce) {
        try {
            coalesce.attempt();
        } catch (DataConstraintsException e) {
            if (existingException == null) {
                return e;
            } else {
                existingException.mergeExceptions(e);
            }
        }

        return existingException;
    }

}