package seng202.group8.io;

import org.apache.commons.io.FileUtils;
import org.sqlite.SQLiteException;
import seng202.group8.AlertHelper;
import seng202.group8.data.Country;
import seng202.group8.data.filters.FilterRange;
import seng202.group8.data.filters.TextualFilter;
import seng202.group8.datacontroller.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class responsible for instantiating the connection to the database and for switching databases and all the edge cases it entails
 */
public class Database {

    public static Connection databaseConnection = null;
    public static Pattern uniqueConstraintFailedRegExp = Pattern.compile("UNIQUE constraint failed: \\w+\\.(\\w+)");
    public static HashMap<String, Country> countries; // Hash map with name of country (all lowercase) being the key
    public static final String defaultDatabasePath = "/seng202/group8/defaultDatabase.db";
    public static final String defaultDatabaseName = "database.db";
    protected static URI databasePath;
    protected static URI previousDatabasePath;

    public static final int ERROR_CODE_FATAL_ERROR_ON_DATABASE_LOAD = 4;
    public static final int ERROR_CODE_INFINITE_LOOP_ON_DATABASE_LOAD = 5;


    /**
     * Establishes connection with the database
     */
    public static void establishConnection() {
        establishConnection(0);
    }

    /**
     * Establishes connection with the database
     *
     * @param counter counter to use to prevent infinite loops
     */
    protected static void establishConnection(int counter) {
        if (counter++ > 2) {
            AlertHelper.showGenericErrorAlert(null, false,
                "A fatal database error occurred",
                "The program encountered an infinite loop loading database",
                "The program will now exit. Check if '" + defaultDatabaseName + "' is a valid database file",
                ERROR_CODE_INFINITE_LOOP_ON_DATABASE_LOAD
            );
        }

        if (databaseConnection != null) return; // We are already connected to the database

        try {
            if (databasePath == null) {
                setDatabasePath(counter);
            }

            // Somehow, establishConnection is called multiple times during tests in parallel
            // Takes time to connect the first time, and the second one gets called while establishing the connection
            // Hence, databaseConnection is null so a second connection gets generated, causing a lock
            Connection temporaryConnection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.getPath());
            if (databaseConnection != null) {
                temporaryConnection.close();
                return;
            } else {
                databaseConnection = temporaryConnection;
            }

            if (databaseIsEmpty()) {
                databaseConnection.close(); // Need to close connection to overwrite file
                databaseConnection = null;
                copyDefaultDatabaseToPath(databasePath);
                setDatabasePath(databasePath, counter); // re-establish connection and update DCs
                return; // Don't need to load countries: setDatabasePath calls establishConnection
            }

            loadAllCountries();
        } catch (SQLiteException e) {
            dealWithExceptionOnDatabaseLoad(counter);
        } catch (SQLException | IOException throwables) {
            AlertHelper.showGenericErrorAlert(throwables, true,
                    "Fatal Database Exception",
                    "A fatal database error occurred; the program will close",
                    null,
                    ERROR_CODE_FATAL_ERROR_ON_DATABASE_LOAD
            );
            // If DB doesn't load, we're screwed so just crash and burn
        }
    }


    /**
     * Deals with an exception that occurs on DB load. Will attempt to revert to the previous database, unless that fails several times, then bails.
     * Was built to deal with a `SQLITE_NOTADB` exception but handles every other type of SQLite the same way
     * @param counter   counter to prevent infinite loops. Should be called with 0 by all other methods.
     */
    public static void dealWithExceptionOnDatabaseLoad(int counter) {
        String badPath = databasePath.toString();
        try {
            String errorMessage = String.format("'%s' is not a valid database.", badPath);

            if (previousDatabasePath != null) {
                errorMessage += String.format(" Switching back to '%s'", previousDatabasePath.toString());
            }

            AlertHelper.showGenericErrorAlert(null, false,
                "Bad database selected",
                "The program will revert back to the previous database",
                    errorMessage,
                null
            );

            if (previousDatabasePath != null) {
                setDatabasePath(previousDatabasePath, counter);
            } else {
                setDatabasePath(counter);
            }

        } catch (IOException | SQLException throwables) {
            AlertHelper.showGenericErrorAlert(throwables, true,
                    "Bad database selected",
                    "A fatal error occurred attempting to revert back to the previous database",
                    String.format("'%s' is not a valid database and attempt to switch back to '%s' failed.\n", badPath, databasePath.toString())
                    + AlertHelper.sendReportToDevWithStacktraceString,
                    ERROR_CODE_FATAL_ERROR_ON_DATABASE_LOAD
            );
        }
    }

    /**
     * Sets the database path to the default path and re-establishes a connection with the new database
     * @throws IOException if there's something wrong with the file
     * @throws SQLException if error from the database
     * @throws URISyntaxException syntax exception
     */
    public static void setDatabasePath() throws IOException, SQLException, URISyntaxException {
        setDatabasePath(0);
    }

    /**
     * Sets the database path to the default path and re-establishes a connection with the new database
     *
     * @param counter counter used to prevent infinite loops between `setDatabasePath` and `establishConnection`
     * @throws IOException if there's something wrong with the file
     * @throws SQLException if error from database
     */
    protected static void setDatabasePath(int counter) throws IOException, SQLException {
        setDatabasePath(Paths.get("./", defaultDatabaseName).toUri(), counter);
    }

    /**
     * Sets the database path to the given URI and re-establishes a connection with the new database
     * @param uri the URI
     * @throws IOException if there's something wrong with the file
     * @throws SQLException if error from database
     */
    public static void setDatabasePath(URI uri) throws IOException, SQLException {
        setDatabasePath(uri, 0);
    }

    /**
     * Shows an alert and exits the program if an exception occurs while calling `setDatabasePath`
     * @param throwables exception thrown by `setDatabasePath`
     */
    public static void exitProgramOnSetDatabasePathException(Exception throwables) {
        AlertHelper.showGenericErrorAlert(throwables, true,
                "A fatal error occurred loading the database",
                "A fatal error occurred while loading the database",
                null,
                ERROR_CODE_FATAL_ERROR_ON_DATABASE_LOAD
        );
    }

    /**
     * Sets the database path to the given URI, re-establishes a connection with the new database and sets the prepared statements
     * for the DataControllers
     * @param uri the URI
     * @param counter counter used to prevent infinite loops between `setDatabasePath` and `establishConnection`
     * @throws IOException if given database path is invalid
     * @throws SQLException error from database
     */
    protected static void setDatabasePath(URI uri, int counter) throws IOException, SQLException {
        if (databaseConnection != null) {
            databaseConnection.close();
            databaseConnection = null;
        }
        if (databasePath != null) {
            previousDatabasePath = databasePath;
        }

        databasePath = new File(new File(uri).getCanonicalPath()).toURI();
        establishConnection(counter);

        for (DataController dc : new DataController[]{
                AirlineDataController.getSingleton(),
                AirportDataController.getSingleton(),
                RouteDataController.getSingleton(),
                TripDataController.getSingleton()
        }) {
            dc.onDBChange();
            dc.notifyGlobalObservers(null); // Needed to update filters bounds, so notify all observers
        }
    }

    /**
     * Copies the default database to the specified path
     *
     * @param destinationPath URI denoting the path of the new database
     * @throws IOException if there's something wrong with the file
     */
    protected static void copyDefaultDatabaseToPath(URI destinationPath) throws IOException {
        URL defaultDBPath = Database.class.getResource(defaultDatabasePath);
        File destination = new File(destinationPath);

        FileUtils.copyURLToFile(defaultDBPath, destination);

    }

    /**
     * DB stored in /resources path. Have no idea how Maven packages things and the sqlite JDBC seems only support
     * a read-only database using `getResource`. So, if DB has zero tables (if file doesn't exist, sqlite/JBDC makes
     * an empty DB automatically so there won't be an error from the file not existing),
     * copy the DB in the resources folder to ./
     * @return bool whether the database is empty
     * @throws SQLException if an SQL error occurs
     */
    protected static boolean databaseIsEmpty() throws SQLException {
        try(Statement statement = databaseConnection.createStatement()) {
            try(ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM sqlite_master;")) {
                return (resultSet.next() && resultSet.getInt(1) == 0);
            }
        }

    }

    /**
     * Loads all countries into the `countries` hashmap
     * @throws SQLException if error from database
     */
    protected static void loadAllCountries() throws SQLException {
        countries = new HashMap<>();
        establishConnection();

        try(PreparedStatement statement = databaseConnection.prepareStatement("SELECT * FROM COUNTRY")) {
            try(ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("ID");
                    String iso = resultSet.getString("ISO");
                    String name = resultSet.getString("Name");
                    Country country = new Country(id, name, iso);
                    countries.put(name.toLowerCase(), country);
                }
            }
        }
    }

    /**
     * Gets a country from its name
     *
     * @param countryName name of the country
     * @return associated country object, or null
     */
    public static Country getCountry(String countryName) {
        establishConnection();
        return countries.get(countryName.toLowerCase().trim());
    }

    /**
     * Gets all country names, sorted alphabetically
     *
     * @return list of country names
     */
    public static ArrayList<String> getAllCountryNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Country country : countries.values()) {
            names.add(country.getName());
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Interface for method which converts a value of a given data type into a string suitable to use in a SQL query
     *
     * @param <DataType> data type to convert
     */
    public interface FilterRangeFormatter<DataType> {
        /**
         * Converts a value of the given type into a string that can be used in a SQL query (e.g. Date object to '2001-31-12')
         *
         * @param val value to convert
         * @return string that can be used in an SQL query
         */
        String format(DataType val);
    }

    /**
     * Generates SQL text along the lines of 'columnName @lt; min' or `columnName BETWEEN min AND max`
     *
     * @param propertyName name of the column
     * @param range range object containing min/max values (inclusive), or NULL if there is no lower/upper bound
     * @param formatter method that converts the range value into a string that can be parsed by the database
     * @param <DataType> the type of data for filtering
     * @return SQL text, or NULL if there are no lower and upper bounds
     */
    public static <DataType> String generateFilterRangeSQLText(String propertyName, FilterRange<DataType> range, FilterRangeFormatter<DataType> formatter) {
        boolean noLowerBound = range.min == null;
        boolean noUpperBound = range.max == null;
        if (noLowerBound && noUpperBound) {
            return null;
        } else if (!noLowerBound && !noUpperBound) {
            // Lower and upper bound
            return String.format("%s BETWEEN %s AND %s", propertyName, formatter.format(range.min), formatter.format(range.max));
        } else if (!noLowerBound) {
            // Lower bound, no upper bound
            return String.format("%s >= %s", propertyName, formatter.format(range.min));
        } else {
            // Upper bound, no lower bound
            return String.format("%s <= %s", propertyName, formatter.format(range.max));
        }
    }

    /**
     * Generates SQL text along the lines of 'columnName @lt; min' or `columnName BETWEEN min AND max` for an integer column
     *
     * @param propertyName name of the column
     * @param range        range object containing min/max integer values (inclusive), or NULL if there is no lower/upper bound
     * @return SQL text, or NULL if there are no lower and upper bounds
     */
    public static String generateFilterRangeSQLTextForInt(String propertyName, FilterRange<Integer> range) {
        return generateFilterRangeSQLText(propertyName, range, (val) -> {
            return Integer.toString((Integer) val);
        });
    }

    /**
     * Generates SQL text along the lines of 'columnName @lt; min' or `columnName BETWEEN min AND max` for a double column
     *
     * @param propertyName name of the column
     * @param range        range object containing min/max integer values (inclusive), or NULL if there is no lower/upper bound
     * @return SQL text, or NULL if there are no lower and upper bounds
     */
    public static String generateFilterRangeSQLTextForDouble(String propertyName, FilterRange<Double> range) {
        return generateFilterRangeSQLText(propertyName, range, (val) -> {
            return Double.toString((Double) val);
        });
    }


    /**
     * Generates SQL text along the lines of `columnName IN (val1, val2...)`
     *
     * @param propertyName name of the column
     * @param options      list of options
     * @return SQL text, or NULL if no options are given
     */
    public static String generateTextualFilterSQLText(String propertyName, Collection<String> options) {
        if (options.size() == 0) {
            return null;
        }

        String sql = propertyName + " IN (";

        boolean first = true;
        for (String val : options) {
            sql += (first ? "" : ", ") + "'" + val.replace("'", "''") + "'"; // Wrap in quotes, comma separate
            first = false;
        }

        sql += ")";
        return sql;
    }

    /**
     * Generates SQL text along the lines of `columnName IN (val1, val2...)`
     *
     * @param propertyName name of the column
     * @param options      list of integers
     * @return SQL text, or NULL if no integers are given
     */
    public static String generateIdFilterSQLText(String propertyName, List<Integer> options) {
        if (options.size() == 0) {
            return null;
        }

        String sql = propertyName + " IN (";

        boolean first = true;
        for (Integer val : options) {
            sql += (first ? "" : ", ") + val; // Wrap in quotes, comma separate
            first = false;
        }

        sql += ")";
        return sql;
    }

    /**
     * Generates SQL text along the lines of `columnName IN (val1, val2...)`
     *
     * @param propertyName name of the column
     * @param filter       textual filter containing values that you want
     * @return SQL text, or NULL if the filter is disabled
     */
    public static String generateTextualFilterSQLText(String propertyName, TextualFilter filter) {
        if (filter == null) {
            return null;
        }

        return generateTextualFilterSQLText(propertyName, filter.getSelectedOptions());
    }

    /**
     * Merges where clauses into a single string in form `WHERE propertyName filterDefinition AND propertyName filterDefinition`
     *
     * @param clauses a list of clauses, each being of the form `propertyName filterDefinition`, or null
     * @return merged clauses, with no whitespace at the start or end
     */
    public static String mergeSQLWhereClauses(String... clauses) {
        String result = "";
        boolean first = true;

        for (String clause : clauses) {
            if (clause != null) {
                result += (first ? " WHERE " : " AND ") + clause;
                first = false;
            }
        }
        return result;

    }

    /**
     * Gets the name of property which broke the uniqueness SQL constraint
     *
     * @param error SQLException's error message(getMessage)
     * @return the name of the property, or null if could not find it
     */
    public static String getUniqueConstraintFailedPropertyName(String error) {
        Matcher matcher = uniqueConstraintFailedRegExp.matcher(error);

        if (error.contains("Route.Airline, Route.Source, Route.Destination")) {   // A horrible horrible kludge to make it display the custom constrain. Should never even be using regex for this stuff
            return "airline, source and destination combined";
        } else if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    /**
     * Generates an error message for a SQLException due to a uniqueness constraint being broken
     *
     * @param error SQLException, hopefully due to a broken uniqueness constraint
     * @return raw error message, or specific error message referencing the property that had the broken uniqueness constraint if found
     */
    public static String generateUniquenessFailedErrorMessage(SQLException error) {
        String propertyName = getUniqueConstraintFailedPropertyName(error.getMessage());
        if (propertyName == null) {
            return error.getMessage();
        } else {
            return String.format("The value for the %s must be unique", propertyName);
        }
//        return new DataConstraintsException(propertyName == null? Data.UNKNOWN_COLUMN: propertyName, "Another entry with the same value already exists"); // Want to change it to this since the setters/constructors use this
    }
}
