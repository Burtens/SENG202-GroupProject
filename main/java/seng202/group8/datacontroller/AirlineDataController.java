package seng202.group8.datacontroller;

import seng202.group8.AlertHelper;
import seng202.group8.data.Airline;
import seng202.group8.io.ConstraintsError;
import seng202.group8.io.Database;
import seng202.group8.io.SortOrder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible for interfacing program with database for Airline data
 */
public class AirlineDataController extends DataController<Airline> {
    private static AirlineDataController singleton;
    private PreparedStatement addToDatabaseStatement = null;
    private PreparedStatement batchAddToDatabaseStatement = null;
    private PreparedStatement updateInDatabaseStatement = null;
    private PreparedStatement getEntityByIdStatement = null;
    private PreparedStatement getEntityByNameStatement = null;
    private PreparedStatement getEntityByCodeStatement = null;
    private PreparedStatement getAllEntitiesStatement = null;

    /**
     * Gets the singleton instance for the DataController
     *
     * @return the singleton instance
     */
    public static AirlineDataController getSingleton() {
        if (singleton == null) {
            singleton = new AirlineDataController();
        }

        return singleton;
    }

    /**
     * Initializer for the data controller; protected as it is a singleton
     */
    protected AirlineDataController() {
        super();
        onDBChange();
    }

    /**
     * {@inheritDoc}
     */
    public boolean onDBChange() {
        try {
            Database.establishConnection();

            tryClose(addToDatabaseStatement);
            addToDatabaseStatement = Database.databaseConnection.prepareStatement("INSERT INTO Airline(Name, IATA, ICAO, Callsign, Country) VALUES (?, UPPER(?), UPPER(?), ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);

            tryClose(batchAddToDatabaseStatement);
            batchAddToDatabaseStatement = Database.databaseConnection.prepareStatement("INSERT OR IGNORE INTO Airline(Name, IATA, ICAO, Callsign, Country) VALUES (?, UPPER(?), UPPER(?), ?, ?)");

            tryClose(updateInDatabaseStatement);
            updateInDatabaseStatement = Database.databaseConnection.prepareStatement("UPDATE Airline SET Name = ?, IATA = UPPER(?), ICAO = UPPER(?), Callsign = ?, Country = ? WHERE ID = ?");

            tryClose(getEntityByIdStatement);
            getEntityByIdStatement = Database.databaseConnection.prepareStatement("SELECT * FROM Airline WHERE ID = ?");

            tryClose(getEntityByNameStatement);
            getEntityByNameStatement = Database.databaseConnection.prepareStatement("SELECT * FROM Airline WHERE Name = ? LIMIT 1");

            tryClose(getEntityByCodeStatement);
            getEntityByCodeStatement = Database.databaseConnection.prepareStatement("SELECT * FROM Airline WHERE IATA = ? OR ICAO = ? LIMIT 1");

            tryClose(getAllEntitiesStatement);
           getAllEntitiesStatement = Database.databaseConnection.prepareStatement("SELECT * FROM Airline");

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
     * {@inheritDoc}
     */
    protected Airline addToDatabase(Airline airline, boolean returnNew) throws SQLException, ConstraintsError {
        setStatementValues(addToDatabaseStatement, airline);
        try {
            addToDatabaseStatement.executeUpdate();
        } catch (SQLException e) {
            throw new ConstraintsError(Database.generateUniquenessFailedErrorMessage(e));
        }

        try(ResultSet resultSet = addToDatabaseStatement.getGeneratedKeys()) {
            resultSet.next();
            int id = resultSet.getInt(1);
            if (returnNew) return getEntity(id);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    protected Airline addToDatabase(Airline airline) throws SQLException, ConstraintsError {
        return addToDatabase(airline, true);
    }


    /**
     * Adds the airline to the batch insert statement
     *
     * @param airline airline to add to the batch statement
     * @throws SQLException if an SQL error occurs
     */
    public void addToBatch(Airline airline) throws SQLException {
        setStatementValues(batchAddToDatabaseStatement, airline);
        batchAddToDatabaseStatement.addBatch();
    }

    /**
     * Sets the unbound values in the passed batch statement to the relevant values from the airline object
     *
     * @param batchAddToDatabaseStatement batch statement to bound airline values to
     * @param airline                     airline object to bind values to
     * @throws SQLException if an SQL error occurs
     */
    private void setStatementValues(PreparedStatement batchAddToDatabaseStatement, Airline airline) throws SQLException {
        batchAddToDatabaseStatement.setString(1, airline.getName());
        batchAddToDatabaseStatement.setString(2, airline.getIata());
        batchAddToDatabaseStatement.setString(3, airline.getIcao());
        batchAddToDatabaseStatement.setString(4, airline.getCallsign());
        batchAddToDatabaseStatement.setString(5, airline.getCountry());
    }

    /**
     * Updates an Airline object in the database
     *
     * @param airline The Airline object
     * @throws SQLException     Exception if a database error occurs
     * @throws ConstraintsError Exception if a uniqueness constraint in the database is violated
     */
    protected void updateInDatabase(Airline airline) throws SQLException, ConstraintsError {
        updateInDatabaseStatement.setString(1, airline.getName());
        updateInDatabaseStatement.setString(2, airline.getIata());
        updateInDatabaseStatement.setString(3, airline.getIcao());
        updateInDatabaseStatement.setString(4, airline.getCallsign());
        updateInDatabaseStatement.setString(5, airline.getCountry());
        updateInDatabaseStatement.setInt(6, airline.getId());

        try {
            updateInDatabaseStatement.executeUpdate();
        } catch (SQLException e) {
            // Something went wrong, probably failed uniqueness constraint, i.e. value already in database
            throw new ConstraintsError(Database.generateUniquenessFailedErrorMessage(e));
        }
    }

    /**
     * Given a result set from a SQL `SELECT * FROM Airline ...` query, generates a single Airline object.
     * It does not move the pointer of the result set
     *
     * @param resultSet result set from the select statement at the right pointer
     * @return new Airline object generated from the result set
     * @throws SQLException If ta fatal error occurs in the database
     */
    private Airline makeAirline(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt(Airline.ID);
        String name = resultSet.getString(Airline.NAME);
        String iata = resultSet.getString(Airline.IATA);
        String icao = resultSet.getString(Airline.ICAO);
        String callsign = resultSet.getString(Airline.CALLSIGN);
        String country = resultSet.getString(Airline.COUNTRY);
        return new Airline(id, name, callsign, iata, icao, country);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Airline getEntity(int id) throws SQLException {
        getEntityByIdStatement.setInt(1, id);

        try(ResultSet resultSet = getEntityByIdStatement.executeQuery()) {
            if (resultSet.next()) {
                return makeAirline(resultSet);
            }
        }

        return null;
    }

    /**
     * Gets an airline object from a provided name
     *
     * @param name The name of the airline
     * @return an Airline object corresponding to the provide name, null if name is not in database
     * @throws SQLException Exception for if something goes wrong in the database
     */
    public Airline getEntityByName(String name) throws SQLException {
        getEntityByNameStatement.setString(1, name);

        try(ResultSet resultSet = getEntityByNameStatement.executeQuery()) {
            if (resultSet.next()) {
                return makeAirline(resultSet);
            }
        }

        return null;
    }

    /**
     * Gets an airline object from a provided airline ICAO or IATA code
     *
     * @param code The IATA or ICAO code for the airline
     * @return an Airline object corresponding to the provide name, or null if not found
     * @throws SQLException Exception for if something goes wrong in the database
     */
    public Airline getEntity(String code) throws SQLException {
        onDBChange();
        getEntityByCodeStatement.setString(1, code.toUpperCase());
        getEntityByCodeStatement.setString(2, code.toUpperCase());

        try(ResultSet resultSet = getEntityByCodeStatement.executeQuery()) {
            if (resultSet.next()) {
                return makeAirline(resultSet);
            }
        }

        return null;
    }


    /**
     * Attempts to retrieve all airlines from the database
     *
     * @return List of all airline objects in the database
     * @throws SQLException Error connecting to database, or some similar unrecoverable error
     */
    public List<Airline> getAllEntities() throws SQLException {
        ArrayList<Airline> airlines = new ArrayList<Airline>();

        try(ResultSet resultSet = getAllEntitiesStatement.executeQuery()) {
            while (resultSet.next()) {
                airlines.add(makeAirline(resultSet));
            }
        }

        return airlines;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<Airline> getSortedFilteredEntities(String sortColumn, SortOrder order, int numRows, int offset) throws SQLException {
        Database.establishConnection();
        String SQLQuery = "SELECT *, coalesce(IATA, ICAO) AS Code FROM Airline";

//        Filters.
        FiltersController filters = FiltersController.getSingleton();


        if (filters != null) {
            String country = Database.generateTextualFilterSQLText("Country", filters.getCountryFilter());
            String name = Database.generateTextualFilterSQLText("Name", filters.getAirlineNameFilter());
            String code = Database.generateTextualFilterSQLText("Code", filters.getAirlineCodeFilter());
            SQLQuery += Database.mergeSQLWhereClauses(country, name, code);
        }

        if (sortColumn != null && order != null) {
            if (sortColumn.equals("Code")) { //Sorts by code prioritising IATA's over ICAO's. This is because IATA's are always shown when available
                SQLQuery += " ORDER BY coalesce(IATA, ICAO) " + order.getSQLCode() + " NULLS LAST";
            } else {
                SQLQuery += " ORDER BY " + sortColumn + " " + order.getSQLCode() + " NULLS LAST";
            }
        }

        SQLQuery += String.format(" LIMIT %d OFFSET %d", numRows, offset);

        if (SQLQuery.contains(";")) {
            return null;
        }

        ArrayList<Airline> airlines = new ArrayList<>();

        try(PreparedStatement statement = Database.databaseConnection.prepareStatement(SQLQuery)) {
            try(ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    airlines.add(makeAirline(resultSet));
                }
            }
        }

        return airlines;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteFromDatabase(int id) throws SQLException {
        super.deleteFromDatabase("Airline", id);
        notifyObserversOfDeletion(id);
    }
}
