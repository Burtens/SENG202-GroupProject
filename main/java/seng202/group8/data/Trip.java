package seng202.group8.data;

import seng202.group8.datacontroller.DataConstraintsException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/*
Table: Trip
    - ID: Unique, Primary key, Auto-incrementing integer
    - Name: Text, Unique
    - Comment: Text, Can be null

Table: Flight
    - ID: Unique, Primary key, Auto-incrementing integer
    - Trip: Foreign key
    - Route: Foreign key (Route ID), Can be null
    - FlightTakeoff: Integer (Time in minutes)
    - FlightDate: Date
    - Comment: Text, Can be null
 */

/**
 * Data class storing information on a trip and its corresponding flights. Corresponds to the 'Trip' and 'Flight` tables in the database.
 */
public class Trip extends Data {
    private String name;
    public static final String NAME = "Name";

    private String comment;
    public static final String COMMENT = "Comment";

    private ArrayList<TripFlight> flights;
    public static final String FLIGHTS = "FlightsNotAnActualColumn";

    /**
     * Constructor used for importing from the database
     * @param id unique identifier of the Trip
     * @param name name of the trip
     * @param comment optional comment for the trip
     */
    public Trip(int id, String name, String comment) {
        super(id);
        this.name = name;
        this.comment = (comment == null ? "" : comment);

        flights = new ArrayList<>();
    }

    /**
     * This is the constructor used when importing from a CSV, or when a user is editing and or changing data
     * Setters are used and errors may be thrown if the data is invalid
     *
     * @param name    name of the trip
     * @param comment optional comment for the trip
     * @throws DataConstraintsException if one or more of the values are invalid. It will attempt to set all values before failing,
     *                                  and so may contain error messages for multiple properties
     */
    public Trip(String name, String comment) throws DataConstraintsException {
        DataConstraintsException e = null;
        e = DataConstraintsException.attempt(e, () -> setName(name));
        e = DataConstraintsException.attempt(e, () -> setComment(comment));

        if (e != null) {
            throw e;
        }

        flights = new ArrayList<>();
    }

    /**
     * Gets the name of the trip
     *
     * @return the name of the trip
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the trip. Must be at least three characters long
     *
     * @param name name of the trip. Empty strings (after trimming) are converted to null
     * @throws DataConstraintsException exception when the (trimmed) name is shorter than three characters
     */
    public void setName(String name) throws DataConstraintsException {
        name = trimmedEmptyStringToNull(name);
        if (name == null || name.length() < 3) {
            throw new DataConstraintsException(NAME, "Name cannot be shorter than three characters long");
        }

        this.name = name;
    }

    /**
     * Gets the comment for the trip
     *
     * @return the comment, or null
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment for the trip
     *
     * @param comment comment for the trip. Empty strings (after trimming) are converted to null
     */
    public void setComment(String comment) {
        this.comment = trimmedEmptyStringToNull(comment);
    }

    /**
     * Adds a flight to the trip and sorts the list of flights
     *
     * @param flight flight to add to the list
     */
    public void addFlight(TripFlight flight) {
        this.flights.add(flight);
        sortFlightsByTakeoffTime(flights);
    }

    /**
     * Adds a multiple flights to the trip
     * @param flights flights to add to list
     */
    public void addFlights(List<TripFlight> flights) {
        for (TripFlight flight : flights) {
            this.addFlight(flight);
        }
    }

    /**
     * Sorts a list of flights by their takeoff time in-place
     *
     * @param flights list of flights to sort
     */
    public static void sortFlightsByTakeoffTime(List<TripFlight> flights) {
        Collections.sort(flights, Comparator.comparingLong(o -> o.getUTCTakeoffDateTime().toEpochSecond()));
    }

    /**
     * Overwrites the existing flight array with a new list. NOT AUTOMATICALLY SORTED
     *
     * @param flights new flights array
     */
    public void setFlights(ArrayList<TripFlight> flights) {
        this.flights = flights;
    }

    /**
     * Returns a read-write list of flights belonging to the trip
     *
     * @return list of flights that is used internally. Changes to this will propagate to the trip
     */
    public ArrayList<TripFlight> getFlights() {
        return flights;
    }
}
