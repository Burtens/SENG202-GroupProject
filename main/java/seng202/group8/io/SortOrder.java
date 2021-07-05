package seng202.group8.io;

/**
 * An enum of the types of sort ordering (ascending and descending).
 * Each value contains an SQL code that is the sql token for that type of sort order.
 */
public enum SortOrder {
    DESCENDING("DESC"),
    ASCENDING("ASC");

    private final String SQLCode;

    /**
     * Initializes the sort order to the given value
     *
     * @param SQLCode sort order, either 'ASC' or 'DESC'
     */
    SortOrder(String SQLCode) {
        this.SQLCode = SQLCode;
    }

    /**
     * Gets the sort order
     *
     * @return sort order as a string, either 'ASC' or 'DESC'
     */
    public String getSQLCode() {
        return SQLCode;
    }
}
