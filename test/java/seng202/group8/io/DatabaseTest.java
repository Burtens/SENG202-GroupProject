package seng202.group8.io;

import org.junit.*;
import seng202.group8.AlertHelper;
import seng202.group8.data.Airport;
import seng202.group8.data.Country;
import seng202.group8.data.filters.FilterRange;
import seng202.group8.data.filters.TextualFilter;
import seng202.group8.datacontroller.AirportDataController;
import seng202.group8.datacontroller.DataConstraintsException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class DatabaseTest {
    public static String originalDBBackupFilename = "database.db.BACKUP";
    public Path originalDBPath;
    public Path renamedDBPath;
    public Path testDBPath;
    public Path testDBPath2;
    public boolean currentTestFailed = true;

    AirportDataController airportDC;
    Airport airport;

    @BeforeClass
    public static void setupClass() throws SQLException, IOException, URISyntaxException {
        AlertHelper.isTesting = true;

        Database.setDatabasePath();
        Path backup = Paths.get("./", originalDBBackupFilename);
        Path original = Paths.get("./", Database.defaultDatabaseName);

        Files.copy(original, backup, StandardCopyOption.REPLACE_EXISTING);

    }

    public static void restoreBackup(boolean deleteBackup) throws IOException {
        Path backup = Paths.get("./", originalDBBackupFilename);
        Path original = Paths.get("./", Database.defaultDatabaseName);
        if (deleteBackup) {
            Files.move(backup, original, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.copy(backup, original, StandardCopyOption.REPLACE_EXISTING);
        }

    }

    @AfterClass
    public static void teardownClass() throws SQLException, IOException, URISyntaxException {
        tryClose();
        restoreBackup(true);

        Database.setDatabasePath();
        Database.establishConnection(); // DB can be closed which messes up tests for other classes
    }

    public static ArrayList<String> genArrList(String... args) {
        ArrayList<String> list = new ArrayList<>();
        for (String arg : args) {
            list.add(arg);
        }
        return list;
    }

    public static void tryClose() {
        if (Database.databaseConnection != null) {
            try {
                Database.databaseConnection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    @Before
    public void setup() throws DataConstraintsException {
        currentTestFailed = true;
        originalDBPath = Paths.get("./", Database.defaultDatabaseName);
        renamedDBPath = Paths.get("./", "database.db_RENAME_BACK_IF_DATABASE_TESTS_FAIL.db");
        testDBPath = Path.of("./", "TEST_DB_DELETE_IF_FOUND");
        testDBPath2 = Path.of("./", "TEST_DB2_DELETE_IF_FOUND");

        airportDC = AirportDataController.getSingleton();
        airport = new Airport("Matty G Airport", "Christchurch", "New Zealand", "999", "9999", 10, 10, 0, 0, 'Z');
    }

    @After
    public void teardown() throws SQLException, IOException, URISyntaxException {
        if (currentTestFailed) {
            System.out.println("DB TEST FAILED. RESTORING BACKUP");
            tryClose();
            for (Path path : new Path[]{renamedDBPath, testDBPath, testDBPath2}) {
                new File(path.toUri()).delete();
            }

            try {
                restoreBackup(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Database.previousDatabasePath = null;
        Database.databasePath = null;
    }

    @Test
    public void getCountry() throws SQLException {
        Country country = Database.getCountry("New Zealand");
        assertEquals("NZ", country.getISO());
        assertEquals(164, country.getId());

        currentTestFailed = false;
    }

    @Test
    public void getAllCountryNames() throws SQLException {
        ArrayList<String> countries = Database.getAllCountryNames();

        assertEquals("Afghanistan", countries.get(0));
        assertEquals("Australia", countries.get(11));
        assertEquals("Georgia", countries.get(80));
        assertEquals("New Zealand", countries.get(154));
        assertEquals("United States", countries.get(228));
        assertEquals("Zimbabwe", countries.get(239));

        currentTestFailed = false;
    }

    @Test
    public void testGenerateWhereClauses() {
        // Tests textual and range filters text generator, and function that combines them
        TextualFilter filterA = new TextualFilter("Blah", genArrList());
        filterA.setSelectedOptions(genArrList());
        TextualFilter filterB = new TextualFilter("Blah", genArrList("B-1", "B-2", "B-3"));
        filterB.setSelectedOptions(genArrList("B-1", "B-2"));

        assertEquals(
                " WHERE Text-B IN ('B-1', 'B-2') AND Int-B <= 100 AND Int-C >= 1 AND Double-A BETWEEN 3.14159265358979 AND 123.45",
                Database.mergeSQLWhereClauses(
                        Database.generateTextualFilterSQLText("Text-A", filterA),
                        Database.generateTextualFilterSQLText("Text-B", filterB),
                        Database.generateFilterRangeSQLTextForInt("Int-A", new FilterRange<Integer>(null, null)),
                        Database.generateFilterRangeSQLTextForInt("Int-B", new FilterRange<Integer>(null, 100)),
                        Database.generateFilterRangeSQLTextForInt("Int-C", new FilterRange<Integer>(1, null)),
                        Database.generateFilterRangeSQLTextForInt("Int-D", new FilterRange<Integer>(null, null)),
                        Database.generateFilterRangeSQLTextForDouble("Double-A", new FilterRange<Double>(3.14159265358979, 123.45))
                )
        );

        // Both sub constraints return null, so should get empty string
        assertEquals("", Database.mergeSQLWhereClauses(
                Database.generateTextualFilterSQLText("Text-A", filterA),
                Database.generateFilterRangeSQLTextForInt("Int-A", new FilterRange<Integer>(null, null))
        ));

        assertEquals("", Database.mergeSQLWhereClauses());

        currentTestFailed = false;
    }

    public void renameExistingDB() {
        tryClose();
        new File(renamedDBPath.toUri()).delete();
        new File(originalDBPath.toUri()).renameTo(new File(renamedDBPath.toUri()));
    }

    public void unRenameDB() {
        tryClose();
        new File(originalDBPath.toUri()).delete();
        new File(renamedDBPath.toUri()).renameTo(new File(originalDBPath.toUri()));
    }

    @Test
    public void testNoDefaultDatabase() throws SQLException, IOException, URISyntaxException {
        renameExistingDB();
        Database.setDatabasePath(); // Set to default; should create blank DB
        airportDC.save(airport);
        assertNotNull(airportDC.getEntity(airport.getCode()));

        unRenameDB();
        Database.setDatabasePath();
        assertNull(airportDC.getEntity(airport.getCode())); // Going back to original DB, it should not have it

        currentTestFailed = false;
    }


    public String cannonicalPath() throws IOException {
        return new File(originalDBPath.toUri()).getCanonicalPath();
    }

    @Test
    public void testNotADatabaseDefault() throws IOException, SQLException, URISyntaxException {
        renameExistingDB();
        File dbFile = new File(originalDBPath.toUri());
        dbFile.createNewFile();
        PrintWriter writer = new PrintWriter(dbFile, StandardCharsets.UTF_8);
        writer.println("Help I'm trapped in a driver's license factory");
        writer.close();

        long size = dbFile.length();

        assertThrows(AlertHelper.SystemExitTestingWrapper.class, () -> Database.setDatabasePath());
        assertEquals(size, dbFile.length());
        unRenameDB();

        currentTestFailed = false;
    }

    @Test
    public void testNotADatabaseNotDefault() throws IOException, SQLException, URISyntaxException {
        File testDBFile = new File(testDBPath.toUri());
        testDBFile.createNewFile();
        PrintWriter writer = new PrintWriter(testDBFile, StandardCharsets.UTF_8);
        writer.println("Sudo make me a sandwich");
        writer.close();

        long size = testDBFile.length();

        Database.setDatabasePath(testDBPath.toUri());
        assertEquals(size, testDBFile.length()); // Should not touch the file
        new File(testDBPath.toUri()).delete(); // Should be able to delete file as it is not being used
        assertEquals(cannonicalPath(), cannonicalPath());

        tryClose();

        currentTestFailed = false;
    }

    @Test
    public void testNotADatabaseNotDefaultPreviousSet() throws IOException, SQLException, URISyntaxException {
        Database.setDatabasePath(testDBPath2.toUri());

        File testDBFile = new File(testDBPath.toUri());
        testDBFile.createNewFile();
        PrintWriter writer = new PrintWriter(testDBFile, StandardCharsets.UTF_8);
        writer.println("Sudo make me a sandwich");
        writer.close();

        long size = testDBFile.length();

        Database.setDatabasePath(testDBPath.toUri());
        assertEquals(size, testDBFile.length()); // Should not touch the file
        new File(testDBPath.toUri()).delete(); // Should be able to delete file as it is not being used
        assertEquals(cannonicalPath(), cannonicalPath());

        tryClose();

        new File(testDBPath2.toUri()).delete();

        currentTestFailed = false;
    }

    @Test
    public void testCreateDBNotDefaultPath() throws IOException, SQLException, URISyntaxException {
        Database.setDatabasePath(testDBPath.toUri());

        airportDC.save(airport);
        assertNotNull(airportDC.getEntity(airport.getCode()));

        Database.setDatabasePath();
        assertNull(airportDC.getEntity(airport.getCode())); // Going back to original DB, it should not have it

        new File(testDBPath.toUri()).delete();


        currentTestFailed = false;
    }

    @Test
    public void testGenerateIdFilterSQLTextNoOptions() {
        assertNull(Database.generateIdFilterSQLText("Prop", List.of()));

        currentTestFailed = false;
    }

    @Test
    public void testGenerateIdFilterSQLTextOneOption() {
        assertEquals("Prop IN (123)", Database.generateIdFilterSQLText("Prop", List.of(123)));

        currentTestFailed = false;
    }

    @Test
    public void testGenerateIdFilterSQLTextMultipleOptions() {
        assertEquals("Prop IN (123, 456, 789)", Database.generateIdFilterSQLText("Prop", List.of(123, 456, 789)));

        currentTestFailed = false;
    }

    @Test
    public void testGenerateUniquenessFailedErrorMessageUniqueness() {
        assertEquals(
                Database.generateUniquenessFailedErrorMessage(new SQLException("Error: UNIQUE constraint failed: Airline.ID")),
                "The value for the ID must be unique"
        );

        currentTestFailed = false;
    }

    @Test
    public void testGenerateUniquenessFailedErrorMessageNotUniqueness() {
        String msg = "Error: CHECK constraint failed: IATA or ICAO";
        assertEquals(
                Database.generateUniquenessFailedErrorMessage(new SQLException(msg)), msg
        );

        currentTestFailed = false;
    }
}
