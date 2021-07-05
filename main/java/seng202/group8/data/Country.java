package seng202.group8.data;

/**
 * Class responsible for creating Airline object
 */
public class Country extends Data {
    private String name;  // name (e.g. 'Australia')
    public static final String NAME = "Name";

    private String iso;  // iso, unique (e.g. 'AU')
    public static final String ISO = "ISO";

    /**
     * Initializer for country; no error checking and a storage only class; does not allow setting of new values
     *
     * @param id   ID of the country
     * @param name name of the country
     * @param iso  2 character ISO code for the country
     */
    public Country(int id, String name, String iso) {
        super(id);
        this.name = name;
        this.iso = iso;
    }

    /**
     * Getter for 2 character ISO code for the country
     *
     * @return the ISO code for the country
     */
    public String getISO() {
        return iso;
    }

    /**
     * Gets the name of the country
     *
     * @return name of the country
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s (ISO = %s, id = %d)", name, iso, getId());
    }
}
