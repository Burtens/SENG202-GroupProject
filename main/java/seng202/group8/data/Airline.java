package seng202.group8.data;

import seng202.group8.datacontroller.DataConstraintsException;

/**
 * Data class storing information on an airline. Corresponds to the 'Airline' table in the database.
 */
public class Airline extends Data {
    private String name;
    public static final String NAME = "Name"; // Name of Airline. Unique, not null

    private String callsign;
    public static final String CALLSIGN = "Callsign"; // Callsign of the airline. Unique

    private String iata;
    public static final String IATA = "Iata"; // two-character IATA code. Unique

    private String icao;
    public static final String ICAO = "Icao"; // three-character ICAO code. Unique

    private String country;
    public static final String COUNTRY = "Country"; // Country or territory where airport is located. Foreign key to country name

    /**
     * Regexp which checks if the code is a valid IATA code
     */
    public static final String isAirlineIATARegExp = "^[A-Za-z0-9]{2}$";

    /**
     * Regexp which checks if the code is a valid ICAO code
     */
    public static final String isAirlineICAORegExp = "^[A-Za-z0-9]{3}$";

    /**
     * Constructor for importing from the database
     *
     * @param id       unique identifier for the airline in the database
     * @param name     name of the airline
     * @param callsign callsign of the Airline
     * @param iata     two-character alphanumeric IATA airline code. One of IATA and ICAO can be null
     * @param icao     three-character alphanumeric ICAO airline code. One of IATA and ICAO can be null
     * @param country  name of the country the airline is located in
     */
    public Airline(int id, String name, String callsign, String iata, String icao, String country) {
        super(id);
        this.name = name;
        this.callsign = callsign;
        this.iata = iata;
        this.icao = icao;
        this.country = country;
    }

    /**
     * This is the constructor when a user is editing and or changing data, using setters as the data given may not
     * be valid and needs to be error checked
     *
     * @param name     name of the Airline
     * @param callsign callsign of the Airline
     * @param iata     two-character alphanumeric IATA code for the airline
     * @param icao     three-character alphanumeric ICAO code for the airline
     * @param country  name of the country the airline is located in
     * @throws DataConstraintsException if one or more of the values are invalid. It will attempt to set all values before failing,
     *                                  and so may contain error messages for multiple properties
     */
    public Airline(String name, String callsign, String iata, String icao, String country) throws DataConstraintsException {
        super();
        DataConstraintsException e = null;
        e = DataConstraintsException.attempt(e, () -> this.setName(name));
        e = DataConstraintsException.attempt(e, () -> this.setCallsign(callsign));
        e = DataConstraintsException.attempt(e, () -> this.setCountry(country));

        // IATA and ICAO cannot both be null, so if IATA is null, set ICAO first. Fails either way if both are null
        e = DataConstraintsException.attempt(e, () -> this.setIataIcao(iata, icao));
        if (e != null) {
            throw e;
        }
    }


    /**
     * Getter for the name of the airline
     * Name may be null if it was null in the database
     *
     * @return name the full name of the airline
     */
    public String getName() {
        return name;
    }

    /**
     * Setter for name of the airline
     *
     * @param name full name of the airline. Empty strings (after trimming) are converted to null
     * @throws DataConstraintsException exception when the (trimmed) name is shorter than three characters
     */
    public void setName(String name) throws DataConstraintsException {
        name = trimmedEmptyStringToNull(name);
        if (name == null || name.length() < 3) {
            throw new DataConstraintsException(NAME, "Name cannot be shorter than three characters");
        } else if (name.contains(";")) {
            throw new DataConstraintsException(NAME, "Name cannot contain semicolons");
        }

        this.name = name;
    }

    /**
     * Getter for the callsign of the airline
     *
     * @return callsign of the airline. May be null
     */
    public String getCallsign() {
        return callsign;
    }

    /**
     * Setter for airline callsign
     *
     * @param callsign callsign of the airline. Empty strings (after trimming) are converted to null
     */
    public void setCallsign(String callsign) throws DataConstraintsException {
        callsign = trimmedEmptyStringToNull(callsign);
        if (callsign != null && callsign.contains(";")) {
            throw new DataConstraintsException(CALLSIGN, "Callsign cannot contain semicolons");
        }
        this.callsign = callsign;
    }


    /**
     * Sets both IATA and ICAO codes. Zero or one of them can be null. Both get trimmed and converted to uppercase. Empty strings (after trimming) are converted to null
     *
     * @param iata two-character alphanumeric IATA code
     * @param icao three-character alphanumeric ICAO code
     * @throws DataConstraintsException exception if IATA or ICAO are not two- or three- character alphanumeric codes respectively
     */
    public void setIataIcao(String iata, String icao) throws DataConstraintsException {
        DataConstraintsException e = null;
        if (trimmedEmptyStringToNull(iata) == null) {
            e = DataConstraintsException.attempt(e, () -> this.setIcao(icao));
            e = DataConstraintsException.attempt(e, () -> this.setIata(iata));
        } else {
            e = DataConstraintsException.attempt(e, () -> this.setIata(iata));
            e = DataConstraintsException.attempt(e, () -> this.setIcao(icao));
        }

        if (e != null) {
            throw e;
        }
    }

    /**
     * Gets the two-character alphanumeric IATA code
     *
     * @return the iata of the airline as a string. May be null
     */
    public String getIata() {
        return iata;
    }

    /**
     * Setter for the IATA code. Gets trimmed and converted to uppercase. Empty strings (after trimming) are converted to null
     *
     * @param iata two-character alphanumeric IATA code.
     * @throws DataConstraintsException exception if IATA is not a two-character alphanumeric string (after trimming),
     *                                  or when attempting to set IATA to null while ICAO is also null
     */
    public void setIata(String iata) throws DataConstraintsException {
        iata = trimmedEmptyStringToNull(iata);
        if (this.icao == null && iata == null) {
            throw new DataConstraintsException(IATA, "IATA cannot be empty if ICAO is also empty");
        } else if (iata == null) {
            this.iata = null;
        } else if (iata.matches(isAirlineIATARegExp)) {
            this.iata = iata.toUpperCase();
        } else {
            throw new DataConstraintsException(IATA, "IATA must be an two-character alphanumeric code");
        }
    }

    /**
     * Gets the three-character alphanumeric ICAO code
     *
     * @return icao code for the airline. MAY BE AN EMPTY STRING
     */
    public String getIcao() {
        return icao;
    }

    /**
     * Setter for ICAO code. Gets trimmed and converted to uppercase. Empty strings (after trimming) are converted to null
     *
     * @param icao three-chraracter alphanumeric ICAO code
     * @throws DataConstraintsException exception if ICAO is not a three-character alphanumeric string (after trimming),
     *                                  or when attempting to set ICAO to null while IATA is also null
     */
    public void setIcao(String icao) throws DataConstraintsException {
        icao = trimmedEmptyStringToNull(icao);
        if (this.iata == null && icao == null) {
            throw new DataConstraintsException(ICAO, "ICAO cannot be empty if IATA is also empty");
        } else if (icao == null) {
            this.icao = null;
        } else if (icao.matches(isAirlineICAORegExp)) {
            this.icao = icao.toUpperCase();
        } else {
            throw new DataConstraintsException(ICAO, "ICAO must be an three-character alphanumeric code");
        }
    }

    /**
     * Getter for the country the airline is located in
     *
     * @return Name of the country the airline is located in
     */
    public String getCountry() {
        return country;
    }

    /**
     * Setter for the country the airline is located in
     *
     * @param country name of the country the airline is located in
     * @throws DataConstraintsException if the country name is null or an empty string
     */
    public void setCountry(String country) throws DataConstraintsException {
        country = trimmedEmptyStringToNull(country);
        if (country == null) {
            throw new DataConstraintsException(COUNTRY, "Country name cannot be empty");
        }
        this.country = country;
    }

    /**
     * Gets either the ICAO or IATA code
     *
     * @return ICAO or IATA code, preferring ICAO if both are set
     */
    public String getCode() {
        if (iata == null) {
            return icao;
        } else {
            return iata;
        }
    }
}