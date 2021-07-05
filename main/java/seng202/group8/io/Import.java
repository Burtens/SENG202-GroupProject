package seng202.group8.io;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import seng202.group8.AlertHelper;
import seng202.group8.data.*;
import seng202.group8.datacontroller.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Reads csv or mtyg files to put trip, route, airline or airports to the database
 */
public class Import {

    protected static AirlineDataController airlineDC = AirlineDataController.getSingleton();
    protected static AirportDataController airportDC = AirportDataController.getSingleton();
    protected static RouteDataController routeDC = RouteDataController.getSingleton();

    protected static String csvRowSplitRegExp = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";

    /**
     * Charset that is being used for read/write operations (UTF-8)
     */
    public static final Charset FILE_ENCODING = StandardCharsets.UTF_8;

    /**
     * Maximum size of a batch during import
     */
    public static final int BATCH_SIZE = 500;

    /**
     * Imports trip from a CSV file in chosen file location
     * Top is name and comment, subsequent lines are TripFlights
     *
     * @param filePath to csv to open trip
     * @return Trip object generated. Null if there is any sort of error
     * @throws IOException something wrong with the file
     */
    public static Trip importTrip(String filePath) throws IOException {
        // line 1: name, comment
        // All others: TripFlights - source code, destination code, airline code, takeoffDateTime, comment
        File csvFile = new File(filePath);

        try(FileReader filereader = new FileReader(csvFile, FILE_ENCODING)) {
            // The csv reader must use the rfc4180 to work nicely with the trip csv writer, otherwise backslashes will cause issues
            // See https://dzone.com/articles/properly-handling-backslashes-using-opencsv for more info
            RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
            CSVReader csvReader = new CSVReaderBuilder(filereader).withCSVParser(rfc4180Parser).build();
            try {
                List<TripFlight> flights = new ArrayList<TripFlight>();
                String[] record = csvReader.readNext();
                String name = trim(record[0]);
                String comment = null;
                if (record.length > 1) {
                    comment = trim(record[1]);
                }

                while ((record = csvReader.readNext()) != null) {
                    try {
                        for (int i = 0; i < record.length; i++) {   // Trim all attributes in the record, just in case
                            record[i] = trim(record[i]);
                        }
                        TripFlight tripFlight = importTripFlight(record);
                        if (tripFlight == null) {  // Invalid route means file has been tampered with, abort import
                            csvReader.close();
                            return null;
                        }
                        flights.add(tripFlight);
                    } catch (ConstraintsError e) {
                        // If any issue occurs, the import is aborted
                        return null;
                    }
                }
                Trip trip = new Trip(name, comment);
                trip.addFlights(flights);
                csvReader.close();
                return trip;
            } catch (DataConstraintsException | CsvValidationException e) {
                return null;
            }
        }
    }

    /**
     * Creates TripFlight object from line of data to return for putting in Trip
     *
     * @param data to be made into TripFlight object
     * @return tripFlight to put into Trip or null if something went wrong
     */
    public static TripFlight importTripFlight(String[] data) {
        // source code, destination code, airline code, takeoffTime, takeoffDate, comment
        try {
            return new TripFlight(data[1], data[2], data[0], Integer.parseInt(data[3]), LocalDate.parse(data[4]), (data.length == 6) ? data[5] : null);
        } catch (DataConstraintsException | IndexOutOfBoundsException | NumberFormatException | DateTimeParseException e) {
            //e.printStackTrace();
            return null;
        }
    }


    /**
     * Inserts given error into the errors hashmap
     *
     * @param errors    hashmap of errors, the key being the error message and value being a list of row numbers where the error occurred
     * @param message   error message
     * @param rowNumber row number where the error occurred
     */
    protected static void insertError(HashMap<String, ArrayList<Integer>> errors, String message, int rowNumber) {
        if (!errors.containsKey(message)) {
            errors.put(message, new ArrayList<>());
        }
        errors.get(message).add(rowNumber);
    }


    /**
     * Method that adds errors caught by the DB (e.g. uniqueness constraint broken) that lead to insert failing
     *
     * @param errors             hashmap of errors, with the key being the error message and value being a list of line numbers where the error occurred
     * @param rowNumbers         rows from the CSV that are part of the current batch, in order
     * @param batchExecuteResult result from batch execute: number of rows updated
     */
    private static void onBatchExecute(HashMap<String, ArrayList<Integer>> errors, ArrayList<Integer> rowNumbers, int[] batchExecuteResult) {
        String message = "Could not insert into database; a row with the same identifiers may exist in the database";
        for (int i = 0; i < batchExecuteResult.length; i++) {
            if (batchExecuteResult[i] == 0) {
                // 0 rows updated means error occurred
                insertError(errors, message, rowNumbers.get(i));
            }
        }
    }

    /**
     * Generates error message string given hashmap of errors
     *
     * @param errors hashmap with key being the error message and value being a list of rows they occurred on
     * @return null if no errors, or a string
     */
    protected static String generateErrorMessageString(HashMap<String, ArrayList<Integer>> errors) {
        if (errors.size() == 0) {
            return null;
        }
        StringBuilder message = new StringBuilder();

        message.append("The following error");
        if (errors.size() != 1) message.append("s");
        message.append(" occurred on these rows:\n\n");

        // Convert hashmap to list to allow sorting to occur
        ArrayList<Pair<String, ArrayList<Integer>>> errorsSorted = new ArrayList<>(errors.size());
        errors.forEach((msg, rows) -> {
            errorsSorted.add(new Pair<>(msg, rows));
        });

        // Sort by number of errors, ascending, and then by name
        Collections.sort(errorsSorted,
                Comparator.comparingInt((Pair<String, ArrayList<Integer>> pair) -> pair.getValue1().size()).
                        thenComparing(Pair::getValue0));

        for (Pair<String, ArrayList<Integer>> pair : errorsSorted) {
            message.append(pair.getValue0());
            message.append(": row");
            if (pair.getValue1().size() != 1) message.append('s');
            message.append(" ");

            boolean isFirst = true;
            for (int lineNum : pair.getValue1()) {
                if (!isFirst) {
                    message.append(", ");
                }
                isFirst = false;
                message.append(lineNum);
            }
            message.append('\n');
        }
        return message.toString();
    }

    /**
     * Imports data from a CSV file in chosen file location to database
     *
     * @param filePath path to file to open
     * @param fileType "Airport" "Airline" or "Route"
     * @param progress the progress property, used to display the loading bar in the GUI
     * @return report on the importing success: number of rows, number of failures, duration in milliseconds, error message (or null);
     */
    public static Quartet<Integer, Integer, Long, String> importData(String filePath, String fileType, DoubleProperty progress) {
        long startTime = System.currentTimeMillis();

        HashMap<String, ArrayList<Integer>> errors = new HashMap<>(); // Multiple rows may have the same error message so to reduce the amount of text in the error message shown to the user, store it in the hash map where the key is the error message and the value is a list of row numbers where the error occurred
        ArrayList<Integer> batchLineNumbers = new ArrayList<>(BATCH_SIZE); // Row numbers for rows that were added to the batch: execute batch returns an integer for each 'operation' (every time add to batch is called) denoting the number of rows modified: if it is zero, this means it failed. This array allows us to map a row number to the number of rows modified, and thus detect which rows have failed due to uniqueness constraints etc.

        DataController<?> dc = chooseDataController(fileType);
        int rowNumber = 0;
        CSVReader csvReader = null;
        File csvFile = new File(filePath);
        try(FileReader filereader = new FileReader(csvFile, FILE_ENCODING)) {

            int numberOfRows = numberOfRows(csvFile);

            // The csv reader must use the rfc4180, otherwise backslashes will cause issues
            // See https://dzone.com/articles/properly-handling-backslashes-using-opencsv for more info
            RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
            csvReader = new CSVReaderBuilder(filereader).withCSVParser(rfc4180Parser).build();

            rowNumber = readLines(csvReader, fileType, batchLineNumbers, errors, dc, progress, numberOfRows);// Loop through the file

            // Commit the files in the last batch
            if (batchLineNumbers.size() != 0) {
                onBatchExecute(errors, batchLineNumbers, dc.executeBatch());
            }

            submitRoutes(fileType); // Commit the saves done to database if type was Route

            if (progress != null)
                if (fileType.equals("Airline"))
                    progress.set(1);
                else
                    progress.set(0.5);

            csvReader.close();
        } catch (IOException | CsvValidationException e) {
            if (csvReader != null) {
                try {
                    csvReader.close();
                } catch (IOException ioException) {
                    AlertHelper.showGenericErrorAlert(ioException, true, 
                        "Error importing file", 
                        "File could not be closed", 
                        AlertHelper.sendReportToDevWithStacktraceString, 
                        null
                    );
                }
            }
        }

        if (fileType.equals("Airport") || fileType.equals("Route")) {
            try {
                routeDC.autoGenerateValuesForAllRoutesWithPriceZero(progress);
            } catch (SQLException exception) {
                AlertHelper.showGenericErrorAlert(exception, true,
                        fileType + " Import Issue",
                        fileType + "s imported successfully but route filters may be inaccurate",
                        "Price, duration and takeoff times are automatically generated for routes which have a price of zero. The algorithm uses the distance between the source and destination airport to do so. This process failed and so the filters for price and duration may have unexpected results. To force this process to run, import the file again. If this process fails repeatedly, contact the developers with a copy of your database and the following stack trace:",
                        null
                );
            }
        }

        DataController<?> finalDc = dc;
        if (progress != null) {
            Platform.runLater(() -> {
                // Remember to notify the observers observing this data type that data might have been added by the import
                finalDc.notifyGlobalObservers(null); // DO NOT DELETE! PLEASE! THIS IS ACTUALLY IMPORTANT, YOU CAN'T JUST GET RID OF IT WHEN YOU FEEL LIKE IT
            });
        } else {
            finalDc.notifyGlobalObservers(null); // DO NOT DELETE! PLEASE! THIS IS ACTUALLY IMPORTANT, YOU CAN'T JUST GET RID OF IT WHEN YOU FEEL LIKE IT
        }

        HashSet<Integer> numErrors = new HashSet<>(); // Line numbers where errors occurred. Need hashset as a single row can have multiple errors e.g. DataConstraintsExcption
        errors.forEach((msg, rows) -> {
            numErrors.addAll(rows);
        });

        long endTime = System.currentTimeMillis();
        String errorMessage = generateErrorMessageString(errors);

        return Quartet.with(rowNumber, numErrors.size(), endTime - startTime, errorMessage);
    }

    /**
     * Imports data from a CSV file in chosen file location to database
     *
     * @param filePath path to file to open
     * @param fileType "Airport" "Airline" or "Route"
     * @return report on the importing success: number of rows, number of failures, duration in milliseconds, error message (or null);
     */
    public static Quartet<Integer, Integer, Long, String> importData(String filePath, String fileType) {
        return importData(filePath, fileType, null);
    }


    /**
     * Finds the number of rows in a file, as parsed by RFC4180 CSVReader
     * @param file file to check
     * @return number of rows
     */
    private static int numberOfRows(File file) {
        int rows = 0;
        try(FileReader filereader = new FileReader(file, FILE_ENCODING)) {
            RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
            CSVReader csvReader = new CSVReaderBuilder(filereader).withCSVParser(rfc4180Parser).build();
            while (csvReader.readNext() != null) {
                rows++;
            }
        } catch (IOException | CsvValidationException e) {
            return 0;
        }
        return rows;
    }

    /**
     * Reads through the file
     *
     * @param csvReader        reader to read csv
     * @param fileType         data type of the file - one of  "Airport", "Airline" or "Route"
     * @param batchLineNumbers list of lines to be submitted as batch
     * @param errors           hash map of error message to line error is associated with
     * @param dc               data controller for correct type
     * @param progress         a rough progress counter that is updated as the import process runs
     * @param numberOfRows     number of rows in the CSV
     * @return last row number
     * @throws IOException            if something went badly wrong while reading the file
     * @throws CsvValidationException Not passed valid csv
     */
    protected static int readLines(CSVReader csvReader, String fileType, ArrayList<Integer> batchLineNumbers,
                                   HashMap<String, ArrayList<Integer>> errors, DataController<?> dc, DoubleProperty progress, int numberOfRows)
            throws IOException, CsvValidationException {
        
        // Must be immutable as passing by reference is not allowed in java
        int[] batchNumber = {0};
        int rowNumber = 0;
        String[] record;
        while ((record = csvReader.readNext()) != null) {
            try {
                rowNumber++; // First row is row 1, so increment at the top of the loop
                sortLineType(fileType, batchLineNumbers, rowNumber, record, progress, numberOfRows);
                toSubmit(batchLineNumbers, errors, dc, progress, batchNumber, numberOfRows, fileType);

            } catch (SQLException | ConstraintsError e) { // Many catches to give more specific error messages
                // DB constraint broken
                insertError(errors, e.getMessage(), rowNumber);
            } catch (DataConstraintsException e) {
                // Invalid value caught by setters
                int finalLineNumber = rowNumber; // Something to do with lambdas
                e.errors.forEach((column, message) -> {
                    insertError(errors, String.format("Column '%s' (%s)", column, message), finalLineNumber);
                });
            } catch (NumberFormatException e) {
                // Invalid value for number field
                insertError(errors, "Could not parse number", rowNumber);
            } catch (IndexOutOfBoundsException e) {
                // Not enough rows
                insertError(errors, "Row has too few columns", rowNumber);
            }
        }
        return rowNumber;
    }

    /**
     * Select the data controller associated with the file type
     *
     * @param fileType Type of file
     * @return data controller of appropriate type
     */
    protected static DataController<?> chooseDataController(String fileType) {
        DataController<?> dc = null;

        switch (fileType) {
            case "Airline":
                dc = airlineDC;
                break;
            case "Airport":
                dc = airportDC;
                break;
            case "Route":
                dc = routeDC;
                try {
                    Database.databaseConnection.setAutoCommit(false);
                    // Due to takeoff times, `save` must be used instead of batch and thus, need to disable autocommit for performance
                } catch (SQLException throwables) {
                    AlertHelper.showErrorAlert(throwables, "Error occurred while preparing database for route import");
                }
                break;
        }
        return dc;
    }

    /**
     * Sorts line into correct import method
     *
     * @param fileType         type of data to import
     * @param batchLineNumbers record of what's been imported and to be committed to database
     * @param rowNumber        row number to place in batch
     * @param record           line of data to pass to import method
     * @param progress         a rough progress counter that is updated as the import process runs
     * @param numberOfRows     number of rows in the CSV
     * @throws SQLException             if error with database
     * @throws DataConstraintsException if data of unexpected format or violates a constraint
     */
    protected static void sortLineType(String fileType, ArrayList<Integer> batchLineNumbers, int rowNumber, String[] record, DoubleProperty progress, int numberOfRows) throws SQLException, DataConstraintsException {
        switch (fileType) {
            case "Airline":
                Airline airline = importAirline(record);
                airlineDC.addToBatch(airline);
                batchLineNumbers.add(rowNumber);
                break;
            case "Airport":
                Airport airport = importAirport(record);
                airportDC.addToBatch(airport);
                batchLineNumbers.add(rowNumber);
                break;
            case "Route":
                Route route = importRoute(record);
                routeDC.save(route, false);
                if (rowNumber % 500 == 0 && progress != null)
                    progress.set(rowNumber / (double) numberOfRows / 2);
                break;
        }
    }

    /**
     * Checks if batch ready to be submitted to database
     *
     * @param batchLineNumbers Array of lines ready to be submitted
     * @param errors           HashMap of errors associated with lines
     * @param dc               data controller for submitting files
     * @param progress         a rough progress counter that is updated as the import process runs
     * @param batchNumber      the number of batches that have run so far - should initially be zero
     * @param numberOfRows     number of rows in the CSV
     * @param fileType         type of file being imported - one of "Airport", "Airline" or "Route"
     */
    protected static void toSubmit(ArrayList<Integer> batchLineNumbers, HashMap<String, ArrayList<Integer>> errors, DataController<?> dc, DoubleProperty progress, int[] batchNumber, int numberOfRows, String fileType) {
        if (batchLineNumbers.size() == BATCH_SIZE) {
            // Once batch reaches certain size, execute it
            onBatchExecute(errors, batchLineNumbers, dc.executeBatch());
            batchLineNumbers.clear();
            if (progress != null)
                // If we are importing airports we still need to generate route data, so just importing is only half the loading bar
                progress.set(((BATCH_SIZE * ++batchNumber[0]) / (double) numberOfRows) * (fileType.equals("Airline") ? 1 : 0.5));
        }
    }

    /**
     * Routes aren't done by batch, so need to commit the mini saves done throughout
     *
     * @param fileType to check if the file is of Route type
     */
    protected static void submitRoutes(String fileType) {
        if (fileType.equals("Route")) {
            try {
                Database.databaseConnection.commit();
                Database.databaseConnection.setAutoCommit(true);
            } catch (SQLException throwables) {
                AlertHelper.showErrorAlert(throwables);
            }
        }
    }

    /**
     * Creates Airline object from an array of data
     *
     * @param data (1 row from csv, Airline specified as type)
     * @return Airline data object to put into database
     * @throws DataConstraintsException if the country does not exist, or one of the given values are invalid
     * @throws IndexOutOfBoundsException if there are not enough elements in the array
     */
    protected static Airline importAirline(String[] data) throws DataConstraintsException, IndexOutOfBoundsException {
        // ID(0), name(1), Alias(2), IATA(3), ICAO(4), Callsign(5), Country(6), Active(7)
        if (data[6] == null || Database.getCountry(data[6]) == null)
            throw new DataConstraintsException(Airport.COUNTRY, "Country does not exist in the database");

        return new Airline(data[1], data[5], data[3], data[4], data[6]);
    }

    /**
     * Creates Airport object from an array of data
     *
     * @param data (1 row from csv, Airport specified as type)
     * @return Airport data object to put in database
     * @throws DataConstraintsException if the country does not exist, or one of the given values are invalid
     * @throws IndexOutOfBoundsException if there are not enough elements in the array
     * @throws NumberFormatException if a field that should be a double or integer cannot be parsed as such
     */
    protected static Airport importAirport(String[] data) throws DataConstraintsException, IndexOutOfBoundsException, NumberFormatException {
        // ID(0), Name(1), City(2), Country(3), IATA(4), ICAO(5), Lat(6), Long(7), Alt(8),
        // Timezone (hours from UTC)(9), Daylight Savings Time(10), Timezone code(11)

        if (data[3] == null || Database.getCountry(data[3]) == null) {
            throw new DataConstraintsException(Airport.COUNTRY, "Country does not exist in the database");
        }
        return new Airport(data[1], data[2], data[3], data[4], data[5],
                Double.parseDouble(data[6]), Double.parseDouble(data[7]), Integer.parseInt(data[8]),
                Double.parseDouble(data[9]), data[10].charAt(0));

    }

    /**
     * Creates Route object from an array of data
     *
     * @param data (1 row from csv, Route specified as type)
     * @return Route data object to put in database
     * @throws SQLException if there is a database error while attempting to retrieve the source/destination airport
     * @throws DataConstraintsException if the country does not exist, or one of the given values are invalid
     * @throws IndexOutOfBoundsException if there are not enough elements in the array
     * @throws NumberFormatException if a field that should be a double or integer cannot be parsed as such
     */
    protected static Route importRoute(String[] data) throws SQLException, DataConstraintsException, IndexOutOfBoundsException, NumberFormatException {
        // AirlineCode(0), Airline ID(1), Source Airport(2), Source Airport ID(3), Destination Airport(4)
        // Destination Airport ID(5), Codeshare(6), Stops(7), Equipment(8)
        // Optional: Price(9), Takeoff Times(10)
        int price = 0;
        List<Integer> takeoffTimes = new ArrayList<Integer>(); // Initialise takeoffTimes list for Route to generate

        boolean isCodeShare = false;
        if (data[6] != null && data[6].toLowerCase().equals("y")) {
            isCodeShare = true;
        }

        String[] equipment = (data.length == 8) ? null : data[8].split(" ");
        return new Route(data[0], data[2], data[4], equipment, price, isCodeShare, 0, takeoffTimes);
    }

    /**
     * Removes " " or ' ' from edge of strings
     *
     * @param val string to have quotes stripped
     * @return stripped val
     */
    protected static String trim(String val) {
        val = val.trim();
        if (val.length() > 2) {
            // If start character == end character and that character is " or ',
            // strip the first and last character
            if (val.charAt(0) == val.charAt(val.length() - 1) && val.charAt(0) == '\"'
                    || val.charAt(0) == '\'') {
                return val.substring(1, val.length() - 1);
            }
        }
        return val;
    }
}

