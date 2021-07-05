package seng202.group8.data;

/**
 * Abstract class responsible for creating data object
 * The class implements the observer pattern, HOWEVER, modifications to properties WILL NOT trigger a notification automatically as otherwise, observers would be bombarded with multiple notifications when multiple properties are changed at once. Thus, the object modifying the data object MUST call `notifyObservers` once all modifications have been made
 * <p>
 * Static final strings such as ID and OTHER are property identifiers; it helps with figuring out which properties have invalid values when saving it to the database. The value of the identifier is the name of the column in the database schema
 */
public abstract class Data {
    /**
     * ID of the data object if it is not in the database
     */
    public static final int UNKNOWN_ID = -1;

    private final int id;
    public static final String ID = "ID";

    /**
     * Sets ID to UNKNOWN_ID
     */
    public Data() {
        id = UNKNOWN_ID;
    }

    /**
     * Initializes the data object with the given ID
     *
     * @param id id of the data object
     */
    public Data(int id) {
        this.id = id;
    }

    /**
     * Gets the ID of the data object. If unknown, returns UNKNOWN_ID
     *
     * @return id of the object; may be UNKNOWN_ID
     */
    public int getId() {
        return id;
    }

    /**
     * Denotes whether or not this data object is in memory only, and SHOULD NOT be saved to the database
     * Setters should run constraints checking but not save it to the database
     *
     * @return true if the object is in memory only and not in the database
     */
    public boolean isMemoryOnly() {
        return id == UNKNOWN_ID;
    }

    /**
     * Trims strings and converts empty strings to null
     *
     * @param str input string
     * @return null if the trimmed string is empty, or the trimmed string
     */
    public String trimmedEmptyStringToNull(String str) {
        if (str == null || str.trim().length() == 0 || str.equals("\"\"") || str.equals("-") || str.equals("N/A") || str.equals("\\N")) {
            return null;
        }

        return str.trim();
    }
}