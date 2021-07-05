package seng202.group8.data;

import seng202.group8.datacontroller.DataConstraintsException;

/* OpenFlights Data
Airport ID 	Unique OpenFlights identifier for this airport.
Name 	Name of airport. May or may not contain the City name.
City 	Main city served by airport. May be spelled differently from Name.
Country 	Country or territory where airport is located.
IATA 	3-letter IATA code. Null if not assigned/unknown.
ICAO 	4-letter ICAO code. Null if not assigned.
Latitude 	Decimal degrees, usually to six significant digits. Negative is South, positive is North.
Longitude 	Decimal degrees, usually to six significant digits. Negative is West, positive is East.
Altitude 	In feet.
Timezone 	Hours offset from UTC. Fractional hours are expressed as decimals, eg. India is 5.5.
DST 	Daylight savings time. One of E (Europe), A (US/Canada), S (South America),
 O (Australia), Z (New Zealand), N (None) or U (Unknown)
Tz database time zone 	Timezone in "tz" (Olson) format, eg. "America/Los_Angeles".
Type 	Type of the airport. Value "airport" for air terminals, "station" for train stations,
 "port" for ferry terminals and "unknown" if not known. In airports.csv, only type=airport is included.
*/


/**
 * Data class storing information on an airport. Corresponds to the 'Airport' table in the database.
 */
public class Airport extends Data {
    private String name = "";
    public static final String NAME = "Name"; // Name of Airport. Not null

    private String city = "";
    public static final String CITY = "City"; // Name of City the Airport is in.

    private String country = "";
    public static final String COUNTRY = "Country"; // Name of Country the Airport is in. Foreign key

    private String iata = "";
    public static final String IATA = "Iata"; // Iata code of the Airport, 3-letter code.

    private String icao = "";
    public static final String ICAO = "Icao"; // Icao code of the Airport, 4-letter code.

    private double latitude;
    public static final String LATITUDE = "Latitude"; // Latitude of Airport.

    private double longitude;
    public static final String LONGITUDE = "Longitude"; // Longitude of Airport.

    private int altitude;
    public static final String ALTITUDE = "Altitude"; // Altitude of Airport in feet.

    private double timezone;
    public static final String TIMEZONE = "Timezone"; // Timezone of Airport.

    private DSTType dst;
    public static final String DST = "DST"; // Daylight Savings Time (DST) of Airport.

    public static final String NULLINPUT = "Null Input"; // When a null input is found in the constructor


    /**
     * Regexp which checks if the code is a valid IATA code
     */
    public static final String isAirportIATARegExp = "^[A-Za-z0-9]{3}$";

    /**
     * Regexp which checks if the code is a valid ICAO code
     */
    public static final String isAirportICAORegExp = "^[A-Za-z0-9]{4}$";

    /**
     * Constructor for importing from the database
     *
     * @param id        unique identifier for the row in the database
     * @param name      name of the airport
     * @param city      name of the city the airport is located in
     * @param country   the country (object) the airport is located in
     * @param iata      three-character, alphanumeric IATA code for the airport. Optional
     * @param icao      four-character, alphanumeric ICAO code for the airport
     * @param latitude  latitude of the airport
     * @param longitude longitude of the airport
     * @param altitude  altitude of the airport in feet
     * @param timezone  UTC offset in hours of the airport
     * @param dst       daylight saving time type for the airport
     */
    public Airport(int id, String name, String city, String country, String iata, String icao, double latitude,
                   double longitude, int altitude, double timezone, char dst) {
        super(id);
        this.name = name;
        this.city = city;
        this.country = country;
        this.iata = iata;
        this.icao = icao;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.timezone = timezone;
        this.dst = DSTType.fromCode(dst);
    }

    /**
     * This is the constructor used when importing from a CSV, or when a user is editing and or changing data
     * Setters are used and errors may be thrown if the data is invalid
     *
     * @param name      name of the airport
     * @param city      name of the city the airport is located in
     * @param country   the name of the country the airport is located in
     * @param iata      three-character, alphanumeric IATA code for the airport. Optional
     * @param icao      four-character, alphanumeric ICAO code for the airport
     * @param latitude  latitude of the airport
     * @param longitude longitude of the airport
     * @param altitude  altitude of the airport in feet
     * @param timezone  UTC offset in hours of the airport
     * @param dst       daylight saving time type for the airport
     * @throws DataConstraintsException if one or more of the values are invalid. It will attempt to set all values before failing,
     *                                  and so may contain error messages for multiple properties
     */
    public Airport(String name, String city, String country, String iata, String icao, double latitude,
                   double longitude, int altitude, double timezone, char dst) throws DataConstraintsException {
        super();
        DataConstraintsException e = null;
        e = DataConstraintsException.attempt(e, () -> this.setName(name));
        e = DataConstraintsException.attempt(e, () -> this.setCity(city));
        e = DataConstraintsException.attempt(e, () -> this.setCountry(country));
        e = DataConstraintsException.attempt(e, () -> this.setIata(iata));
        e = DataConstraintsException.attempt(e, () -> this.setIcao(icao));
        e = DataConstraintsException.attempt(e, () -> this.setLatitude(latitude));
        e = DataConstraintsException.attempt(e, () -> this.setLongitude(longitude));
        e = DataConstraintsException.attempt(e, () -> this.setAltitude(altitude));
        e = DataConstraintsException.attempt(e, () -> this.setTimezone(timezone));
        e = DataConstraintsException.attempt(e, () -> this.setDst(dst));
        if (e != null) {
            throw e;
        }
    }

    /**
     * Gets the unique name for the airport
     *
     * @return unique name for the airport
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the airport. Should be unique, but this setter DOES NOT enforce this
     *
     * @param name name of the airport. Gets trimmed and if empty string, gets interpreted as null. After trimming,
     *             the name must be at least three characters long
     * @throws DataConstraintsException if the name is not long enough
     */
    public void setName(String name) throws DataConstraintsException {
        name = trimmedEmptyStringToNull(name);
        if (name == null || name.length() < 3) {
            throw new DataConstraintsException(NAME, "Name cannot be shorter than length 3");
        } else if (name.contains(";")) {
            throw new DataConstraintsException(NAME, "Name cannot contain semicolons");
        }
        else {
            this.name = name;
        }
    }

    /**
     * Gets the name of the city the airport is located in
     *
     * @return name of the city
     */
    public String getCity() {
        return city;
    }

    /**
     * Sets the name of the city the airport is located in. Gets trimmed and if empty string, gets interpreted as null
     *
     * @param city name of the city. Must not be null
     * @throws DataConstraintsException if city is null
     */
    public void setCity(String city) throws DataConstraintsException {
        city = trimmedEmptyStringToNull(city);
        if (city == null) {
            throw new DataConstraintsException(CITY, "City cannot be empty");
        } else if (city.contains(";")) {
            throw new DataConstraintsException(CITY, "City cannot contain semicolons");
        }
        this.city = city;
    }

    /**
     * Gets the name of the country the airport is located in
     *
     * @return name of the country the airport is located in
     */
    public String getCountry() {
        return country;
    }

    /**
     * Sets the name of the country the airport is located in
     *
     * @param country name of the country the airport is located in
     * @throws DataConstraintsException if country is null or an empty string
     */
    public void setCountry(String country) throws DataConstraintsException {
        country = trimmedEmptyStringToNull(country);
        if (country == null) {
            throw new DataConstraintsException(COUNTRY, "Country cannot be null");
        }
        this.country = country;
    }

    /**
     * Getter for the three-character, alphanumeric IATA code
     *
     * @return iata code that identifies the airport. MAY BE AN EMPTY STRING
     */
    public String getIata() {
        return iata;
    }

    /**
     * Setter for three-character, alphanumeric IATA code
     *
     * @param iata three-character, alphanumeric IATA code. Gets trimmed and converted to uppercase. If empty string,
     *             gets interpreted as null
     * @throws DataConstraintsException if the IATA code is neither null or a three-character alphanumeric string
     */
    public void setIata(String iata) throws DataConstraintsException {
        iata = trimmedEmptyStringToNull(iata);

        if (iata == null) {
            this.iata = null;
        } else if (iata.matches(isAirportIATARegExp)) {
            this.iata = iata.toUpperCase();
        } else {
            throw new DataConstraintsException(IATA, "IATA must be a three-character alphanumeric code");
        }
    }

    /**
     * Setter for the four-character, alphanumeric ICAO code
     *
     * @param icao four-character, alphanumeric ICAO code. Gets trimmed and converted to uppercase. If empty string,
     *             gets interpreted as null. Cannot be null.
     * @throws DataConstraintsException if the icao is not a four-character alphanumeric string
     */
    public void setIcao(String icao) throws DataConstraintsException {
        icao = trimmedEmptyStringToNull(icao);
        if (icao == null) {
            throw new DataConstraintsException(ICAO, "ICAO cannot be empty");
        } else if (icao.matches(isAirportICAORegExp)) {
            this.icao = icao.toUpperCase();
        } else {
            throw new DataConstraintsException(ICAO, "ICAO must be a four-character alphanumeric code");
        }
    }

    /**
     * Getter for the four-character, alphanumeric ICAO code
     *
     * @return icao that identifies the airport
     */
    public String getIcao() {
        return icao;
    }

    /**
     * Gets either the three-character IATA or four-character ICAO code, preferring the IATA code
     *
     * @return IATA or ICAO code, preferring IATA if both are available
     */
    public String getCode() {
        if (getIata() == null) {
            return icao;
        } else {
            return iata;
        }
    }

    /**
     * Gets the latitude of the airport
     *
     * @return latitude of the airport
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Sets the latitude of the airport
     *
     * @param latitude must be a double between -90 and 90 inclusive
     * @throws DataConstraintsException if the latitude is invalid
     */
    public void setLatitude(double latitude) throws DataConstraintsException {
        if (latitude < -90 || latitude > 90) {
            throw new DataConstraintsException(LATITUDE, "Latitude must be between -90 and 90");
        } else {
            this.latitude = latitude;
        }
    }

    /**
     * Gets the longitude of the airport
     *
     * @return longitude of the airport
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Sets the longitude of the airport
     *
     * @param longitude must be a double between -180 and 180 inclusive
     * @throws DataConstraintsException if the longitude is invalid
     */
    public void setLongitude(double longitude) throws DataConstraintsException {
        if (longitude < -180 || longitude > 180) {
            throw new DataConstraintsException(LONGITUDE, "Longitude must be between -180 and 180");
        } else {
            this.longitude = longitude;
        }
    }

    /**
     * Gets the altitude of the airport in feet
     *
     * @return the altitude of the airport in feet
     */
    public int getAltitude() {
        return altitude;
    }

    /**
     * Sets the altitude of the airport
     *
     * @param altitude altitude of the airport in feet. Can be below 0
     * @throws DataConstraintsException if the altitude is below -1240ft
     */
    public void setAltitude(int altitude) throws DataConstraintsException {
        if (altitude >= -1240) {
            this.altitude = altitude;
        } else {
            throw new DataConstraintsException(ALTITUDE, "Altitude can not be below -1240ft");
        }
    }

    /**
     * Gets the timezone the airport is in
     *
     * @return timezone in hours
     */
    public double getTimezone() {
        return timezone;
    }

    /**
     * Sets the timezone the airport is in
     *
     * @param timezone UTC timezone offset between -12 and 14 inclusive
     * @throws DataConstraintsException if the timezone is not within the given range
     */
    public void setTimezone(double timezone) throws DataConstraintsException {
        if (timezone > 14 || timezone < -12) {
            throw new DataConstraintsException(TIMEZONE, "Timezone UTC offset must be between -12 and 14");
        } else {
            this.timezone = timezone;
        }
    }

    /**
     * Getter for the DST (Daylight Saving Time) type used by the airport
     *
     * @return DSTType enum value
     */
    public DSTType getDst() {
        return dst;
    }

    /**
     * Setter for the DST (Daylight Saving Time) type used by the airport
     *
     * @param dst character corresponding to code used by OpenFlights. If unknown, it is set to UNKNOWN
     */
    public void setDst(char dst) {
        this.dst = DSTType.fromCode(dst);
    }
}
