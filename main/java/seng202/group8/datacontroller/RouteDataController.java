package seng202.group8.datacontroller;

import javafx.beans.property.DoubleProperty;
import seng202.group8.AlertHelper;
import seng202.group8.data.Airline;
import seng202.group8.data.Airport;
import seng202.group8.data.Route;
import seng202.group8.io.ConstraintsError;
import seng202.group8.io.Database;
import seng202.group8.io.SortOrder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Class responsible for interfacing program with database for Route data
 */
public class RouteDataController extends DataController<Route> {
    private static RouteDataController singleton;

    private AirportDataController airportDC;
    private AirlineDataController airlineDC;

    private PreparedStatement addToDatabaseStatement = null;
    private PreparedStatement batchAddToDatabaseStatement = null;
    private PreparedStatement addTakeoffTimesToDatabaseStatement = null;
    // No built in method for this :(
    private int addTakeoffTimesToDatabaseStatementSize = 0;
    private PreparedStatement getTakeoffTimesStatement = null;
    private int updateRouteStatementSize = 0;
    private PreparedStatement updateRouteStatement = null;
    private PreparedStatement deleteTakeoffTimesStatement = null;
    private PreparedStatement getEntityByIdStatement = null;
    private PreparedStatement getEntityByAirportAirlineTripletStatement = null;

    /**
     * Gets the singleton instance for the DataController
     *
     * @return the singleton instance
     */
    public static RouteDataController getSingleton() {
        if (singleton == null) {
            singleton = new RouteDataController();
        }

        return singleton;
    }

    /**
     * Initializer for the data controller
     */
    protected RouteDataController() {
        super();
        onDBChange();
        airlineDC = AirlineDataController.getSingleton();
        airportDC = AirportDataController.getSingleton();
    }

    /**
     * {@inheritDoc}
     */
    public boolean onDBChange() {
        try {
            Database.establishConnection();

            tryClose(addToDatabaseStatement);
            addToDatabaseStatement = Database.databaseConnection.prepareStatement("INSERT INTO Route(Airline, Source, Destination, Equipment, Price, Codeshare, TimeLength) VALUES (UPPER(?), UPPER(?), UPPER(?), ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);

            tryClose(batchAddToDatabaseStatement);
            batchAddToDatabaseStatement = Database.databaseConnection.prepareStatement("INSERT OR IGNORE INTO Route(Airline, Source, Destination, Equipment, Price, Codeshare, TimeLength) VALUES (UPPER(?), UPPER(?), UPPER(?), ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);

            tryClose(addTakeoffTimesToDatabaseStatement);
            addTakeoffTimesToDatabaseStatement = Database.databaseConnection.prepareStatement("INSERT INTO TakeoffTimes(Route, Time) VALUES (?, ?)");

            tryClose(getTakeoffTimesStatement);
            getTakeoffTimesStatement = Database.databaseConnection.prepareStatement("SELECT * FROM TakeoffTimes Where Route = ?");

            tryClose(updateRouteStatement);
            updateRouteStatement = Database.databaseConnection.prepareStatement("UPDATE Route SET Airline = ?, Source = ?, Destination = ?, Equipment = ?, Price = ?, Codeshare = ?, TimeLength = ? WHERE ID = ?");

            tryClose(deleteTakeoffTimesStatement);
            deleteTakeoffTimesStatement = Database.databaseConnection.prepareStatement("DELETE FROM TakeoffTimes WHERE Route = ?");

            tryClose(getEntityByIdStatement);
            getEntityByIdStatement = Database.databaseConnection.prepareStatement("SELECT * FROM Route WHERE ID = ?");

            tryClose(getEntityByAirportAirlineTripletStatement);
            getEntityByAirportAirlineTripletStatement = Database.databaseConnection.prepareStatement("SELECT * FROM Route WHERE Source = ? AND Destination = ? AND Airline = ?");

            return true;
        } catch (SQLException exception) {
            AlertHelper.showErrorAlert(exception);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getBatchAddToDatabaseStatement() {
        return batchAddToDatabaseStatement;
    }

    /**
     * Adds an airline object and its takeoff times to the database
     *
     * @param route the {@link Route} to add to the database
     * @throws SQLException     Exception if a database error occurs
     * @throws ConstraintsError Exception if a uniqueness constraint in the database is violated
     */
    @Override
    protected Route addToDatabase(Route route, boolean returnNew) throws SQLException, ConstraintsError {
        setStatementValues(addToDatabaseStatement, route);

        try {
            addToDatabaseStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ConstraintsError(Database.generateUniquenessFailedErrorMessage(e));
        }

        try (ResultSet resultSet = addToDatabaseStatement.getGeneratedKeys()) {
            if (resultSet.next()) {
                int id = resultSet.getInt(1);

                insertTakeoffTimes(id, route.getTakeoffTimes());
                if (returnNew) return getEntity(id);
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    protected Route addToDatabase(Route route) throws SQLException, ConstraintsError {
        return addToDatabase(route, true);
    }

    /**
     * Adds the given route to the batch statement, and possibly executes the batch if it reaches the maximum batch size
     * WARNING: does not add takeoff times
     *
     * @param route route to add to the batch statement
     * @throws SQLException if an SQL error occurs
     */
    public void addToBatch(Route route) throws SQLException {
        setStatementValues(batchAddToDatabaseStatement, route);
        batchAddToDatabaseStatement.addBatch();
    }

    /**
     * Executes the batched SQL Statements in the batchAddToDatabaseStatement PreparedStatement.
     * WARNING: does not add takeoff times
     *
     * @return the number of rows that were affected
     */
    @Override
    public int[] executeBatch() {
        return super.executeBatch();
    }

    /**
     * Executes the batched SQL Statements in the batchAddToDatabaseStatement PreparedStatement.
     * WARNING: does not add takeoff times
     *
     * @param isTesting If this is being run in an automated JUnit test. If true, it will not modify auto-commit settings
     * @return the number of rows that were affected
     */
    @Override
    public int[] executeBatch(boolean isTesting) {
        return super.executeBatch(isTesting);
    }

    /**
     * Binds relevant values from the given route object to the passed prepared statement
     *
     * @param batchAddToDatabaseStatement prepared statements the values will be bound to
     * @param route                       route object to bind values to
     * @throws SQLException if an SQL error occurs
     */
    private void setStatementValues(PreparedStatement batchAddToDatabaseStatement, Route route) throws SQLException {
        batchAddToDatabaseStatement.setString(1, route.getAirlineCode());
        batchAddToDatabaseStatement.setString(2, route.getSourceAirportCode());
        batchAddToDatabaseStatement.setString(3, route.getDestinationAirportCode());
        batchAddToDatabaseStatement.setString(4, route.getPlaneTypesRaw());
        batchAddToDatabaseStatement.setInt(5, route.getPrice());
        batchAddToDatabaseStatement.setString(6, route.isCodeShare() ? "Y" : "N");
        batchAddToDatabaseStatement.setInt(7, route.getFlightDuration());
    }


    /**
     * Binds relevant values from the given route object to the update route statement
     *
     * @param route route object to bind values to
     * @throws SQLException if an SQL error occurs
     */
    protected void setUpdateRouteStatementValues(Route route) throws SQLException {
        updateRouteStatement.setString(1, route.getAirlineCode());
        updateRouteStatement.setString(2, route.getSourceAirportCode());
        updateRouteStatement.setString(3, route.getDestinationAirportCode());
        updateRouteStatement.setString(4, route.getPlaneTypesRaw());
        updateRouteStatement.setInt(5, route.getPrice());
        updateRouteStatement.setString(6, route.isCodeShare() ? "Y" : "N");
        updateRouteStatement.setInt(7, route.getFlightDuration());
        updateRouteStatement.setInt(8, route.getId());
    }

    /**
     * Updates an Route object in the database
     *
     * @param route The Route object
     * @throws SQLException     Exception if a database error occurs
     * @throws ConstraintsError Exception if a uniqueness constraint in the database is violated
     */
    protected void updateInDatabase(Route route) throws SQLException {
        setUpdateRouteStatementValues(route);

        try {
            updateRouteStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ConstraintsError(Database.generateUniquenessFailedErrorMessage(e));
        }

        deleteTakeoffTimesStatement.setInt(1, route.getId());
        deleteTakeoffTimesStatement.executeUpdate();

        insertTakeoffTimes(route.getId(), route.getTakeoffTimes());
    }

    /**
     * Adds generated route stats to a batch to be executed later
     *
     * @param route The route to update in the database
     * @throws SQLException Exception if an error occurs with the database
     */
    public void updateWithGeneratedStats(Route route) throws SQLException {
        // This should only ever be called by
        setUpdateRouteStatementValues(route);
        updateRouteStatement.addBatch();
        updateRouteStatementSize++;

        if (route.getTakeoffTimes().size() == 0) {
            return;
        }

        addTakeoffTimesToDatabaseStatement.setInt(1, route.getId());
        for (int time : route.getTakeoffTimes()) {
            addTakeoffTimesToDatabaseStatement.setInt(2, time);
            addTakeoffTimesToDatabaseStatement.addBatch();
            addTakeoffTimesToDatabaseStatementSize++;
        }
    }

    /**
     * Commit the batches that contain the updated stats for routes
     *
     * @throws SQLException If an error occurs with the database or is a database constraint is violated
     */
    private void commitUpdateWithGeneratedStats() throws SQLException {
        boolean commitMode = Database.databaseConnection.getAutoCommit();
        Database.databaseConnection.setAutoCommit(false);
        if (updateRouteStatementSize != 0) {
            updateRouteStatement.executeBatch();
            updateRouteStatement.clearBatch();
            updateRouteStatementSize = 0;
        }
        if (addTakeoffTimesToDatabaseStatementSize != 0) {
            addTakeoffTimesToDatabaseStatement.executeBatch();
            addTakeoffTimesToDatabaseStatement.clearBatch();
            addTakeoffTimesToDatabaseStatementSize = 0;
        }

        Database.databaseConnection.commit();
        Database.databaseConnection.setAutoCommit(commitMode);
    }

    /**
     * Given a route, inserts all the takeoff times
     *
     * @param id           id of route the takeoffs belong to
     * @param takeoffTimes list of takeoff times to insert
     * @throws SQLException Exception if a database error occurs
     */
    private void insertTakeoffTimes(int id, List<Integer> takeoffTimes) throws SQLException {
        if (takeoffTimes.size() == 0) {
            return;
        }

        addTakeoffTimesToDatabaseStatement.setInt(1, id);
        for (int time : takeoffTimes) {
            addTakeoffTimesToDatabaseStatement.setInt(2, time);
            addTakeoffTimesToDatabaseStatement.addBatch();
        }

        addTakeoffTimesToDatabaseStatement.executeBatch();
        addTakeoffTimesToDatabaseStatement.clearBatch();
    }

    /**
     * Given a result set from a SQL `SELECT * FROM ROUTE ...` query, generates a single Route object.
     * It runs another SQL query to get the takeoff times
     * It does not move the pointer of the result set
     *
     * @param resultSet       result set from the select statement at the right pointer
     * @param getTakeoffTimes if true, queries and gets takeoff times
     * @return new Route object generated from the result set
     */
    private Route makeRoute(ResultSet resultSet, boolean getTakeoffTimes) throws SQLException {
        int id = resultSet.getInt("ID");
        String airline = resultSet.getString("Airline");
        String source = resultSet.getString("Source");
        String destination = resultSet.getString("Destination");
        String equipment = resultSet.getString("Equipment");
        int price = resultSet.getInt("Price");

        // Ensure codeshare is 'N' if NULL in database
        char codeshare = 'N';
        if (resultSet.getString("Codeshare") != null) {
            codeshare = resultSet.getString("Codeshare").charAt(0);
        }

        int time = resultSet.getInt("TimeLength");
        List<Integer> takeoffTimes = getTakeoffTimes ? getTakeoffTimes(id) : new ArrayList<>();
        return new Route(id, airline, source, destination, equipment, price, codeshare, time, takeoffTimes);
    }

    /**
     * @param sortColumn The column to sort by
     * @param order      The order (eg ascending, descending) to sort by
     * @param numRows    Maximum numbers of rows to return
     * @param offset     Offset for the rows being returned (e.g. numRows=50, offset=50 means it gets the 50-99th rows for the given sort order)
     * @return Filtered list of routes
     * @throws SQLException if error from database
     */
    public ArrayList<Route> getSortedFilteredEntities(String sortColumn, SortOrder order, int numRows, int offset) throws SQLException {
        Database.establishConnection();
        String SQLQuery = "SELECT r.*, a.Name FROM Route r LEFT JOIN (SELECT Name, IATA, ICAO FROM Airline) a ON r.Airline = a.IATA OR r.Airline = a.ICAO";

        FiltersController filters = FiltersController.getSingleton();

        if (filters != null) {
            String airline = Database.generateTextualFilterSQLText("Name", filters.getAirlineNameFilter());
            String source = Database.generateTextualFilterSQLText(Route.SOURCE_AIRPORT_CODE, filters.getStartFilter());
            String dest = Database.generateTextualFilterSQLText(Route.DESTINATION_AIRPORT_CODE, filters.getDestinationFilter());
            String price = Database.generateFilterRangeSQLTextForInt(Route.PRICE, filters.getPriceFilter().getBounds());
            String duration = Database.generateFilterRangeSQLTextForInt(Route.FLIGHT_DURATION, filters.getDurationFilter().getBounds());
            SQLQuery += Database.mergeSQLWhereClauses(airline, source, dest, price, duration);
        }

        if (sortColumn != null && order != null) {
            if (sortColumn.equals("Duration")) {
                SQLQuery += " ORDER BY TimeLength " + order.getSQLCode() + " NULLS LAST";
            } else {
                SQLQuery += " ORDER BY " + sortColumn + " " + order.getSQLCode() + " NULLS LAST";
            }

        }

        SQLQuery += String.format(" LIMIT %d OFFSET %d", numRows, offset);

        if (SQLQuery.contains(";")) {
            return null;
        }

        ArrayList<Route> routes = new ArrayList<>();

        try (PreparedStatement statement = Database.databaseConnection.prepareStatement(SQLQuery)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    routes.add(makeRoute(resultSet, false));
                }
            }
        }

        commitUpdateWithGeneratedStats();
        batchGetTakeoffTimes(routes); // makeRoute doesn't get takeoff times; way too slow
        return routes;
    }


    /**
     * Gets all the takeoff times for a given route
     *
     * @param routeId ID of the route
     * @return takeoff times for that route
     * @throws SQLException If ta fatal error occurs in the database
     */
    private ArrayList<Integer> getTakeoffTimes(int routeId) throws SQLException {
        getTakeoffTimesStatement.setInt(1, routeId);

        ArrayList<Integer> takeoffTimes = new ArrayList<>();
        try (ResultSet resultSet = getTakeoffTimesStatement.executeQuery()) {
            while (resultSet.next()) {
                int time = resultSet.getInt("Time");
                takeoffTimes.add(time);
            }
        }

        return takeoffTimes;
    }

    /**
     * Gets all the takeoff times for the given routes, clearing the current times and setting them, running a query through the DB.
     * If an error occurs while setting the takeoff time, it will fail silently
     *
     * @param routes list of routes ID of the route
     * @throws SQLException if error from database
     */
    public void batchGetTakeoffTimes(List<Route> routes) throws SQLException {
        if (routes.size() == 0) {
            return; // No routes, no takeoff times needed
        }

        Database.establishConnection();

        String sql = "SELECT * FROM TakeoffTimes";
        // this becomes expanded to SELECT * FROM TakeoffTimes WHERE Route IN (routeId1, routeId2, ...)

        List<Integer> ids = new ArrayList<>(routes.size());
        HashMap<Integer, ArrayList<Integer>> takeoffTimesMap = new HashMap<>(); // id: takeoff times array
        // Need this so that when we get the routes back, we can map the route the takeoff time belongs to to an array of takeoff times for that route
        for (Route route : routes) {
            ids.add(route.getId());
            takeoffTimesMap.put(route.getId(), new ArrayList<>());
        }

        sql += Database.mergeSQLWhereClauses(Database.generateIdFilterSQLText("Route", ids));

        try (Statement statement = Database.databaseConnection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                while (resultSet.next()) {
                    // Find the route the takeoff time belongs to, and add it to the array
                    int time = resultSet.getInt("Time");
                    int routeId = resultSet.getInt("Route");
                    takeoffTimesMap.get(routeId).add(time);
                }
            }
        }

        for (Route route : routes) {
            try {
                route.setTakeoffTimes(takeoffTimesMap.get(route.getId()));
            } catch (DataConstraintsException e) {
                // This should never happen as only valid data should be able to get into the database
                AlertHelper.showErrorAlert(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteFromDatabase(int id) throws SQLException {
        super.deleteFromDatabase("Route", id);
        notifyObserversOfDeletion(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Route getEntity(int id) throws SQLException {
        getEntityByIdStatement.setInt(1, id);

        try (ResultSet resultSet = getEntityByIdStatement.executeQuery()) {
            if (resultSet.next()) {
                return makeRoute(resultSet, true);
            }
        }

        return null;
    }

    /**
     * Gets a route from the combination of the source and destination airport codes and the airline code
     *
     * @param sourceAirportCode      IATA or ICAO code of the source airport
     * @param destinationAirportCode IATA or ICAO code of the destination airport
     * @param airlineCode            IATA or ICAO code of the airline that operates the route
     * @return route object that matches the criteria, or null
     * @throws SQLException error connecting to database, or some similar unrecoverable error
     */
    public Route getEntity(String sourceAirportCode, String destinationAirportCode, String airlineCode) throws SQLException {
        getEntityByAirportAirlineTripletStatement.setString(1, sourceAirportCode);
        getEntityByAirportAirlineTripletStatement.setString(2, destinationAirportCode);
        getEntityByAirportAirlineTripletStatement.setString(3, airlineCode);

        try (ResultSet resultSet = getEntityByAirportAirlineTripletStatement.executeQuery()) {
            if (resultSet.next()) {
                return makeRoute(resultSet, true);
            }
        }

        return null;
    }

    /**
     * Checks that a route is allowed: airline/airports exist, source and destination airports not equal
     *
     * @param sourceAirportCode      source airport IATA/ICAO code
     * @param destinationAirportCode destination airport IATA/ICAO code
     * @param airlineCode            IATA/ICAO code of the airline that operates the route
     * @return null if no errors, or a string if there is an error
     * @throws SQLException if error from database
     */
    public String routeSanityCheck(String sourceAirportCode, String destinationAirportCode, String airlineCode) throws SQLException {
        Airport source = airportDC.getEntity(sourceAirportCode);
        Airport destination = airportDC.getEntity(destinationAirportCode);
        Airline airline = airlineDC.getEntity(airlineCode);

        if (source == null) {
            return "Origin airport is not in the database";
        } else if (destination == null) {
            return "Destination airport is not in the database";
        } else if (airline == null) {
            return "Airline is not in the database";
        } else if (source.getCode().equals(destination.getCode())) {
            // source could use IATA, destination ICAO, so can't just compare what is given
            return "Origin and destination airports are the same";
        }

        return null;
    }

    /**
     * Checks that a route is allowed: airline/airports exist, source and destination airports not equal
     *
     * @param route route to run the check on
     * @return null if no errors, or a string if there is an error
     * @throws SQLException if error from database
     */
    public String routeSanityCheck(Route route) throws SQLException {
        return routeSanityCheck(route.getSourceAirportCode(), route.getDestinationAirportCode(), route.getAirlineCode());
    }

    /**
     * This method auto generates values (takeoff times, price, duration) for all routes it can (source/destination airports exist) where price is zero.
     * <p>
     * If this method is not called, then these values will be generated the first time routes are loaded which causes problems.
     * <p>
     * If you sort by duration, the duration is initially 0 but by the time it gets to the table, duration is generated and so every single time you click update filters different routes pop up.
     * <p>
     * This also it allows two databases with the same data to end up having different takeoff times and prices if you don't go view each route in the table because random number generators are used (this could be fixed by generating the seed from the source/destination/airport code combination).
     * <p>
     * WARNING: this method disables autocommit
     * @param progress a rough progress counter that is updated as values are autogenerated. The value is set to 0.5 at the start and ends it at 1
     * @throws SQLException if database error occurs while retreiving, saving or commiting changes
     */
    public void autoGenerateValuesForAllRoutesWithPriceZero(DoubleProperty progress) throws SQLException {
        String sql = "SELECT Route.*, Source.Latitude srcLat, Source.Longitude srcLng, Destination.Latitude dstLat, Destination.Longitude dstLng FROM Route\n" +
                "JOIN Airport Source ON Route.Source = Source.IATA OR Route.Source = Source.ICAO\n" +
                "JOIN Airport Destination ON Route.Destination = Destination.IATA OR Route.Destination = Destination.ICAO\n" +
                "WHERE Route.Price = 0;";

        int updateNum = 0;
        int numberOfRows = -1;
        if (progress != null) {
            String countSql = "SELECT COUNT(*) FROM Route\n" +
                    "JOIN Airport Source ON Route.Source = Source.IATA OR Route.Source = Source.ICAO\n" +
                    "JOIN Airport Destination ON Route.Destination = Destination.IATA OR Route.Destination = Destination.ICAO\n" +
                    "WHERE Route.Price = 0;";
            try (Statement countStatement = Database.databaseConnection.createStatement()) {
                try(ResultSet countResultSet = countStatement.executeQuery(countSql)){
                    if(countResultSet.next())
                        numberOfRows = countResultSet.getInt(1);
                }
            }
        }


        try (Statement statement = Database.databaseConnection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("ID");
                    String airline = resultSet.getString("Airline");
                    String source = resultSet.getString("Source");
                    String destination = resultSet.getString("Destination");
                    String equipment = resultSet.getString("Equipment");

                    // Ensure codeshare is 'N' if NULL in database
                    char codeshare = 'N';
                    if (resultSet.getString("Codeshare") != null) {
                        codeshare = resultSet.getString("Codeshare").charAt(0);
                    }

                    double srcLat = resultSet.getDouble("srcLat");
                    double srcLng = resultSet.getDouble("srcLng");
                    double dstLat = resultSet.getDouble("dstLat");
                    double dstLng = resultSet.getDouble("dstLng");


                    double distance = Route.getDistanceFromLongLat(srcLat, srcLng, dstLat, dstLng);
                    int duration = 0;
                    int price = 0;
                    ArrayList<Integer> takeoffTimes = new ArrayList<>();
                    if (distance > 0.1) {
                        // Division by zero could happen
                        duration = Route.generateFlightDuration(distance, Route.PLANE_SPEED);
                        price = Route.generatePrice(Route.TIME_TO_COST, duration);
                        takeoffTimes = Route.generateTakeoffTimes(duration);
                    }

                    Route route = new Route(id, airline, source, destination, equipment, price, codeshare, duration, takeoffTimes);
                    updateWithGeneratedStats(route);
                    updateNum ++;
                    // Don't want to update the progress bar too often
                    if (progress != null)
                        if (updateNum % 500 == 0)
                            progress.set(0.5 + ((updateNum / (double)numberOfRows)) * 0.4);

                }

                // This represents the final 10% of the progress bar
                commitUpdateWithGeneratedStats();
                if (progress != null)
                    progress.set(1);
            }
        }
    }

    public void autoGenerateValuesForAllRoutesWithPriceZero() throws SQLException {
        autoGenerateValuesForAllRoutesWithPriceZero(null);
    }
}

