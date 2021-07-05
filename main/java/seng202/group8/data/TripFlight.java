package seng202.group8.data;

import seng202.group8.datacontroller.DataConstraintsException;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Data class storing information on a flight relating to a trip. Corresponds to the 'Trip' table in the database.
 */
public class TripFlight extends Data {
    private String sourceCode;
    public static final String SOURCE_CODE = "Source";

    private String destinationCode;
    public static final String DESTINATION_CODE = "Destination";

    private String airlineCode;
    public static final String AIRLINE_CODE = "Airline";

    private int takeoffTime;
    public static final String TAKEOFF_TIME = "FlightTakeoff";

    private LocalDate takeoffDate;
    public static final String TAKEOFF_DATE = "FlightDate";

    private String comment;
    public static final String COMMENT = "Comment";

    /**
     * Constructor for importing from the database
     *
     * @param id              unique identifier for the flight in the database
     * @param sourceCode      IATA/ICAO code of the source airport of the route
     * @param destinationCode IATA/ICAO code of the destination airport of the route
     * @param airlineCode     IATA/ICAO code of the airline of the route
     * @param takeoffTime     time the flight takes off, stored up to an accuracy of a minute
     * @param takeoffDate     date the flight takes off
     * @param comment         optional comment
     */
    public TripFlight(int id, String sourceCode, String destinationCode, String airlineCode, int takeoffTime, LocalDate takeoffDate, String comment) {
        super(id);
        this.sourceCode = sourceCode;
        this.destinationCode = destinationCode;
        this.airlineCode = airlineCode;
        this.takeoffTime = takeoffTime;
        this.takeoffDate = takeoffDate;
        this.comment = (comment == null ? "" : comment);
    }

    /**
     * This is the constructor used when importing from a CSV, or when a user is editing and or changing data
     * Setters are used and errors may be thrown if the data is invalid
     *
     * @param sourceCode      IATA/ICAO code of the source airport of the route
     * @param destinationCode IATA/ICAO code of the destination airport of the route
     * @param airlineCode     IATA/ICAO code of the airline of the route
     * @param takeoffTime     time the flight takes off
     * @param takeoffDate     date the flight takes off
     * @param comment         optional comment
     * @throws DataConstraintsException if one or more of the values are invalid. It will attempt to set all values before failing,
     *                                  and so may contain error messages for multiple properties
     */
    public TripFlight(String sourceCode, String destinationCode, String airlineCode, int takeoffTime, LocalDate takeoffDate, String comment) throws DataConstraintsException {
        super();

        DataConstraintsException e = null;
        e = DataConstraintsException.attempt(e, () -> setSourceCode(sourceCode));
        e = DataConstraintsException.attempt(e, () -> setDestinationCode(destinationCode));
        e = DataConstraintsException.attempt(e, () -> setAirlineCode(airlineCode));
        e = DataConstraintsException.attempt(e, () -> setTakeoffTime(takeoffTime));
        e = DataConstraintsException.attempt(e, () -> setTakeoffDate(takeoffDate));
        e = DataConstraintsException.attempt(e, () -> setComment(comment));

        if (e != null) {
            throw e;
        }
    }

    /**
     * This is the constructor used when importing from a CSV, or when a user is editing and or changing data
     * Setters are used and errors may be thrown if the data is invalid
     *
     * @param sourceCode      IATA/ICAO code of the source airport of the route
     * @param destinationCode IATA/ICAO code of the destination airport of the route
     * @param airlineCode     IATA/ICAO code of the airline of the route
     * @param takeoffDateTime zoned date time for the takeoff time
     * @param comment         optional comment
     * @throws DataConstraintsException if one or more of the values are invalid. It will attempt to set all values before failing,
     *                                  and so may contain error messages for multiple properties
     */
    public TripFlight(String sourceCode, String destinationCode, String airlineCode, ZonedDateTime takeoffDateTime, String comment) throws DataConstraintsException {
        super();

        DataConstraintsException e = null;
        e = DataConstraintsException.attempt(e, () -> setSourceCode(sourceCode));
        e = DataConstraintsException.attempt(e, () -> setDestinationCode(destinationCode));
        e = DataConstraintsException.attempt(e, () -> setAirlineCode(airlineCode));
        e = DataConstraintsException.attempt(e, () -> setTakeoffDateTime(takeoffDateTime));
        e = DataConstraintsException.attempt(e, () -> setComment(comment));

        if (e != null) {
            throw e;
        }
    }

    /**
     * Gets the source airport code for the route the flight is for
     *
     * @return source airport IATA/ICAO code
     */
    public String getSourceCode() {
        return sourceCode;
    }

    /**
     * Sets the source airport code for the route the flight is for
     *
     * @param code an IATA or ICAO airport code
     * @throws DataConstraintsException if it does not match the IATA/ICAO code format
     */
    public void setSourceCode(String code) throws DataConstraintsException {
        code = trimmedEmptyStringToNull(code);
        if (!airportIsValid(code)) {
            throw new DataConstraintsException(SOURCE_CODE, "Source airport code must be an IATA or ICAO code");
        }

        this.sourceCode = code.toUpperCase();
    }

    /**
     * Gets the destination airport code for the route the flight is for
     *
     * @return destination airport IATA/ICAO code
     */
    public String getDestinationCode() {
        return destinationCode;
    }

    /**
     * Sets the source airport code for the route the flight is for
     *
     * @param code an IATA or ICAO airport code
     * @throws DataConstraintsException if it does not match the IATA/ICAO code format
     */
    public void setDestinationCode(String code) throws DataConstraintsException {
        code = trimmedEmptyStringToNull(code);
        if (!airportIsValid(code)) {
            throw new DataConstraintsException(DESTINATION_CODE, "Destination airport code must be an IATA or ICAO code");
        }

        this.destinationCode = code.toUpperCase();
    }

    /**
     * Gets the airline code for the route the flight is for
     *
     * @return airline IATA/ICAO code
     */
    public String getAirlineCode() {
        return airlineCode;
    }

    /**
     * Sets the airline code for the route the flight is for
     *
     * @param code an IATA or ICAO airport code
     * @throws DataConstraintsException if it does not match the IATA/ICAO code format
     */
    public void setAirlineCode(String code) throws DataConstraintsException {
        code = trimmedEmptyStringToNull(code);
        if (code == null || !code.matches(Airline.isAirlineIATARegExp) && !code.matches(Airline.isAirlineICAORegExp)) {
            throw new DataConstraintsException(AIRLINE_CODE, "The airline code must be an IATA or ICAO code");
        }

        this.airlineCode = code.toUpperCase();
    }

    /**
     * Checks if the given string is a valid airport IATA/ICAO code
     *
     * @param code IATA/ICAO code, or null
     * @return true if valid
     */
    protected boolean airportIsValid(String code) {
        if (code == null) {
            return false;
        }

        return code.matches(Airport.isAirportIATARegExp) || code.matches(Airport.isAirportICAORegExp);
    }

    /**
     * Gets the takeoff time for the flight
     *
     * @return takeoff time in minutes since midnight UTC time
     */
    public int getTakeoffTime() {
        return takeoffTime;
    }

    /**
     * Sets the takeoff time for the flight
     *
     * @param takeoffTime time the flight takes off
     * @throws DataConstraintsException if the takeoff time is negative or more than 24 hours
     */
    public void setTakeoffTime(int takeoffTime) throws DataConstraintsException {
        if (takeoffTime < 0 || takeoffTime >= 24 * 60) {
            throw new DataConstraintsException(TAKEOFF_TIME, "Takeoff time (in minutes) must be positive and less than 24 hours long");
        }

        this.takeoffTime = takeoffTime;
    }

    /**
     * Gets the takeoff date in UTC time
     *
     * @return takeoff date
     */
    public LocalDate getTakeoffDate() {
        return takeoffDate;
    }

    /**
     * Sets the takeoff date in UTC time
     *
     * @param date takeoff date
     * @throws DataConstraintsException if date is null
     */
    public void setTakeoffDate(LocalDate date) throws DataConstraintsException {
        if (date == null) {
            throw new DataConstraintsException(TAKEOFF_DATE, "Takeoff date must not be null");
        }
        this.takeoffDate = date;
    }

    /**
     * Sets the takeoff date and time from a zoned date time object
     *
     * @param dateTime date and time of the takeoff
     */
    public void setTakeoffDateTime(ZonedDateTime dateTime) {
        ZonedDateTime utcDateTime = dateTime.withZoneSameInstant(DateTimeHelpers.utcZone); // Time may not be in UTC time, so need to convert it
        this.takeoffDate = LocalDate.of(utcDateTime.getYear(), utcDateTime.getMonth(), utcDateTime.getDayOfMonth());
        this.takeoffTime = utcDateTime.getHour() * 60 + utcDateTime.getMinute();
    }

    /**
     * Gets the comment for the flight
     *
     * @return the comment, or null
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment for the flight
     *
     * @param comment comment for the flight. Empty strings (after trimming) are converted to null
     */
    public void setComment(String comment) {
        this.comment = trimmedEmptyStringToNull(comment);
    }

    /**
     * Gets the takeoff date as a date time object in UTC time
     *
     * @return takeoff date and time in UTC
     */
    public ZonedDateTime getUTCTakeoffDateTime() {
        return DateTimeHelpers.generateUTCDateTime(takeoffDate, takeoffTime);
    }
}
