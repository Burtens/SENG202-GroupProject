package seng202.group8.datacontroller;

import seng202.group8.AlertHelper;
import seng202.group8.data.Airport;
import seng202.group8.data.DSTType;
import seng202.group8.io.ConstraintsError;
import seng202.group8.io.Database;
import seng202.group8.io.SortOrder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible for interfacing program with database for Airport data
 */
public class AirportDataController extends DataController<Airport> {
    private static AirportDataController singleton;

    private PreparedStatement addToDatabaseStatement = null;
    private PreparedStatement batchAddToDatabaseStatement = null;
    private PreparedStatement updateInDatabaseStatement = null;
    private PreparedStatement getEntityFromIDStatement = null;
    private PreparedStatement getEntityFromCodeStatement = null;
    private PreparedStatement getAllEntitiesStatement = null;

    /**
     * Gets the singleton instance for the DataController
     *
     * @return the singleton instance
     */
    public static AirportDataController getSingleton() {
        if (singleton == null) {
            singleton = new AirportDataController();
        }
        return singleton;
    }

    /**
     * Initializer for the data controller; protected as it is a singleton
     */
    protected AirportDataController() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public boolean onDBChange() {
        try {
            Database.establishConnection();

            tryClose(addToDatabaseStatement);
            addToDatabaseStatement = Database.databaseConnection.prepareStatement("INSERT INTO Airport(Name, City, Country, IATA, ICAO, Latitude, Longitude, Altitude, Timezone, DST) VALUES (?, ?, ?, UPPER(?), UPPER(?), ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);

            tryClose(batchAddToDatabaseStatement);
            batchAddToDatabaseStatement = Database.databaseConnection.prepareStatement("INSERT OR IGNORE INTO Airport(Name, City, Country, IATA, ICAO, Latitude, Longitude, Altitude, Timezone, DST) VALUES (?, ?, ?, UPPER(?), UPPER(?), ?, ?, ?, ?, ?)");

            tryClose(updateInDatabaseStatement);
            updateInDatabaseStatement = Database.databaseConnection.prepareStatement("UPDATE Airport SET Name = ?, City = ?, Country = ?, IATA = UPPER(?), ICAO = UPPER(?), Latitude = ?, Longitude = ?, Altitude = ?, Timezone = ?, DST = ? WHERE ID = ?");

            tryClose(getEntityFromIDStatement);
            getEntityFromIDStatement = Database.databaseConnection.prepareStatement("SELECT * FROM Airport WHERE ID = ? LIMIT 1");

            tryClose(getEntityFromCodeStatement);
            getEntityFromCodeStatement = Database.databaseConnection.prepareStatement("SELECT * FROM Airport WHERE ICAO IN (?) OR IATA IN (?) LIMIT 1");

            tryClose(getAllEntitiesStatement);
            getAllEntitiesStatement = Database.databaseConnection.prepareStatement("SELECT * FROM Airport");


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
     * Adds an airline object to the database
     *
     * @param airport the {@link Airport} to add to the database
     * @throws SQLException     Exception if a database error occurs
     * @throws ConstraintsError Exception if a uniqueness constraint in the database is violated
     */
    protected Airport addToDatabase(Airport airport, boolean returnNew) throws SQLException, ConstraintsError {
        if (Database.getCountry(airport.getCountry()) == null) {
            throw new ConstraintsError("Country does not exist in the database");
        }
        addToDatabaseStatement.setString(1, airport.getName());
        addToDatabaseStatement.setString(2, airport.getCity());
        addToDatabaseStatement.setString(3, airport.getCountry());
        addToDatabaseStatement.setString(4, airport.getIata());
        addToDatabaseStatement.setString(5, airport.getIcao());
        addToDatabaseStatement.setDouble(6, airport.getLatitude());
        addToDatabaseStatement.setDouble(7, airport.getLongitude());
        addToDatabaseStatement.setInt(8, airport.getAltitude());
        addToDatabaseStatement.setDouble(9, airport.getTimezone());
        addToDatabaseStatement.setString(10, Character.toString(DSTType.toCode(airport.getDst())));

        try {
            addToDatabaseStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ConstraintsError(Database.generateUniquenessFailedErrorMessage(e));
        }

        try(ResultSet resultSet = addToDatabaseStatement.getGeneratedKeys()) {
            if (resultSet.next()) {
                int id = resultSet.getInt(1);
                if (returnNew) return getEntity(id);
            }
        }

        return null;
    }

    /**
     * Inserts an airport to the database
     *
     * @param airport airport to add to the database
     * @return airport object with the inserted ID
     * @throws SQLException if error from database
     * @throws ConstraintsError if constraints are violated
     */
    protected Airport addToDatabase(Airport airport) throws SQLException, ConstraintsError {
        return addToDatabase(airport, true);
    }

    /**
     * Adds an airport to the `batchAddToDatabaseStatement` statement (but does not execute it)
     * @param airport airport to add to the batch statement
     * @throws SQLException if an SQL error occurs
     */
    public void addToBatch(Airport airport) throws SQLException {
        batchAddToDatabaseStatement.setString(1, airport.getName());
        batchAddToDatabaseStatement.setString(2, airport.getCity());
        if (airport.getCountry() == null) {
            throw new ConstraintsError("Country does not exist or is null");
        }
        batchAddToDatabaseStatement.setString(3, airport.getCountry());
        batchAddToDatabaseStatement.setString(4, airport.getIata());
        batchAddToDatabaseStatement.setString(5, airport.getIcao());
        batchAddToDatabaseStatement.setDouble(6, airport.getLatitude());
        batchAddToDatabaseStatement.setDouble(7, airport.getLongitude());
        batchAddToDatabaseStatement.setInt(8, airport.getAltitude());
        batchAddToDatabaseStatement.setDouble(9, airport.getTimezone());
        batchAddToDatabaseStatement.setString(10, Character.toString(DSTType.toCode(airport.getDst())));
        batchAddToDatabaseStatement.addBatch();
    }


    /**
     * Updates an Airport object in the database
     *
     * @param airport The Airport object
     * @throws SQLException     Exception if a database error occurs
     * @throws ConstraintsError Exception if a uniqueness constraint in the database is violated
     */
    public void updateInDatabase(Airport airport) throws SQLException {
        updateInDatabaseStatement.setString(1, airport.getName());
        updateInDatabaseStatement.setString(2, airport.getCity());
        updateInDatabaseStatement.setString(3, airport.getCountry());
        updateInDatabaseStatement.setString(4, airport.getIata());
        updateInDatabaseStatement.setString(5, airport.getIcao());
        updateInDatabaseStatement.setDouble(6, airport.getLatitude());
        updateInDatabaseStatement.setDouble(7, airport.getLongitude());
        updateInDatabaseStatement.setInt(8, airport.getAltitude());
        updateInDatabaseStatement.setDouble(9, airport.getTimezone());
        updateInDatabaseStatement.setString(10, Character.toString(DSTType.toCode(airport.getDst())));
        updateInDatabaseStatement.setInt(11, airport.getId());

        try {
            updateInDatabaseStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ConstraintsError(Database.generateUniquenessFailedErrorMessage(e));
        }
    }

    /**
     * Generates and Airport Object based on data from an SQL query.
     *
     * @param resultSet {@link ResultSet}
     * @return an airport data object {@link Airport}
     * @throws SQLException If ta fatal error occurs in the database
     */
    private Airport makeAirport(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("ID");
        String name = resultSet.getString("Name");
        String city = resultSet.getString("City");
        String country = resultSet.getString("Country");
        String iata = resultSet.getString("IATA");
        String icao = resultSet.getString("ICAO");
        double latitude = resultSet.getDouble("Latitude");
        double longitude = resultSet.getDouble("Longitude");
        int altitude = resultSet.getInt("Altitude");
        double timezone = resultSet.getDouble("Timezone");
        char dst = resultSet.getString("DST").charAt(0);

        return new Airport(id, name, city, country, iata, icao, latitude, longitude, altitude, timezone, dst);
    }


    /**
     * {@inheritDoc}
     */
    public Airport getEntity(int id) throws SQLException {
        getEntityFromIDStatement.setInt(1, id);

        try(ResultSet resultSet = getEntityFromIDStatement.executeQuery()) {
            if (resultSet.next()) {
                return makeAirport(resultSet);
            }
        }

        return null;
    }

    /**
     * Gets the Airport from the database which has the specified code
     *
     * @param code The code of the airport to get
     * @return The Airport object in the database which has the specified code. Null if no airport with the code is found.
     * @throws SQLException Fatal error in database
     */
    public Airport getEntity(String code) throws SQLException {
        Database.establishConnection();

        getEntityFromCodeStatement.setString(1, code);
        getEntityFromCodeStatement.setString(2, code);

        try(ResultSet resultSet = getEntityFromCodeStatement.executeQuery()) {
            if (resultSet.next()) {
                return makeAirport(resultSet);
            }
        }

        return null;
    }

    /**
     * Attempts to retrieve all airports from the database
     *
     * @return List of all airports objects in the database
     * @throws SQLException Error connecting to database, or some similar unrecoverable error
     */
    public List<Airport> getAllEntities() throws SQLException {
        ArrayList<Airport> airports = new ArrayList<Airport>();

        try(ResultSet resultSet = getAllEntitiesStatement.executeQuery()) {
            while (resultSet.next()) {
                airports.add(makeAirport(resultSet));
            }
        }

        return airports;
    }


    /**
     * @param sortColumn The column to sort by
     * @param order      The order (eg ascending, descending) to sort by
     * @param numRows    Maximum numbers of rows to return
     * @param offset     Offset for the rows being returned (e.g. numRows=50, offset=50 means it gets the 50-99th rows for the given sort order)
     * @return Filtered list of airports
     * @throws SQLException if error from database
     */
    public List<Airport> getSortedFilteredEntities(String sortColumn, SortOrder order, int numRows, int offset) throws SQLException {
        Database.establishConnection();
        String SQLQuery = "SELECT a.*, IFNULL(r.NumRoutes, 0) as Routes, coalesce(IATA, ICAO) AS Code FROM Airport a LEFT JOIN (SELECT Source, COUNT(*) AS NumRoutes FROM Route GROUP BY Source) r ON a.IATA = r.Source OR a.ICAO = r.Source";

        //        Filters.
        FiltersController filters = FiltersController.getSingleton();

        if (filters != null) {
            String country = Database.generateTextualFilterSQLText("Country", filters.getCountryFilter());
            String code = Database.generateTextualFilterSQLText("Code", filters.getAirportCodeFilter());
            String name = Database.generateTextualFilterSQLText("Name", filters.getAirportNameFilter());
            String numFlights = Database.generateFilterRangeSQLTextForInt("Routes", filters.getRouteNumberFilter().getBounds());
            SQLQuery += Database.mergeSQLWhereClauses(country, numFlights, code, name);
        }

        if (sortColumn != null && order != null) {
            if ("Code".equals(sortColumn)) { //Sorts by code prioritising IATA's over ICAO's. This is because IATA's are always shown when available
                SQLQuery += " ORDER BY coalesce(IATA, ICAO) " + order.getSQLCode() + " NULLS LAST";
            } else {
                SQLQuery += " ORDER BY " + sortColumn + " " + order.getSQLCode() + " NULLS LAST";
            }
        }

        SQLQuery += String.format(" LIMIT %d OFFSET %d", numRows, offset);

        if (SQLQuery.contains(";")) {
            // The is an attempt at causing an SQL Injection attack
            return null;
        }
        ArrayList<Airport> airports = new ArrayList<>();

        try(PreparedStatement statement = Database.databaseConnection.prepareStatement(SQLQuery)) {
            try(ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    airports.add(makeAirport(resultSet));
                }
            }
        }

        return airports;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteFromDatabase(int id) throws SQLException {
        super.deleteFromDatabase("Airport", id);
        notifyObserversOfDeletion(id);
    }

    /**
     * Takes an airport code and gets the number of routes for that airport.
     *
     * @param code The code that is stored in the airport object
     * @return Total number of routes from Airport, or 0 if query failed somehow
     * @throws SQLException if error from database
     */
    public int getTotalRoutes(String code) throws SQLException {
        Database.establishConnection();
        String SQLQuery = "SELECT COUNT(*) FROM Route WHERE Source = ?";

        PreparedStatement statement = Database.databaseConnection.prepareStatement(SQLQuery);
        statement.setString(1, code);

        try(ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }
}