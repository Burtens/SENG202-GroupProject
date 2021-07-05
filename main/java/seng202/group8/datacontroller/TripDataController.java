package seng202.group8.datacontroller;

import seng202.group8.AlertHelper;
import seng202.group8.data.Airport;
import seng202.group8.data.Route;
import seng202.group8.data.Trip;
import seng202.group8.data.TripFlight;
import seng202.group8.io.ConstraintsError;
import seng202.group8.io.Database;
import seng202.group8.io.SortOrder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Class responsible for interfacing program with database for Trip data
 */
public class TripDataController extends DataController<Trip> {
    private static TripDataController singleton;


    /**
     * Interface that subscribers of the current trip must follow
     */
    public interface CurrentTripObserver {
        /**
         * Method called when current trip is changed or updated - saving a trip object
         * with the ID of the current trip triggers this.
         * @param trip new current trip. May be null
         */
        void currentTripChange(Trip trip);
    }

    private Trip currentlyOpenTrip;
    protected HashSet<CurrentTripObserver> currentTripObservers = new HashSet<>();
    private PreparedStatement addToDatabaseStatement = null;
    private PreparedStatement updateInDatabaseStatement = null;
    private PreparedStatement deleteFlightsForTripStatement = null;
    private PreparedStatement insertFlightsStatement = null;
    private PreparedStatement getFlightsForTripStatement = null;
    private PreparedStatement getEntityByIdStatement = null;
    private PreparedStatement getEntityByNameStatement = null;
    private PreparedStatement getAllTripNamesStatement = null;

    /**
     * Gets the singleton instance for the DataController
     *
     * @return the singleton instance
     */
    public static TripDataController getSingleton() {
        if (singleton == null) {
            singleton = new TripDataController();
        }

        return singleton;
    }

    /**
     * Initializer for the data controller
     */
    protected TripDataController() {
        super();
        Database.establishConnection();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onDBChange() {
        setCurrentlyOpenTrip(null);
        try {
            Database.establishConnection();

            tryClose(addToDatabaseStatement);
            addToDatabaseStatement = Database.databaseConnection.prepareStatement("INSERT INTO Trip(Name, Comment) VALUES (?, ?)");
            tryClose(updateInDatabaseStatement);
            updateInDatabaseStatement = Database.databaseConnection.prepareStatement("UPDATE Trip SET Name = ?, Comment = ? WHERE ID = ?");

            tryClose(deleteFlightsForTripStatement);
            deleteFlightsForTripStatement = Database.databaseConnection.prepareStatement("DELETE FROM Flight WHERE Trip = ?");

            tryClose(insertFlightsStatement);
            insertFlightsStatement = Database.databaseConnection.prepareStatement("INSERT INTO Flight(Trip, Airline, Source, Destination, FlightTakeoff, FlightDate, Comment) VALUES (?, ?, ?, ?, ?, ?, ?)");

            tryClose(getFlightsForTripStatement);
            getFlightsForTripStatement = Database.databaseConnection.prepareStatement("SELECT * FROM Flight WHERE Trip = ?");

            tryClose(getEntityByIdStatement);
            getEntityByIdStatement = Database.databaseConnection.prepareStatement("SELECT * FROM Trip WHERE ID = ?");

            tryClose(getEntityByNameStatement);
            getEntityByNameStatement = Database.databaseConnection.prepareStatement("SELECT * FROM Trip WHERE Name = ?");

            tryClose(getAllTripNamesStatement);
            getAllTripNamesStatement = Database.databaseConnection.prepareStatement("SELECT Name FROM Trip");
            return true;
        } catch (SQLException exception) {
            AlertHelper.showErrorAlert(exception);
            return false;
        }
    }


    /**
     * Not implemented. Do not call
     * @return null
     */
    @Override
    protected PreparedStatement getBatchAddToDatabaseStatement() {
        return null;
    }

    /**
     * Not implemented. Do not call
     * @return null
     */
    @Override
    public List<Trip> getSortedFilteredEntities(String sortColumn, SortOrder order, int numRows, int offset) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Trip addToDatabase(Trip data) throws SQLException, ConstraintsError {
        addToDatabaseStatement.setString(1, data.getName());
        addToDatabaseStatement.setString(2, data.getComment());

        try {
            addToDatabaseStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ConstraintsError(Database.generateUniquenessFailedErrorMessage(e));
        }

        try(ResultSet resultSet = addToDatabaseStatement.getGeneratedKeys()) {
            if (resultSet.next()) {
                int id = resultSet.getInt(1);
                insertFlights(id, data.getFlights());

                Trip updatedTrip = getEntity(id);
                if (currentlyOpenTrip != null && updatedTrip.getId() == currentlyOpenTrip.getId()) {
                    // update the current trip if ID matches the updated trips OR current trip is null
                    setCurrentlyOpenTrip(updatedTrip);
                }

                return updatedTrip;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Trip addToDatabase(Trip data, boolean returnNew) throws SQLException, ConstraintsError {
        return addToDatabase(data);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateInDatabase(Trip data) throws SQLException, ConstraintsError {
        updateInDatabaseStatement.setString(1, data.getName());
        updateInDatabaseStatement.setString(2, data.getComment());
        updateInDatabaseStatement.setInt(3, data.getId());


        try {
            updateInDatabaseStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ConstraintsError(Database.generateUniquenessFailedErrorMessage(e));
        }


        deleteFlightsForTripStatement.setInt(1, data.getId());
        deleteFlightsForTripStatement.executeUpdate();

        insertFlights(data.getId(), data.getFlights());

        if (currentlyOpenTrip != null && data.getId() == currentlyOpenTrip.getId()) {
            // Remember to update currentlyOpenTrip object if the corresponding entity in the db is changed
            setCurrentlyOpenTrip(data);
        }
    }


    /**
     * Inserts flights belonging to a trip to the database. Ensure they are not already in the DB when this is called
     *
     * @param id      ID of the trip the flights belong to
     * @param flights list of flights belonging to the trip
     * @throws SQLException if an SQL error occurs
     */
    private void insertFlights(int id, ArrayList<TripFlight> flights) throws SQLException {
        if (flights.size() == 0) {
            return;
        }

        insertFlightsStatement.setInt(1, id);
        for (TripFlight flight : flights) {
            insertFlightsStatement.setString(2, flight.getAirlineCode());
            insertFlightsStatement.setString(3, flight.getSourceCode());
            insertFlightsStatement.setString(4, flight.getDestinationCode());
            insertFlightsStatement.setInt(5, flight.getTakeoffTime());
            insertFlightsStatement.setString(6, flight.getTakeoffDate().format(DateTimeFormatter.ISO_LOCAL_DATE)); // Formats as yyyy-mm-dd
            insertFlightsStatement.setString(7, flight.getComment());
            insertFlightsStatement.addBatch();
        }

        insertFlightsStatement.executeBatch();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteFromDatabase(int id) throws SQLException {
        super.deleteFromDatabase("Trip", id);
        notifyObserversOfDeletion(id);
        if (currentlyOpenTrip != null && currentlyOpenTrip.getId() == id) {
            setCurrentlyOpenTrip(null);
        }
    }

    /**
     * Generates a Trip Object based on data from an SQL query.
     *
     * @param resultSet {@link ResultSet}
     * @return a trip data object {@link Trip}
     * @throws SQLException  If ta fatal error occurs in the database
     */
    private Trip makeTrip(ResultSet resultSet) throws SQLException {
        Trip trip = new Trip(
                resultSet.getInt(Trip.ID),
                resultSet.getString(Trip.NAME),
                resultSet.getString(Trip.COMMENT)
        );

        trip.setFlights(getFlights(trip.getId()));
        Trip.sortFlightsByTakeoffTime(trip.getFlights());

        return trip;
    }

    /**
     * Gets all the flights for a given trip
     *
     * @param tripId ID of the trip of interest
     * @return all the flights for the trip in order
     * @throws SQLException If ta fatal error occurs in the database
     */
    private ArrayList<TripFlight> getFlights(int tripId) throws SQLException {
        getFlightsForTripStatement.setInt(1, tripId);
        ArrayList<TripFlight> flights = new ArrayList<>();

        try(ResultSet resultSet = getFlightsForTripStatement.executeQuery()) {
            while (resultSet.next()) {
                flights.add(
                    new TripFlight(
                        resultSet.getInt(TripFlight.ID),
                        resultSet.getString(TripFlight.SOURCE_CODE),
                        resultSet.getString(TripFlight.DESTINATION_CODE),
                        resultSet.getString(TripFlight.AIRLINE_CODE),
                        resultSet.getInt(TripFlight.TAKEOFF_TIME),
                        LocalDate.parse(
                            resultSet.getString(TripFlight.TAKEOFF_DATE),
                            DateTimeFormatter.ISO_LOCAL_DATE
                        ),
                        resultSet.getString(TripFlight.COMMENT)
                    )
                );
            }
        }

        Trip.sortFlightsByTakeoffTime(flights);
        return flights;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Trip getEntity(int id) throws SQLException {
        getEntityByIdStatement.setInt(1, id);

        try(ResultSet resultSet = getEntityByIdStatement.executeQuery()) {
            if (resultSet.next()) {
                return makeTrip(resultSet);
            }
        }

        return null;
    }

    /**
     * Gets a trip by its name
     *
     * @param name name of the trip
     * @return trip with the given name, or null if not found
     * @throws SQLException if an SQL error occurs
     */
    public Trip getEntity(String name) throws SQLException {
        getEntityByNameStatement.setString(1, name);

        try(ResultSet resultSet = getEntityByNameStatement.executeQuery()) {
            if (resultSet.next()) {
                return makeTrip(resultSet);
            }
        }

        return null;
    }

    /**
     * Gets the names of all trips in the DB
     *
     * @return list of all trip names
     * @throws SQLException if an SQL error occurs
     */
    public List<String> getAllTripNames() throws SQLException {
        List<String> names = new ArrayList<>();

        try(ResultSet resultSet = getAllTripNamesStatement.executeQuery()) {
            while (resultSet.next()) {
                names.add(resultSet.getString(1));
            }
        }

        return names;
    }

    /**
     * Checks if the new flight is not while in the air
     *
     * @param existingFlights existing flights in a trip
     * @param newFlight       new flight to check clashes for
     * @return Message, or null if no clashes or the new flight's route is null. If the route for the existing flight is null, it will be ignored
     * @throws SQLException if error from database
     */
    public String canAddFlightWithoutClash(List<TripFlight> existingFlights, TripFlight newFlight) throws SQLException {
        Route newRoute = RouteDataController.getSingleton().getEntity(newFlight.getSourceCode(), newFlight.getDestinationCode(), newFlight.getAirlineCode());

        if (newRoute == null) {
            return null;
        }

        ZonedDateTime newFlightTakeoff = newFlight.getUTCTakeoffDateTime();
        ZonedDateTime newFlightLanding = newFlightTakeoff.plus(newRoute.getFlightDurationAsDuration());

        for (TripFlight flight : existingFlights) {
            Route route = RouteDataController.getSingleton().getEntity(flight.getSourceCode(), flight.getDestinationCode(), flight.getAirlineCode());
            if (route == null) continue;

            ZonedDateTime flightTakeoff = flight.getUTCTakeoffDateTime();
            ZonedDateTime flightLanding = flightTakeoff.plus(route.getFlightDurationAsDuration());

            if (!newFlightTakeoff.isAfter(flightLanding) && !newFlightLanding.isBefore(flightLanding) ||
                    !newFlightLanding.isBefore(flightTakeoff) && !newFlightTakeoff.isAfter(flightTakeoff)) {
                // Using ! since need <= or >= to account for if the two flights are identical
                return String.format("Clashes with flight %s → %s (%s)",
                        flight.getSourceCode(),
                        flight.getDestinationCode(),
                        flight.getUTCTakeoffDateTime().format(DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy"))
                );
            }
        }

        return null;
    }


    /**
     * Class used by tripSanityCheck to give information about if a message is a warning or error
     */
    public class WarningError {
        /**
         * If true, error. If false, warning
         */
        public boolean isError;

        /**
         * Warning or error message
         */
        public String message;

        /**
         * Initializer
         *
         * @param isError if true, error. If false, warning
         * @param message message
         */
        public WarningError(boolean isError, String message) {
            this.isError = isError;
            this.message = message;
        }

        @Override
        public String toString() {
            return (isError ? "Error: " : "Warning: ") + message;
        }
    }

    /**
     * Checks if a trip would be possible/probable, with errors such as takeoff before landing, route not existing, and warnings such as landing at one airport but having another flight a few hours later from an airport a thousand kilometers away.
     *
     * @param trip trip object with all the flights in sorted order
     * @return list of WarningErrors, with indexes correlating to the flight. If null, there was no issue for the flight
     * @throws SQLException if error from database
     */
    public ArrayList<WarningError> tripSanityCheck(Trip trip) throws SQLException {
        ZonedDateTime previousLandingTime = null; // Time the previous flight lands
        Airport previousDestinationAirport = null;

        ArrayList<WarningError> errors = new ArrayList<>();
        for (TripFlight flight : trip.getFlights()) {
            String message = null;
            Route route = RouteDataController.getSingleton().getEntity(flight.getSourceCode(), flight.getDestinationCode(), flight.getAirlineCode());
            if (route == null) {
                errors.add(new WarningError(true, "The route is not in the database"));
                continue;
            }

            Airport sourceAirport = AirportDataController.getSingleton().getEntity(route.getSourceAirportCode());
            String sourceAirportName = sourceAirport == null ? route.getSourceAirportCode() : sourceAirport.getName();

            Airport destinationAirport = AirportDataController.getSingleton().getEntity(route.getDestinationAirportCode());
            String destinationAirportName = destinationAirport == null ? route.getDestinationAirportCode() : destinationAirport.getName();

            if (sourceAirport == null) {
                errors.add(new WarningError(true, "The origin airport (" + sourceAirportName + ") is not in the database"));
                continue;
            }

            if (destinationAirport == null) {
                errors.add(new WarningError(true, "The destination airport (" + destinationAirportName + ") is not in the database"));
                continue;
            }

            List<Integer> takeoffTimes = route.getTakeoffTimes();
            int takeoffTime = flight.getTakeoffTime();
            if (!takeoffTimes.contains(takeoffTime)) {
                if (route.getTakeoffTimes().size() == 0) {
                    errors.add(new WarningError(true, "The route has no scheduled flights"));
                    continue;
                }

                int bestTime = takeoffTimes.get(0);
                for (int candidate : takeoffTimes) { // Finds the time that is nearest to the takeoff time in the flight
                    candidate = Math.abs(takeoffTime - candidate) % (60 * 24);
                    int current = Math.abs(takeoffTime - bestTime) % (60 * 24);
                    if (current < candidate) {
                        bestTime = candidate;
                    }
                }

                errors.add(new WarningError(true, String.format(
                        "The selected takeoff time, %s was not found in the route. Closest alternative: %s",
                        LocalTime.of(takeoffTime / 60, takeoffTime % 60).format(DateTimeFormatter.ofPattern("HH:mm")),
                        LocalTime.of(bestTime / 60, bestTime % 60).format(DateTimeFormatter.ofPattern("HH:mm"))
                )));
                continue;

            }

            Double distanceBetweenPreviousDestinationAndCurrentSource = null;
            if (previousDestinationAirport != null && previousDestinationAirport.getCode() != sourceAirport.getCode()) {
                // If dest/source are the same, don't care about distance
                distanceBetweenPreviousDestinationAndCurrentSource = Route.getDistanceFromLongLat(
                        previousDestinationAirport.getLatitude(), previousDestinationAirport.getLongitude(),
                        sourceAirport.getLatitude(), sourceAirport.getLongitude()
                );
            }

            boolean isInternationalFlight = sourceAirport != null && destinationAirport != null && !sourceAirport.getCountry().equals(destinationAirport.getCountry());

            if (previousLandingTime != null) { // If previous landing time null, can't do much
                Duration layoverTime = Duration.between(previousLandingTime, flight.getUTCTakeoffDateTime());
                if (layoverTime.isNegative()) {
                    errors.add(new WarningError(true, "The flight takes off before you land"));
                    continue;
                }

                if (isInternationalFlight && layoverTime.minusHours(2).isNegative()) {
                    message = "You have less than 2 hours between connections for an international flight";
                } else if (layoverTime.minusMinutes(30).isNegative()) {
                    message = "You have less than 30 minutes between connections for a domestic flight";
                } else if (distanceBetweenPreviousDestinationAndCurrentSource != null &&
                        distanceBetweenPreviousDestinationAndCurrentSource != 0 &&
                        layoverTime.minusHours(24).isNegative()) {
                    // If distance between airports is large compared with time, could be a missing flight
                    // If time between flights more than a day or so, it's probably fine
                    int hoursByCar = (int) Math.floor(distanceBetweenPreviousDestinationAndCurrentSource / 100);
                    // Reasonable estimate at how long it would take to drive
                    if (layoverTime.minusHours(hoursByCar).isNegative()) {
                        message = String.format("No connecting flight between %s and %s, which are %d km away",
                                previousDestinationAirport.getName(),
                                sourceAirportName,
                                distanceBetweenPreviousDestinationAndCurrentSource.intValue()
                        );
                    }
                }
            }

            previousLandingTime = flight.getUTCTakeoffDateTime().plus(route.getFlightDurationAsDuration());
            previousDestinationAirport = destinationAirport;

            if (message == null) {
                errors.add(null);
            } else {
                errors.add(new WarningError(false, message));
            }
        }

        return errors;
    }

    /**
     * Takes a TripFlight object and returns the landing time of that flight in UTC.
     *
     * @param flight The TripFLight object whose landing time is being queried.
     * @return The landing time of that flight in UTC.
     * @throws SQLException If the flight has no associated route
     */
    public ZonedDateTime getUTCLandingTime(TripFlight flight) throws SQLException {
        Route route = RouteDataController.getSingleton().getEntity(flight.getSourceCode(), flight.getDestinationCode(), flight.getAirlineCode());
        if (route == null) {
            throw new SQLException("No route found for this flight");
        }
        return flight.getUTCTakeoffDateTime().plusMinutes(route.getFlightDuration());
    }

    /**
     * Takes a TripFlight object and returns the price of that flight in dollars.
     *
     * @param flight The TripFLight object whose price is being queried.
     * @return Takes a TripFlight object and returns the price of that flight in dollars.
     * @throws SQLException If the flight has no associated route
     */
    public int getPrice(TripFlight flight) throws SQLException {
        Route route = RouteDataController.getSingleton().getEntity(flight.getSourceCode(), flight.getDestinationCode(), flight.getAirlineCode());
        if (route == null) {
            throw new SQLException("No route found for this flight");
        }
        return route.getPrice();
    }


    /**
     * To subscribe to changes in the current trip
     * @param observer object observing the current trip
     */
    public void subscribeToCurrentTrip(CurrentTripObserver observer) {
        currentTripObservers.add(observer);
    }

    /**
     * To unsubscribe from changes to the current trip
     * @param observer observer that may or may not have previously subscribed
     */
    public void unsubscribeFromCurrentTrip(CurrentTripObserver observer) {
        currentTripObservers.remove(observer);
    }

    /**
     * Gets the currently open trip
     * @return currently open trip. May be null
     */
    public Trip getCurrentlyOpenTrip() {
        return currentlyOpenTrip;
    }

    /**
     * Sets the currently open trip and notifies observers
     *
     * @param currentlyOpenTrip new trip that is open; shown on trip and map view
     */
    public void setCurrentlyOpenTrip(Trip currentlyOpenTrip) {
        this.currentlyOpenTrip = currentlyOpenTrip;
        for (CurrentTripObserver observer: currentTripObservers) {
            observer.currentTripChange(currentlyOpenTrip);
        }
    }
}
