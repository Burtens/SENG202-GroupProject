package seng202.group8.io;

import org.javatuples.Quartet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import seng202.group8.data.*;
import seng202.group8.datacontroller.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ImportTest {
    public Connection db;

    // NOTE:  importTrip and importData take csv files so should be tested manually instead

    Path testDatabasePath;

    @Before
    public void setup() throws SQLException, DataConstraintsException, IOException, URISyntaxException {
        testDatabasePath = Path.of("./", "TEST_DATABASE_DELETE_IF_FOUND.db");

        Database.setDatabasePath();
        db = Database.databaseConnection;
        db.setAutoCommit(false);

        // Generate airports for when testing importRoute which uses long & lat of start and end airports to get distance
        Airport startAirport = new Airport("関西国際空港", "Christchurch", "New Zealand",
                "123", "ABCD",
                57.5, 34.7, 100, 3, 'N');
        Airport endAirport = new Airport("Iceberg", "Wellington", "New Zealand",
                "ZZZ", "1234", 46.7, 89.3, 69, 8, 'U');

        AirportDataController airportData = AirportDataController.getSingleton();
        airportData.save(startAirport);
        airportData.save(endAirport);
    }

    @After
    public void teardown() throws SQLException, IOException {
        if (!Database.databaseConnection.getAutoCommit()) {
            Database.databaseConnection.rollback();
        }
        Database.databaseConnection.close();
        Files.deleteIfExists(testDatabasePath);
    }

    @Test
    public void testCSVReader() throws DataConstraintsException, IOException {
        String row = "\"asd,we\", \"asdwasd\", \"ytr,tr,p\"";

        // test regex for split by comma but not those in quotations
        String[] data = row.split(Import.csvRowSplitRegExp); // <- This is the regex being used in import
        assertEquals(data.length, 3);

        assertEquals(data[0], "\"asd,we\"");
        // trim is a method for removing " or ' from the ends of strings
        // we'll receive data in this form from OpenFlights
        data[0] = Import.trim(data[0]);
        assertEquals(data[0], "asd,we");
    }

    @Test
    public void testCSVReaderNumbers() {
        assertArrayEquals(new String[]{"1234", "456"}, "1234,456".split(Import.csvRowSplitRegExp));
    }

    @Test
    public void testCSVReaderUnquotedStrings() {
        assertArrayEquals(new String[]{"unquoted string", "unquoted string"}, "unquoted string,unquoted string".split(Import.csvRowSplitRegExp));
    }

    @Test
    public void testCSVReaderQuotedStrings() {
        assertArrayEquals(new String[]{"\"quoted string\"", "unquoted string"}, "\"quoted string\",unquoted string".split(Import.csvRowSplitRegExp));
    }

    @Test
    public void testCSVReaderEscapedQuote() {
        assertArrayEquals(new String[]{"\"sandwich\"", "\"DoubleQuote\"\"DoubleQuote\"", "sandwich"}, "\"sandwich\",\"DoubleQuote\"\"DoubleQuote\",sandwich".split(Import.csvRowSplitRegExp));
    }

    @Test
    public void testCSVReaderEscapedQuote2() {
        assertArrayEquals(new String[]{"\"sandwich\"", "\"DoubleQuote\"\"\"", "sandwich"}, "\"sandwich\",\"DoubleQuote\"\"\",sandwich".split(Import.csvRowSplitRegExp));
    }

    @Test
    public void testCSVReaderNewlines() {
        assertArrayEquals(new String[]{"\"sandwich\"", "\"Newline\nNewline\""}, "\"sandwich\",\"Newline\nNewline\"".split(Import.csvRowSplitRegExp));
    }


    @Test
    public void testAirlineImport() throws DataConstraintsException, SQLException {
        // ID(0), name(1), Alias(2), IATA(3), ICAO(4), Callsign(5), Country(6), Active(7)
        String[] airlineData = {"9", "Bob's Planes", "Lil Bobbie", "AB", "123", "Eagle,57", "New Zealand", "Y"};

        Airline airline = Import.importAirline(airlineData);
        assertNotNull(airline); // returns null if something failed to set
        assertEquals(airline.getCountry(), "New Zealand");  // Country in Airline is String not Country Object

        // Fail Airlines: name too short/null, invalid iata/icao, both iata and icao null, country invalid/null, data too short
        List<String[]> badAirlines = new ArrayList<String[]>();
        String[] badAirline1 = {"9", "Bo", "Lil Bobbie", "AB", "123", "Eagle,57", "New Zealand", "Y"};
        String[] badAirline2 = {"9", null, "Lil Bobbie", "AB", "123", "Eagle,57", "New Zealand", "Y"};
        String[] badAirline3 = {"9", "Bob's Planes", "Lil Bobbie", "ABCDEF", "123", "Eagle,57", "New Zealand", "Y"};
        String[] badAirline4 = {"9", "Bob's Planes", "Lil Bobbie", null, "", "Eagle,57", "New Zealand", "Y"};
        String[] badAirline5 = {"9", "Bob's Planes", "Lil Bobbie", "AB", "123", "Eagle,57", "Narnia", "Y"};
        String[] badAirline6 = {"9", "Bob's Planes", "Lil Bobbie", "AB", "123", "Eagle,57", null, "Y"};
        String[] badAirline7 = {"9", "Bob's Planes", "Lil Bobbie", "AB"};
        badAirlines.add(badAirline1);
        badAirlines.add(badAirline2);
        badAirlines.add(badAirline3);
        badAirlines.add(badAirline4);
        badAirlines.add(badAirline5);
        badAirlines.add(badAirline6);
        badAirlines.add(badAirline7);

        for (String[] failCase : badAirlines) {
            Airline badAirline = null;
            try {
                badAirline = Import.importAirline(failCase);
            } catch (DataConstraintsException | IndexOutOfBoundsException | NumberFormatException ignored) {}
            assertNull(badAirline);
        }

    }

    @Test
    public void testAirportImport() throws DataConstraintsException, SQLException {
        // ID(0), Name(1), City(2), Country(3), IATA(4), ICAO(5), Lat(6), Long(7), Alt(8),
        // Timezone (hours from UTC)(9), Daylight Savings Time(10), Timezone code(11)
        String[] airportData = {"3", "Aircraft Carrier", "Christchurch", "New Zealand", "123", "ABCD",
                "57.5", "34.7", "100", "3", "N", "NZ"};

        Airport airport = Import.importAirport(airportData);
        assertNotNull(airport);

        // Fail Airports: bad country, null country, too short name, null name, short data, bad iata, both iata and icao null
        // latitude >90 or <-90, longitude >180 or <-180, timezone > 14 or < -12, altitude < -1240
        List<String[]> badAirports = new ArrayList<String[]>();
        String[] badAirport1 = {"3", "Aircraft Carrier", "Bag End", "Hobbiton", "123", "ABCD", "57.5", "34.7", "100", "3", "N", "NZ"};
        String[] badAirport2 = {"3", "Aircraft Carrier", "Bag End", null, "123", "ABCD", "57.5", "34.7", "100", "3", "N", "NZ"};
        String[] badAirport3 = {"3", "Ai", "Bag End", "New Zealand", "123", "ABCD", "57.5", "34.7", "100", "3", "N", "NZ"};
        String[] badAirport4 = {"3", null, "Bag End", "New Zealand", "123", "ABCD", "57.5", "34.7", "100", "3", "N", "NZ"};
        String[] badAirport5 = {"3", "Aircraft Carrier", "Bag End", "New Zealand"};
        String[] badAirport6 = {"3", "Aircraft Carrier", "Bag End", "New Zealand", "123456", "ABCD", "57.5", "34.7", "100", "3", "N", "NZ"};
        String[] badAirport7 = {"3", "Aircraft Carrier", "Bag End", "New Zealand", null, null, "57.5", "34.7", "100", "3", "N", "NZ"};
        String[] badAirport8 = {"3", "Aircraft Carrier", "Bag End", "New Zealand", "123", "ABCD", "91", "34.7", "100", "3", "N", "NZ"};
        String[] badAirport9 = {"3", "Aircraft Carrier", "Bag End", "New Zealand", "123", "ABCD", "-91", "34.7", "100", "3", "N", "NZ"};
        String[] badAirport10 = {"3", "Aircraft Carrier", "Bag End", "New Zealand", "123", "ABCD", "57.5", "181", "100", "3", "N", "NZ"};
        String[] badAirport11 = {"3", "Aircraft Carrier", "Bag End", "New Zealand", "123", "ABCD", "57.5", "-181", "100", "3", "N", "NZ"};
        String[] badAirport12 = {"3", "Aircraft Carrier", "Bag End", "New Zealand", "123", "ABCD", "57.5", "34.7", "100", "15", "N", "NZ"};
        String[] badAirport13 = {"3", "Aircraft Carrier", "Bag End", "New Zealand", "123", "ABCD", "57.5", "34.7", "100", "-13", "N", "NZ"};
        String[] badAirport14 = {"3", "Aircraft Carrier", "Bag End", "New Zealand", "123", "ABCD", "57.5", "34.7", "-1241", "3", "N", "NZ"};
        badAirports.add(badAirport1);
        badAirports.add(badAirport2);
        badAirports.add(badAirport3);
        badAirports.add(badAirport4);
        badAirports.add(badAirport5);
        badAirports.add(badAirport6);
        badAirports.add(badAirport7);
        badAirports.add(badAirport8);
        badAirports.add(badAirport9);
        badAirports.add(badAirport10);
        badAirports.add(badAirport11);
        badAirports.add(badAirport12);
        badAirports.add(badAirport13);
        badAirports.add(badAirport14);

        for (String[] failCase : badAirports) {
            Airport badAirport = null;
            try {
                badAirport = Import.importAirport(failCase);
            } catch (DataConstraintsException | IndexOutOfBoundsException | NumberFormatException ignored) {}
            assertNull(badAirport);
        }

    }

    public <DataType extends Data> void importTestHarness(String input, DataType expect) throws IOException, SQLException, URISyntaxException {
        Database.setDatabasePath(testDatabasePath.toUri());
        Path fileout = Path.of("./", "TEST_IMPORT_FILE_DELETE_IF_FOUND.csv");
        String fileoutString = new File(fileout.toUri()).getCanonicalPath();

        FileWriter writer = new FileWriter(fileoutString, Import.FILE_ENCODING);
        writer.write(input);
        writer.close();
        if (expect instanceof Airport) {
            Quartet<Integer, Integer, Long, String> result = Import.importData(fileoutString, "Airport");
            Files.delete(fileout);
            assertEquals(0, result.getValue1().intValue());
            Airport cast = (Airport) expect;
            checkAirportsEqual(cast, AirportDataController.getSingleton().getEntity(cast.getCode()));
        } else if (expect instanceof Airline) {
            Quartet<Integer, Integer, Long, String> result = Import.importData(fileoutString, "Airline");
            Files.delete(fileout);
            assertEquals(0, result.getValue1().intValue());
            Airline cast = (Airline) expect;
            checkAirlinesEqual(cast, AirlineDataController.getSingleton().getEntity(cast.getCode()));
        } else if (expect instanceof Route) {
            Quartet<Integer, Integer, Long, String> result = Import.importData(fileoutString, "Route");
            Files.delete(fileout);
            assertEquals(0, result.getValue1().intValue());
            Route cast = (Route) expect;
            checkRoutesEqual(cast, RouteDataController.getSingleton().getEntity(cast.getSourceAirportCode(), cast.getDestinationAirportCode(), cast.getAirlineCode()));
        }
    }

    public void checkAirportsEqual(Airport airport1, Airport airport2) {
        assertNotNull(airport2);
        new AirportDataControllerTest().checkAirportsEqual(airport1, airport2);
    }

    public void checkAirlinesEqual(Airline airline1, Airline airline2) {
        assertNotNull(airline2);
        new AirlineDataControllerTest().checkAirlinesEqual(airline1, airline2);
    }

    public void checkRoutesEqual(Route route1, Route route2) {
        assertNotNull(route2);
        assertEquals(route1.getAirlineCode(), route2.getAirlineCode());
        assertEquals(route1.getSourceAirportCode(), route2.getSourceAirportCode());
        assertEquals(route1.getDestinationAirportCode(), route2.getDestinationAirportCode());
        assertArrayEquals(route1.getPlaneTypes(), route2.getPlaneTypes());
    }

//        Airline	2-letter (IATA) or 3-letter (ICAO) code of the airline.
//        Airline ID	Unique OpenFlights identifier for airline (see Airline).
//        Source airport	3-letter (IATA) or 4-letter (ICAO) code of the source airport.
//        Source airport ID	Unique OpenFlights identifier for source airport (see Airport)
//        Destination airport	3-letter (IATA) or 4-letter (ICAO) code of the destination airport.
//        Destination airport ID	Unique OpenFlights identifier for destination airport (see Airport)
//        Codeshare	"Y" if this flight is a codeshare (that is, not operated by Airline, but another carrier), empty otherwise.
//        Stops	Number of stops on this flight ("0" for direct)
//        Equipment	3-letter codes for plane type(s) generally used on this flight, separated by spaces
//        The data is UTF-8 encoded. The special value \N is used for "NULL" to indicate that no value is available, and is understood automatically by MySQL if imported.
    @Test
    public void testRouteImportFileNoQuotes() throws SQLException, DataConstraintsException, IOException, URISyntaxException {
        String input = "AIR,999,1234,999,DEST, 998 ,,0,747 777"; // No quotes, no codeshare encoded as empty, two plane types
        // Airline AIR, source 1234, dest 4567
        Route route = new Route("AIR", "1234", "DEST", new String[]{"747", "777"}, 100, false, 25, new ArrayList<>());
        importTestHarness(input, route);
    }
    
    @Test
    public void testRouteImport() throws DataConstraintsException, SQLException {
        // AirlineCode(0), Airline ID(1), Source Airport(2), Source Airport ID(3), Destination Airport(4)
        // Destination Airport ID(5), Codeshare(6), Stops(7), Equipment(8)
        String[] routeData = {"2B", "410", "123", "57", "ZZZ", "46", "Y", "0", "CR2 HF3 WE8"};

        Route route = Import.importRoute(routeData);
        assertNotNull(route);

        // Test no destination airport
        String[] noCountryData = {"2B", "410", "123", "57", "P1F", "46", null, "0", "CR2 HF3 WE8"}; // P1F is not an airport
        Route noCountryRoute = Import.importRoute(noCountryData);
        assertNotNull(noCountryRoute);  // Allowed to have no Airport at import, can be dealt with later
        assert (noCountryRoute.getFlightDuration() == 0);
        assert (noCountryRoute.getPrice() == 0);

        // Fail cases: too short, bad airline, bad airports
        List<String[]> badRoutes = new ArrayList<String[]>();
        String[] badRoute1 = {"2B", "410", "123", "57", "ZZZ", "46"};
        String[] badRoute2 = {"2B9FRE", "410", "123", "57", "ZZZ", "46", "Y", "0", "CR2 HF3 WE8"};
        String[] badRoute3 = {"2B", "410", "123FE", "57", "ZZZDFE", "46", "Y", "0", "CR2 HF3 WE8"};
        badRoutes.add(badRoute1);
        badRoutes.add(badRoute2);
        badRoutes.add(badRoute3);

        for (String[] failCase : badRoutes) {
            Route badRoute = null;
            try {
                badRoute = Import.importRoute(failCase);
            } catch (DataConstraintsException | SQLException | IndexOutOfBoundsException | NumberFormatException ignored) {}
            assertNull(badRoute);
        }
    }

    @Test
    public void testTripFlightsImport() throws DataConstraintsException {
        // airline code, source code, destination code, takeoffTime, takeoffDate, comment
        List<String[]> goodTripFlights = new ArrayList<String[]>();
        List<String[]> badTripFlights = new ArrayList<String[]>();

        // good flights: takeoff times from 0 to 24*60-1 hours, any day, any time zone, null comment, empty string comment
        LocalDate day = LocalDate.of(2020, 9, 20);
        String[] flight1 = {"AB", "ZZZ", "123", "0", day.toString(), "First available takeoff"};
        String[] flight2 = {"AB", "ZZZ", "123", "250", day.toString(), ""};
        String[] flight3 = {"AB", "ZZZ", "123", "558", day.toString(), null};
        String[] flight4 = {"AB", "ZZZ", "123", "1439", day.toString(), "Last available takeoff"};
        goodTripFlights.add(flight1);
        goodTripFlights.add(flight2);
        goodTripFlights.add(flight3);
        goodTripFlights.add(flight4);

        // bad flights: source and destination bad, airline bad, time < 0, time >= 24*60, too short, nonsense
        String[] badFlight1 = {"123", "ZZZ", "AB", "-1", day.toString(), "Before first available takeoff"};
        String[] badFlight2 = {"1", "ZZZHJ9", "AB", "250", day.toString(), "Bad airports"};
        String[] badFlight3 = {"123", "ZZZ", "ASDFG", "558", day.toString(), "Bad airline"};
        String[] badFlight4 = {"123", "ZZZ", "AB", "1440", day.toString(), "After last available takeoff"};
        String[] badFlight5 = {"123", "ZZZ", "AB", "250"};
        String[] badFlight6 = {"sadesdf", "123@FDS^", "ASDE*", "09()sdfe", "tgvert*(^%$", ":LKJHUP\""};
        String[] badFlight7 = {"123", "ZZZ", "AB\" \"-1", day.toString(), "Before first available takeoff"};
        badTripFlights.add(badFlight1);
        badTripFlights.add(badFlight2);
        badTripFlights.add(badFlight3);
        badTripFlights.add(badFlight4);
        badTripFlights.add(badFlight5);
        badTripFlights.add(badFlight6);
        badTripFlights.add(badFlight7);

        for (String[] goodFlight : goodTripFlights) {
            TripFlight goodTrip = Import.importTripFlight(goodFlight);
            assertNotNull(goodTrip);
        }
        for (String[] badFlight : badTripFlights) {
            TripFlight badTrip = Import.importTripFlight(badFlight);
            assertNull(badTrip);
        }
    }

    public void importTripTest() throws DataConstraintsException {
        Trip trip = new Trip("Test", "Bad flights");

    }

    public void printTrip(Trip trip) {
        System.out.println(String.format("Name: %s\nComment:%s\n", trip.getName(), trip.getComment() == null ? "" : trip.getComment()));
        for (TripFlight flight : trip.getFlights()) {
            System.out.println(String.format("SRC: %s; DST: %s; AIR: %s; DPRT: %s; CMTN: %s",
                    flight.getSourceCode(), flight.getDestinationCode(), flight.getAirlineCode(),
                    flight.getUTCTakeoffDateTime().toString(),
                    flight.getComment() == null ? "" : flight.getComment()
            ));
        }
    }

    @Test
    public void testChooseDataController() {
        DataController<?> dc1 = Import.chooseDataController("Airport");
        DataController<?> dc2 = Import.chooseDataController("Airline");
        DataController<?> dc3 = Import.chooseDataController("Route");
        DataController<?> dc4 = Import.chooseDataController("Trip");
        assert(dc1 instanceof AirportDataController);
        assert(dc2 instanceof AirlineDataController);
        assert(dc3 instanceof RouteDataController);
        assertNull(dc4);
    }

    @Test
    public void testExportImportTrip() throws DataConstraintsException, IOException {
        Trip trip = new Trip("This \"name'asdkljf!#\\//", null);
        trip.addFlight(new TripFlight("SRC", "DST", "AIR", ZonedDateTime.now(), "This is a comment with newline\n\n and quotes \", ', backslashes \\ and slashes //. Don't die on this!"));
        trip.addFlight(new TripFlight("DST", "SRC", "AIR", ZonedDateTime.now().plusHours(20), null));
        trip.addFlight(new TripFlight("SRC", "ZZZ", "999", ZonedDateTime.now().plusHours(40), "sudo make me a sandwich"));
        trip.addFlight(new TripFlight("SRC", "ZZZ", "999", ZonedDateTime.now().plusHours(40), "UTF-8 plz. Egilsstaðir Airport. 令和. 🎁 ; DROP TABLE airports;"));

        Path fileout = Path.of("./", "TEST_TRIP_EXPORT_DELETE_IF_FOUND.mtyg");
        String fileoutString = new File(fileout.toUri()).getCanonicalPath();
        Export.exportTrip(trip, fileoutString);
        Trip result = Import.importTrip(fileoutString);
        Files.delete(fileout);

//        System.out.println("EXPORTED TRIP, THEN IMPORTED TRIP:");
//        System.out.println("INITIAL TRIP:");
//        printTrip(trip);
//        System.out.println("-----------GOT BACK:------------");
//        printTrip(result);

        new TripDataControllerTest().testEquals(trip, result);
    }

}
