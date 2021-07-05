package seng202.group8.data;

import seng202.group8.datacontroller.AirportDataController;
import seng202.group8.datacontroller.DataConstraintsException;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static java.lang.Math.max;


/**
 * Data class storing information on a route and its corresponding takeoff times. Corresponds to the 'Route' and 'TakeoffTimes` tables in the database.
 */
public class Route extends Data {

    private String airlineCode;
    public static final String AIRLINE_CODE = "Airline"; // Code of airline. Not null

    // Airline is FK but source/destination are not so cannot guarantee there is a related object. Thus, storing the string
    private String sourceAirportCode = ""; // three letter IATA or 4 letter ICAO code. Not null
    public static final String SOURCE_AIRPORT_CODE = "Source";

    private String destinationAirportCode = ""; // three letter IATA or 4 letter ICAO code. Not null
    public static final String DESTINATION_AIRPORT_CODE = "Destination";

    private String planeTypes = ""; // List of three letter codes for plane types used; a space separated list
    public static final String PLANE_TYPES = "Equipment"; // Space separated list

    private int price; // Price of the flight. Not null
    public static final String PRICE = "Price";

    private char isCodeShare = IS_NOT_CODE_SHARE_CHAR;
    public static final String IS_CODE_SHARE = "Codeshare"; // 'Y' if codeshare, empty otherwise
    public static final char IS_CODE_SHARE_CHAR = 'Y'; // Code share if y
    public static final char IS_NOT_CODE_SHARE_CHAR = 'N'; // not an object, so needs to have a non-empty value

    private int flightDuration; // Duration of the flight in minutes. Not null
    public static final String FLIGHT_DURATION = "TimeLength";

    private ArrayList<Integer> takeoffTimes; // Sorted list of the times the flight takes off each day, in minutes from midnight UTC time
    public static final String TAKEOFF_TIMES = "NotAnActualRowInRoutesTakeoffTimes";

    //May want to add custom time to cost and plane speed later
    public static final int TIME_TO_COST = 162; // $/hr
    public static final int PLANE_SPEED = 900; // km/h


    /**
     * Regexp which checks if the code is an valid ICAO or IATA code
     */
    public static final String IS_AIRPORT_ICAO_OR_IATA_REG_EXP = "^[A-Za-z0-9]{3,4}$";

    /**
     * Regexp which checks if the plane type string is valid (three/four character, alphanumeric)
     */
    public static final String IS_PLANE_TYPE_REG_EXP = "^[A-Za-z0-9]{3,4}$";

    /**
     * Constructor used for importing from the database
     *
     * @param id                     unique identifier for the row in the database
     * @param airlineCode            code of the airline the route belongs to
     * @param sourceAirportCode      IATA or IACO code of the origin airport
     * @param destinationAirportCode IATA or IACO code of the destination airport
     * @param planeTypes             Space separated list of three character alphanumeric codes denoting the type of planes that are usually used on the flight
     * @param price                  price of the flight
     * @param codeShare              'Y' char if codeshare, any other char if not
     * @param flightDuration         duration of the flight in minutes
     * @param takeoffTimes           array of TakeoffTime objects for this route
     * @throws SQLException if error saving to database
     */
    public Route(int id, String airlineCode, String sourceAirportCode, String destinationAirportCode, String planeTypes,
                 int price, char codeShare, int flightDuration, List<Integer> takeoffTimes) {
        super(id);
        this.airlineCode = airlineCode;
        this.sourceAirportCode = sourceAirportCode;
        this.destinationAirportCode = destinationAirportCode;
        this.planeTypes = planeTypes;
        this.isCodeShare = codeShare;

        this.price = price;
        this.flightDuration = flightDuration;

        this.takeoffTimes = new ArrayList<>(takeoffTimes.size());
        for (Integer time : takeoffTimes) {
            if (time != null) {
                this.takeoffTimes.add(time);
            }
        }
        Collections.sort(this.takeoffTimes); // Sort by takeoff time
    }

    /**
     * This is the constructor used when importing from a CSV, or when a user is editing and or changing data
     * Setters are used and errors may be thrown if the data is invalid
     *
     * @param airline                code of airline the route belongs to
     * @param sourceAirportCode      IATA or IACO code of the origin airport
     * @param destinationAirportCode IATA or IACO code of the destination airport
     * @param planeTypes             Space separated list of three character alphanumeric codes denoting the type of planes that are usually used on the flight
     * @param price                  price of the flight
     * @param isCodeShare            true if the flight is a code share flight
     * @param flightDuration         duration of the flight in minutes
     * @param takeoffTimes           times the flight takes off, in minutes since midnight UTC
     * @throws DataConstraintsException if one or more of the values are invalid. It will attempt to set all values before failing,
     *                                  and so may contain error messages for multiple properties
     * @throws SQLException if error saving to database
     */
    public Route(String airline, String sourceAirportCode, String destinationAirportCode, String[] planeTypes,
                 int price, boolean isCodeShare, int flightDuration, List<Integer> takeoffTimes) throws DataConstraintsException {
        super();
        this.takeoffTimes = new ArrayList<>();


        // import from csv, generate price, flightDuration and takeoffTimes
        DataConstraintsException e = null;
        e = DataConstraintsException.attempt(e, () -> this.setAirlineCode(airline));
        e = DataConstraintsException.attempt(e, () -> this.setSourceAirportCode(sourceAirportCode));
        e = DataConstraintsException.attempt(e, () -> this.setDestinationAirportCode(destinationAirportCode));
        e = DataConstraintsException.attempt(e, () -> this.setPlaneTypes(planeTypes));
        e = DataConstraintsException.attempt(e, () -> this.setPrice(price));
        e = DataConstraintsException.attempt(e, () -> this.setFlightDuration(flightDuration));
        List<Integer> finalTakeoffTimes = takeoffTimes;
        e = DataConstraintsException.attempt(e, () -> this.setTakeoffTimes(finalTakeoffTimes));

        this.setCodeShare(isCodeShare);

        if (e != null) {
            throw e;
        }
    }

    /**
     * Generates takeoff times
     *
     * @param flightDuration The duration of the flight whose takeoff times are to added
     * @return array of takeoff times
     */
    public static ArrayList<Integer> generateTakeoffTimes(int flightDuration) {
        ArrayList<Integer> takeoffTimes = new ArrayList<>();
        Random random = new Random();

        int time = random.nextInt(24 * 60);
        time = (time / 15) * 15;    // Round to nearest 15 minutes

        double gapTime = max((random.nextDouble() + 0.8) * flightDuration, 30); // Minimum gap of 30 minutes
        long roundedGapTime = Math.round(gapTime / 15) * 15;    // Round to nearest 15 minutes

        while (time < 24 * 60) {
            takeoffTimes.add(time);
            time += roundedGapTime;
        }

        return takeoffTimes;
    }

    /**
     * Converts an list of integers into an int array
     *
     * @param integers list of Integers; elements cannot be null
     * @return int array in the same order
     */
    protected int[] toIntArray(List<Integer> integers) {
        int[] numArr = new int[integers.size()];
        for (int i = 0; i < numArr.length; i++) {
            numArr[i] = integers.get(i);
        }
        return numArr;
    }

    /**
     * Gets a sorted list of takeoff times
     *
     * @return sorted list of takeoff times. This is a copy, making changes will not affect the Route's internal list
     */
    public List<Integer> getTakeoffTimes() {
        ArrayList<Integer> copy = new ArrayList<Integer>(takeoffTimes);
        return (List<Integer>) copy;
    }

    /**
     * Sets takeoff times for the flight, copying the contents of the array to its internal representation
     * Recommended that `checkTakeoffTimesValid` is called
     *
     * @param takeoffTimes list of takeoff times
     * @throws DataConstraintsException if one or more takeoff times are invalid
     */
    public void setTakeoffTimes(List<Integer> takeoffTimes) throws DataConstraintsException {
        if (takeoffTimes == null) {
            throw new DataConstraintsException(TAKEOFF_TIMES, "Takeoff times cannot be null");
        }
        if (checkTakeoffTimesValid(takeoffTimes) != null) {
            throw new DataConstraintsException(TAKEOFF_TIMES, "Error in one or more takeoff times");
        }

        this.takeoffTimes.clear();
        this.takeoffTimes.addAll(takeoffTimes);

        Collections.sort(this.takeoffTimes);
    }

    /**
     * Checks if a list of takeoff times are valid or not
     *
     * @param times list of times
     * @return NULL if there are no errors. If not, a list of error messages with the index of the list corresponding to the index of the time
     */
    public static List<String> checkTakeoffTimesValid(List<Integer> times) {
        ArrayList<String> errors = new ArrayList<>(times.size());
        boolean errorOccurred = false;

        for (int i = 0; i < times.size(); i++) {
            if (times.get(i) == null) {
                errorOccurred = true;
                errors.add("Received null as takeoff time");
                continue;
            }

            int time = times.get(i);
            String message = null;
            // Tests that time is in valid range
            if (time < 0 || 24 * 60 <= time) {
                errorOccurred = true;
                message = "Takeoff time must be between 0 minutes (inclusive) and 24 hours (exclusive)";
            }
            // Finds duplicates: the first one instance can stay but the next one cannot
            // If both duplicate and invalid, the first one will have the invalid time error and the others
            for (int j = 0; j < i; j++) {
                if (times.get(i).equals(times.get(j))) {
                    errorOccurred = true;
                    message = "Duplicate takeoff time encountered";
                    break;
                }
            }

            errors.add(message);
        }

        return errorOccurred ? errors : null;
    }

    /**
     * Gets the code of the airline the route belongs to
     *
     * @return code of the airline the route belongs to
     */
    public String getAirlineCode() {
        return airlineCode;
    }

    /**
     * Sets the code of airline the route belongs to
     *
     * @param airline code of airline the route belongs to. Gets trimmed and if empty string, gets interpreted as null.
     * @throws DataConstraintsException if it is not a valid airline IATA or ICAO code
     */
    public void setAirlineCode(String airline) throws DataConstraintsException {
        airline = trimmedEmptyStringToNull(airline);
        if (airline == null) {
            throw new DataConstraintsException(AIRLINE_CODE, "Airline cannot be null");
        } else if (!airline.matches(Airline.isAirlineICAORegExp) && !airline.matches(Airline.isAirlineIATARegExp)) {
            throw new DataConstraintsException(AIRLINE_CODE, "Airline must be a valid IATA or ICAO code");
        }

        this.airlineCode = airline;
    }

    /**
     * Parses a given airport code for validity, throwing errors if not
     *
     * @param airportCode  three or four character alphanumeric code. Whitespace is trimmed if present, if empty
     *                     string  gets interpreted as null.
     * @param propertyName name of the property used when throwing the DataConstraintsException
     * @return trimmed string converted to uppercase, if valid
     * @throws DataConstraintsException if the passed code is invalid
     */
    private String parseAirportCode(String airportCode, String propertyName) throws DataConstraintsException {
        airportCode = trimmedEmptyStringToNull(airportCode);
        if (airportCode == null || !airportCode.matches(IS_AIRPORT_ICAO_OR_IATA_REG_EXP)) {
            throw new DataConstraintsException(propertyName, "Airport IATA/ICAO code must be 3-4 characters long and be alphanumeric");
        }

        return airportCode.toUpperCase();
    }

    /**
     * Gets the three character IATA or four letter ICAO code for the origin airport
     *
     * @return origin airline code
     */
    public String getSourceAirportCode() {
        return sourceAirportCode;
    }

    /**
     * Sets the code for the source airport
     * @param sourceAirportCode three character IATA or four letter ICAO code
     * @throws DataConstraintsException if not a three or four letter alphanumeric code
     */
    public void setSourceAirportCode(String sourceAirportCode) throws DataConstraintsException {
        this.sourceAirportCode = parseAirportCode(sourceAirportCode, SOURCE_AIRPORT_CODE);
    }

    /**
     * Gets the three character IATA or four letter ICAO code for the destination airport
     *
     * @return destination airline code
     */
    public String getDestinationAirportCode() {
        return destinationAirportCode;
    }

    /**
     * Sets the code for the destination airport
     *
     * @param destinationAirportCode three character IATA or four letter ICAO code
     * @throws DataConstraintsException if not a three or four letter alphanumeric code
     */
    public void setDestinationAirportCode(String destinationAirportCode) throws DataConstraintsException {
        this.destinationAirportCode = parseAirportCode(destinationAirportCode, DESTINATION_AIRPORT_CODE);
    }

    /**
     * Gets a array of plane types, each being a three character code
     *
     * @return array of plane types. Returns an empty array if there are none
     */
    public String[] getPlaneTypes() {
        if (planeTypes == null) {
            return new String[]{};
        }

        return splitPlaneTypesString(planeTypes);
    }

    /**
     * Gets the list of plane types as a space-separated list. May be null
     *
     * @return String of plane types, or null if there are no planes
     */
    public String getPlaneTypesRaw() {
        return planeTypes;
    }

    /**
     * Splits the space separated string that contains the plane types into a array
     *
     * @param planeTypes space separated string
     * @return array from the split string
     */
    public static String[] splitPlaneTypesString(String planeTypes) {
        return planeTypes.trim().split("\\s+");
    }

    /**
     * Checks if the plane type string is valid
     *
     * @param type plane type string: 3 character alphanumeric string (after trimming). If empty
     *             string gets interpreted as null.
     * @return error message if invalid, null if valid
     */
    public String checkIfPlaneTypeIsValid(String type) {
        type = trimmedEmptyStringToNull(type);
        if (type == null) {
            return "Plane types cannot be empty";
        }

        if (!type.matches(IS_PLANE_TYPE_REG_EXP)) {
            return String.format("Plane types must all be three character, alphanumeric codes: got '%s'", type);
        }
        return null;
    }

    /**
     * Sets the plane types; each must be a three character, alphanumeric string (after trimming)
     *
     * @param planeTypes array of plane type strings. May be null
     * @throws DataConstraintsException if not all values are three character, alphanumeric codes (after trimming)
     */
    public void setPlaneTypes(String[] planeTypes) throws DataConstraintsException {
        // Saves the result as a space separated list, like OpenFlights
        if (planeTypes == null || planeTypes.length == 0) {
            this.planeTypes = null;
            return;
        }

        String result = "";
        for (int i = 0; i < planeTypes.length; i++) {
            String type = planeTypes[i];
            if (type != null && !type.equals("")) {
                String error = checkIfPlaneTypeIsValid(type);
                if (error != null) {
                    throw new DataConstraintsException(PLANE_TYPES, error);
                }
                type = type.trim(); // Can't trim initially as may be null
                result += (i == 0) ? type : " " + type; // Space at the beginning of the string if it is not the first code
            }
        }

        this.planeTypes = result;
    }

    /**
     * Sets the plane types; each must be a three character, alphanumeric string (after trimming)
     *
     * @param planes String comma-separated plane types, or null
     * @throws DataConstraintsException if not all values are three character, alphanumeric codes (after trimming)
     */
    public void setPlaneTypes(String planes) throws DataConstraintsException {
        planes = trimmedEmptyStringToNull(planes);
        if (planes == null) {
            this.planeTypes = null;
        } else {
            String[] planeTypes = splitPlaneTypesString(planes);
            setPlaneTypes(planeTypes);
        }
    }

    /**
     * Gets the price of the flight (NZD)
     *
     * @return price of the flight
     */
    public int getPrice() {
        return price;
    }

    /**
     * Sets the price of the flight (NZD)
     *
     * @param price, must be positive and non-zero
     * @throws DataConstraintsException if price is negative
     */
    public void setPrice(int price) throws DataConstraintsException {
        if (price < 0) {
            throw new DataConstraintsException(PRICE, "Price must be positive");
        }
        this.price = price;
    }

    /**
     * Checks if the route is a code-share - the airline does not operate the flight
     *
     * @return true if it is codeshare
     */
    public boolean isCodeShare() {
        return isCodeShare == IS_CODE_SHARE_CHAR; // 'Y' denotes code share, empty otherwise
    }

    /**
     * Sets if route is a code share route
     *
     * @param codeShare if true, is code share
     */
    public void setCodeShare(boolean codeShare) {
        isCodeShare = codeShare ? IS_CODE_SHARE_CHAR : IS_NOT_CODE_SHARE_CHAR;
    }

    /**
     * Sets if route is code share route
     *
     * @param codeShare if 'Y', is code share
     */
    public void setCodeShare(char codeShare) {
        isCodeShare = codeShare == IS_CODE_SHARE_CHAR ? codeShare : IS_NOT_CODE_SHARE_CHAR;
    }

    /**
     * Gets the duration of the flight
     *
     * @return duration of the flight in minutes
     */
    public int getFlightDuration() {
        return flightDuration;
    }

    /**
     * Gets the duration of a flight as a Duration object
     *
     * @return duration of the flight
     */
    public Duration getFlightDurationAsDuration() {
        return Duration.ofMinutes(flightDuration);
    }

    /**
     * Sets the duration of the flight in minutes
     *
     * @param flightDuration new duration of the flight - must be less than 24 hours
     * @throws DataConstraintsException if the flight is longer than 24 hours or negative
     */
    public void setFlightDuration(int flightDuration) throws DataConstraintsException {
        if (flightDuration < 0 || flightDuration >= 24 * 60) {
            throw new DataConstraintsException(FLIGHT_DURATION, "Flight length (in minutes) must be positive and less than 24 hours long");
        }
        this.flightDuration = flightDuration;
    }

    /**
     * Generates the flight duration for a route.
     *
     * @param routeDistance distance between source and destination airports in kilometers
     * @param planeSpeed             speed of aircraft in km/h.
     * @return an int representing the duration for a flight along this route.
     */
    public static int generateFlightDuration(double routeDistance, int planeSpeed) {
        return (int) (routeDistance / planeSpeed * 60);
    }

    /**
     * Generates price for a route
     * @param timeToCost multiplier to turn flight time into a start price
     * @param flightDuration duration of the flight in minutes
     * @return newPrice price generated for the flight
     */
    public static int generatePrice(int timeToCost, int flightDuration) {
        double newPrice;
        newPrice = ((double) flightDuration / 60) * timeToCost; // Flight duration in min -> hrs * $/hr = $
        newPrice += ((2 * Math.random() - 1) * newPrice * 0.1); // random between -10% and 10%
        return (int) newPrice;
    }

    /**
     * Calculates distance between two coordinates
     * References https://stackoverflow.com/questions/27928/calculate-distance-between-two-latitude-longitude-points-haversine-formula
     *
     * @param lat1  latitude of start in degrees
     * @param long1 longitude of start in degrees
     * @param lat2  latitude of end in degrees
     * @param long2 longitude of end in degrees
     * @return distance between coordinates in km
     */
    public static double getDistanceFromLongLat(double lat1, double long1, double lat2, double long2) {
        int radius = 6371; // km
        double dlat = degreeToRadians(lat2 - lat1);
        double dlong = degreeToRadians(long2 - long1);
        double haversine = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                Math.cos(degreeToRadians(lat1)) * Math.cos(degreeToRadians(lat2)) *
                        Math.sin(dlong / 2) * Math.sin(dlong / 2);
        return 2 * radius * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    /**
     * Calculates the distance between the source and destination airport. Requires DB access
     * @param sourceAirportCode source IATA/ICAO airport code
     * @param destinationAirportCode destination IATA/ICAO airport code
     * @return distance between airports in kilometers as the crow flies, or 0 if either the source or destination airport can't be found
     * @throws SQLException if database error occurs while fetching source or destination airport
     */
    public static double calculateDistanceBetweenAirports(String sourceAirportCode, String destinationAirportCode) throws SQLException {
        AirportDataController airportData = AirportDataController.getSingleton();
        Airport start = airportData.getEntity(sourceAirportCode);
        if (start == null) return 0;
        Airport end = airportData.getEntity(destinationAirportCode);
        if (end == null) return 0;

        return getDistanceFromLongLat(
                start.getLatitude(), start.getLongitude(),
                end.getLatitude(), end.getLongitude()
        );
    }

    /**
     * Converts degrees to radians
     *
     * @param degree angle in degrees
     * @return angle converted to radians
     */
    protected static double degreeToRadians(double degree) {
        return degree * Math.PI / 180;
    }
}
