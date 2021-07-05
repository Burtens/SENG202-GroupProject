package seng202.group8.datacontroller;

import com.opencsv.CSVReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import seng202.group8.data.Airline;
import seng202.group8.data.Airport;
import seng202.group8.data.Route;
import seng202.group8.io.Database;
import seng202.group8.io.Import;
import seng202.group8.io.SortOrder;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;

public class getSortedFilteredEntitiesTest {
    AirlineDataController airlineDataController = AirlineDataController.getSingleton();
    AirportDataController airportDataController = AirportDataController.getSingleton();
    RouteDataController routeDataController = RouteDataController.getSingleton();

    ArrayList<Airline> airlinesListDirectFromCSV = new ArrayList<>();
    ArrayList<Airport> airportsListDirectFromCSV = new ArrayList<>();
    ArrayList<Route> routesListDirectFromCSV = new ArrayList<>();

    /**
     * Compares two lists containing airline objects
     *
     * @param list1        List generated by AirlineDataController.getFilteredSortedEntities method.
     * @param list2        List created to test against
     * @param size         amount of data being compared
     * @param valueToCheck type of data to compare. Used for sorting and filtering when there are multiple objects with the
     *                     same data.
     */
    private void compareAirlinesInList(List<Airline> list1, List<Airline> list2, Integer size, String valueToCheck) {
        for (int i = 0; i < size; i++) {
            //Compares the values of each airline
            if (valueToCheck.equals("Name") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getName(), list2.get(i).getName());
            if (valueToCheck.equals("Country") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getCountry(), list2.get(i).getCountry());
            if (valueToCheck.equals("Callsign") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getCallsign(), list2.get(i).getCallsign());
            if (valueToCheck.equals("Iata") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getIata(), list2.get(i).getIata());
            if (valueToCheck.equals("Icao") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getIcao(), list2.get(i).getIcao());
        }
    }

    /**
     * Compares two lists containing airport objects
     *
     * @param list1        List generated by AirportDataController.getFilteredSortedEntities method.
     * @param list2        List created to test against
     * @param size         amount of data being compared
     * @param valueToCheck type of data to compare. Used for sorting and filtering when there are multiple objects with the
     *                     same data.
     */
    private void compareAirportsInList(List<Airport> list1, List<Airport> list2, Integer size, String valueToCheck) {
        for (int i = 0; i < size; i++) {
            //Compares the values of each Airport
            if (valueToCheck.equals("Name") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getName(), list2.get(i).getName());
            if (valueToCheck.equals("Country") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getCountry(), list2.get(i).getCountry());
            if (valueToCheck.equals("City") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getCity(), list2.get(i).getCity());
            if (valueToCheck.equals("Dst") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getDst(), list2.get(i).getDst());
            if (valueToCheck.equals("Iata") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getIata(), list2.get(i).getIata());
            if (valueToCheck.equals("Icao") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getIcao(), list2.get(i).getIcao());
            if (valueToCheck.equals("Altitude") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getAltitude(), list2.get(i).getAltitude());
            if (valueToCheck.equals("Latitude") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getLatitude(), list2.get(i).getLatitude(), 0.0);
            if (valueToCheck.equals("Longitude") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getLongitude(), list2.get(i).getLongitude(), 0.0);
            if (valueToCheck.equals("Timezone") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getTimezone(), list2.get(i).getTimezone(), 0.0);
        }
    }

    /**
     * Compares two lists containing routes objects
     *
     * @param list1        List generated by RouteDataController.getFilteredSortedEntities method.
     * @param list2        List created to test against
     * @param size         amount of data being compared
     * @param valueToCheck type of data to compare. Used for sorting and filtering when there are multiple objects with the
     *                     same data.
     */
    private void compareRoutesInList(List<Route> list1, List<Route> list2, Integer size, String valueToCheck) {
        for (int i = 0; i < size; i++) {
            //Compares the values of each route
            if (valueToCheck.equals("Airline") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getAirlineCode(), list2.get(i).getAirlineCode());
            if (valueToCheck.equals("Source") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getSourceAirportCode(), list2.get(i).getSourceAirportCode());
            if (valueToCheck.equals("Destination") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getDestinationAirportCode(), list2.get(i).getDestinationAirportCode());
            if (valueToCheck.equals("Duration") || valueToCheck.equals("All"))
                assertEquals(list1.get(i).getFlightDuration(), list2.get(i).getFlightDuration());
        }
    }


    @Before
    public void setUp() throws Exception {
        Database.establishConnection();
        Database.setDatabasePath(Paths.get(new File("src/test/java/seng202/group8/datacontroller/").getCanonicalPath(), "testDB").toUri());
        Import.importData("src/main/resources/seng202/group8/testCSVFiles/testAirlineData.csv", "Airline");
        Import.importData("src/main/resources/seng202/group8/testCSVFiles/testAirportData.csv", "Airport");
        Import.importData("src/main/resources/seng202/group8/testCSVFiles/testRouteData.csv", "Route");

        CSVReader csvReader = new CSVReader(new FileReader("src/main/resources/seng202/group8/testCSVFiles/testAirlineData.csv", Import.FILE_ENCODING));
        String[] airlineValues;
        while ((airlineValues = csvReader.readNext()) != null) {
            airlinesListDirectFromCSV.add(new Airline(airlineValues[1], airlineValues[5], airlineValues[3], airlineValues[4], airlineValues[6]));
        }

        csvReader = new CSVReader(new FileReader("src/main/resources/seng202/group8/testCSVFiles/testAirportData.csv", Import.FILE_ENCODING));
        String[] airportValues;
        while ((airportValues = csvReader.readNext()) != null) {
            airportsListDirectFromCSV.add(new Airport(airportValues[1], airportValues[2], airportValues[3], airportValues[4],
                    airportValues[5], Double.parseDouble(airportValues[6]), Double.parseDouble(airportValues[7]),
                    Integer.parseInt(airportValues[8]), Double.parseDouble(airportValues[9]), airportValues[10].charAt(0)));
        }

        csvReader = new CSVReader(new FileReader("src/main/resources/seng202/group8/testCSVFiles/testRouteData.csv", Import.FILE_ENCODING));
        String[] routeValues;
        while ((routeValues = csvReader.readNext()) != null) {
            routesListDirectFromCSV.add(new Route(routeValues[0], routeValues[2], routeValues[4],
                    new String[]{}, 0, true, 0, new ArrayList<>()));
        }
    }

    @Test
    public void testNumrowsAndOffsetAirlines() {
        try {
            //Checks correct amount of rows are returned. Based off values user can set in app
            assertEquals(50, airlineDataController.getSortedFilteredEntities(null, null, 50, 0).size());
            assertEquals(80, airlineDataController.getSortedFilteredEntities(null, null, 80, 0).size());
            assertEquals(100, airlineDataController.getSortedFilteredEntities(null, null, 100, 0).size());

            //Checks from offset 0
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(null, null, 50, 0), airlinesListDirectFromCSV, 50, "All");
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(null, null, 80, 0), airlinesListDirectFromCSV, 80, "All");
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(null, null, 100, 0), airlinesListDirectFromCSV, 100, "All");

            //Checks from offset 50
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(null, null, 50, 50), airlinesListDirectFromCSV.subList(50, 200), 50, "All");
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(null, null, 80, 50), airlinesListDirectFromCSV.subList(50, 200), 80, "All");
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(null, null, 100, 50), airlinesListDirectFromCSV.subList(50, 200), 100, "All");

            //Checks from offset 100
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(null, null, 50, 100), airlinesListDirectFromCSV.subList(100, 200), 50, "All");
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(null, null, 80, 100), airlinesListDirectFromCSV.subList(100, 200), 80, "All");
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(null, null, 100, 100), airlinesListDirectFromCSV.subList(100, 200), 100, "All");

        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    //Data controllers should return the right amount of data when amount is called.
    @Test
    public void testNumrowsAndOffsetAirports() {
        try {
            assertEquals(50, AirportDataController.getSingleton().getSortedFilteredEntities(null, null, 50, 0).size());
            assertEquals(80, AirportDataController.getSingleton().getSortedFilteredEntities(null, null, 80, 0).size());
            assertEquals(100, AirportDataController.getSingleton().getSortedFilteredEntities(null, null, 100, 0).size());

            //Checks from offset 0
            compareAirportsInList(airportDataController.getSortedFilteredEntities(null, null, 50, 0), airportsListDirectFromCSV, 50, "All");
            compareAirportsInList(airportDataController.getSortedFilteredEntities(null, null, 80, 0), airportsListDirectFromCSV, 80, "All");
            compareAirportsInList(airportDataController.getSortedFilteredEntities(null, null, 100, 0), airportsListDirectFromCSV, 100, "All");

            //Checks from offset 50
            compareAirportsInList(airportDataController.getSortedFilteredEntities(null, null, 50, 50), airportsListDirectFromCSV.subList(50, 200), 50, "All");
            compareAirportsInList(airportDataController.getSortedFilteredEntities(null, null, 80, 50), airportsListDirectFromCSV.subList(50, 200), 80, "All");
            compareAirportsInList(airportDataController.getSortedFilteredEntities(null, null, 100, 50), airportsListDirectFromCSV.subList(50, 200), 100, "All");

            //Checks from offset 100
            compareAirportsInList(airportDataController.getSortedFilteredEntities(null, null, 50, 100), airportsListDirectFromCSV.subList(100, 200), 50, "All");
            compareAirportsInList(airportDataController.getSortedFilteredEntities(null, null, 80, 100), airportsListDirectFromCSV.subList(100, 200), 80, "All");
            compareAirportsInList(airportDataController.getSortedFilteredEntities(null, null, 100, 100), airportsListDirectFromCSV.subList(100, 200), 100, "All");

        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testNumrowsAndOffsetRoutes() {
        try {
            assertEquals(10, RouteDataController.getSingleton().getSortedFilteredEntities(null, null, 10, 0).size());
            assertEquals(20, RouteDataController.getSingleton().getSortedFilteredEntities(null, null, 20, 0).size());
            assertEquals(32, RouteDataController.getSingleton().getSortedFilteredEntities(null, null, 32, 0).size());

            compareRoutesInList(routeDataController.getSortedFilteredEntities(null, null, 10, 0), routesListDirectFromCSV, 10, "All");
            compareRoutesInList(routeDataController.getSortedFilteredEntities(null, null, 10, 10), routesListDirectFromCSV.subList(10, 32), 10, "All");
            compareRoutesInList(routeDataController.getSortedFilteredEntities(null, null, 12, 20), routesListDirectFromCSV.subList(20, 32), 12, "All");

        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSortingAirlines() {
        List<Airline> airlinesSorting = new ArrayList<>(List.copyOf(airlinesListDirectFromCSV));

        try {
            airlinesSorting.sort(Comparator.comparing(Airline::getCountry, Comparator.nullsLast(Comparator.naturalOrder())));
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(Airline.COUNTRY, SortOrder.ASCENDING, 200, 0), airlinesSorting, 200, "Country");

            airlinesSorting.sort(Comparator.comparing(Airline::getIcao, Comparator.nullsLast(Comparator.naturalOrder())));
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(Airline.ICAO, SortOrder.ASCENDING, 200, 0), airlinesSorting, 200, "Icao");

            airlinesSorting.sort(Comparator.comparing(Airline::getName, Comparator.nullsLast(Comparator.naturalOrder())));
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(Airline.NAME, SortOrder.ASCENDING, 200, 0), airlinesSorting, 200, "Name");

            airlinesSorting.sort(Comparator.comparing(Airline::getName, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(Airline.NAME, SortOrder.DESCENDING, 200, 0), airlinesSorting, 200, "Name");

            airlinesSorting.sort(Comparator.comparing(Airline::getIata, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(Airline.IATA, SortOrder.DESCENDING, 200, 0), airlinesSorting, 200, "Iata");

            airlinesSorting.sort(Comparator.comparing(Airline::getCallsign, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
            compareAirlinesInList(airlineDataController.getSortedFilteredEntities(Airline.CALLSIGN, SortOrder.DESCENDING, 200, 0), airlinesSorting, 200, "Callsign");

        } catch (SQLException e) {
            fail(e.getMessage());
        }

    }


    @Test
    public void testSortingAirports() {
        List<Airport> airportsSorting = new ArrayList<>(List.copyOf(airportsListDirectFromCSV));

        try {
            airportsSorting.sort(Comparator.comparing(Airport::getCountry, Comparator.nullsLast(Comparator.naturalOrder())));

            compareAirportsInList(airportDataController.getSortedFilteredEntities(Airport.COUNTRY, SortOrder.ASCENDING, 200, 0), airportsSorting, 200, "Country");

            airportsSorting.sort(Comparator.comparing(Airport::getIcao, Comparator.nullsLast(Comparator.naturalOrder())));
            compareAirportsInList(airportDataController.getSortedFilteredEntities(Airport.ICAO, SortOrder.ASCENDING, 200, 0), airportsSorting, 200, "Icao");

            airportsSorting.sort(Comparator.comparing(Airport::getName, Comparator.nullsLast(Comparator.naturalOrder())));
            compareAirportsInList(airportDataController.getSortedFilteredEntities(Airport.NAME, SortOrder.ASCENDING, 200, 0), airportsSorting, 200, "Name");

            airportsSorting.sort(Comparator.comparing(Airport::getAltitude, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
            compareAirportsInList(airportDataController.getSortedFilteredEntities(Airport.ALTITUDE, SortOrder.DESCENDING, 200, 0), airportsSorting, 200, "Altitude");

            airportsSorting.sort(Comparator.comparing(Airport::getIata, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
            compareAirportsInList(airportDataController.getSortedFilteredEntities(Airport.IATA, SortOrder.DESCENDING, 200, 0), airportsSorting, 200, "Iata");

            airportsSorting.sort(Comparator.comparing(Airport::getCity, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
            compareAirportsInList(airportDataController.getSortedFilteredEntities(Airport.CITY, SortOrder.DESCENDING, 200, 0), airportsSorting, 200, "City");

        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testSortingRoutes() {
        List<Route> routeSorting = new ArrayList<>(List.copyOf(routesListDirectFromCSV));

        try {
            routeSorting.sort(Comparator.comparing(Route::getAirlineCode, Comparator.nullsLast(Comparator.naturalOrder())));
            compareRoutesInList(routeDataController.getSortedFilteredEntities(Route.AIRLINE_CODE, SortOrder.ASCENDING, 200, 0), routeSorting, 32, "Airline");

            routeSorting.sort(Comparator.comparing(Route::getSourceAirportCode, Comparator.nullsLast(Comparator.naturalOrder())));
            compareRoutesInList(routeDataController.getSortedFilteredEntities(Route.SOURCE_AIRPORT_CODE, SortOrder.ASCENDING, 200, 0), routeSorting, 32, "Source");

            routeSorting.sort(Comparator.comparing(Route::getDestinationAirportCode, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
            compareRoutesInList(routeDataController.getSortedFilteredEntities(Route.DESTINATION_AIRPORT_CODE, SortOrder.DESCENDING, 200, 0), routeSorting, 32, "Destination");

            routeSorting.sort(Comparator.comparing(Route::getFlightDuration, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
            compareRoutesInList(routeDataController.getSortedFilteredEntities(Route.FLIGHT_DURATION, SortOrder.DESCENDING, 200, 0), routeSorting, 32, "Duration");


        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testFilteringAirlines() {
        FiltersController filtersController = FiltersController.getSingleton();

        try {
            //Adds selected code to filter
            filtersController.getAirlineCodeFilter().setSelectedOptions(Collections.singleton("1T"));
            //Checks if returned list contains one value a airline with Iata "1T"
            assertEquals(1, airlineDataController.getSortedFilteredEntities(null, null, 200, 0).size());
            assertEquals("1T", airlineDataController.getSortedFilteredEntities(null, null, 200, 0).get(0).getCode());

            //Checks if clearing filter options means all data is returned again.
            filtersController.getAirlineCodeFilter().setSelectedOptions(Collections.EMPTY_LIST);
            assertEquals(200, airlineDataController.getSortedFilteredEntities(null, null, 200, 0).size());

            //Adds another code to filter
            filtersController.getAirlineCodeFilter().setSelectedOptions(Arrays.asList("1T", "WYT"));

            assertEquals(2, airlineDataController.getSortedFilteredEntities(null, null, 200, 0).size());
            assertEquals("WYT", airlineDataController.getSortedFilteredEntities(null, null, 200, 0).get(1).getCode());
            assertEquals("1T", airlineDataController.getSortedFilteredEntities(null, null, 200, 0).get(0).getCode());

            filtersController.getAirlineNameFilter().setSelectedOptions(Collections.singletonList("223 Flight Unit State Airline"));

            //Checks that if both filters are enabled because there is no comparison the returned list should be empty
            assertEquals(0, airlineDataController.getSortedFilteredEntities(null, null, 200, 0).size());

            filtersController.getAirlineCodeFilter().setSelectedOptions(Collections.EMPTY_LIST);
            assertEquals(1, airlineDataController.getSortedFilteredEntities(null, null, 200, 0).size());
            assertEquals("223 Flight Unit State Airline", airlineDataController.getSortedFilteredEntities(null, null, 200, 0).get(0).getName());
            filtersController.getAirlineNameFilter().setSelectedOptions(Collections.EMPTY_LIST);

            //Filtering By Countries.
            filtersController.getCountryFilter().setSelectedOptions(Collections.singletonList("Russia"));
            assertEquals(7, airlineDataController.getSortedFilteredEntities(null, null, 200, 0).size());

            for (Airline airline : airlineDataController.getSortedFilteredEntities(null, null, 200, 0)) {
                assertEquals("Russia", airline.getCountry());
            }

            filtersController.getCountryFilter().setSelectedOptions(Arrays.asList("Russia", "United States"));
            assertEquals(37, airlineDataController.getSortedFilteredEntities(null, null, 200, 0).size());

            for (Airline airline : airlineDataController.getSortedFilteredEntities(null, null, 200, 0)) {
                assertTrue(airline.getCountry().equals("Russia") || airline.getCountry().equals("United States"));
            }

            filtersController.getCountryFilter().setSelectedOptions(Collections.EMPTY_LIST);
            filtersController.getCountryFilter().removeOption("Russia");
            filtersController.getCountryFilter().removeOption("United States");
            filtersController.getAirlineNameFilter().removeOption("223 Flight Unit State Airline");
            filtersController.getAirlineCodeFilter().removeOption("1T");
            filtersController.getAirlineCodeFilter().removeOption("WYT");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFilteringAirports() {
        FiltersController filtersController = FiltersController.getSingleton();

        try {
            //Adds selected code to filter

            filtersController.getAirportNameFilter().setSelectedOptions(Collections.EMPTY_LIST);
            filtersController.getAirportCodeFilter().setSelectedOptions(Collections.singleton("SFJ"));
            //Checks if returned list contains one value
            assertEquals(1, airportDataController.getSortedFilteredEntities(null, null, 200, 0).size());
            assertTrue(airportDataController.getSortedFilteredEntities(null, null, 200, 0).get(0).getCode().equals("SFJ"));

            //Checks if clearing means all data is returned again.
            filtersController.getAirportCodeFilter().setSelectedOptions(Collections.EMPTY_LIST);
            assertEquals(200, airportDataController.getSortedFilteredEntities(null, null, 200, 0).size());

            //Adds another code to filter
            filtersController.getAirportCodeFilter().setSelectedOptions(Arrays.asList("VEY", "YBG"));

            assertEquals(2, airportDataController.getSortedFilteredEntities(null, null, 200, 0).size());
            assertTrue(airportDataController.getSortedFilteredEntities(null, null, 200, 0).get(0).getCode().equals("VEY"));
            assertTrue(airportDataController.getSortedFilteredEntities(null, null, 200, 0).get(1).getName().equals("CFB Bagotville"));

            filtersController.getAirportNameFilter().setSelectedOptions(Arrays.asList("Chapleau Airport"));

            //Checks that if both filters are enabled because there is no comparison the returned list should be empty
            assertEquals(0, airportDataController.getSortedFilteredEntities(null, null, 200, 0).size());

            filtersController.getAirportCodeFilter().setSelectedOptions(Collections.EMPTY_LIST);
            assertEquals(1, airportDataController.getSortedFilteredEntities(null, null, 200, 0).size());
            assertTrue(airportDataController.getSortedFilteredEntities(null, null, 200, 0).get(0).getName().equals("Chapleau Airport"));
            filtersController.getAirportNameFilter().setSelectedOptions(Collections.EMPTY_LIST);

            //Filtering by Country. 180 Airports in Canada based on testCSV.
            filtersController.getCountryFilter().setSelectedOptions(Collections.singletonList("Canada"));
            assertEquals(180, airportDataController.getSortedFilteredEntities(null, null, 200, 0).size());

            //Filtering by Country. 180 Airports in Canada and 4 Airports in Greenland based on testCSV.
            filtersController.getCountryFilter().setSelectedOptions(Arrays.asList("Canada", "Greenland"));
            assertEquals(184, airportDataController.getSortedFilteredEntities(null, null, 200, 0).size());
            for (Airport airport : airportDataController.getSortedFilteredEntities(null, null, 200, 0)) {
                assertTrue(airport.getCountry().equals("Canada") || airport.getCountry().equals("Greenland"));
            }

            filtersController.getCountryFilter().setSelectedOptions(Collections.EMPTY_LIST);
            assertEquals(200, airportDataController.getSortedFilteredEntities(null, null, 200, 0).size());

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testAirportFiltersUpdatedAfterDeletion() throws SQLException {
        FiltersController filtersController = FiltersController.getSingleton();
        int prevCodeCount = filtersController.getAirportCodeFilter().getOptions().size();
        int prevNameCount = filtersController.getAirportNameFilter().getOptions().size();
        Airport prevAirport = airportDataController.getEntity(1);

        airportDataController.deleteFromDatabase(1);

        int newCodeCount = filtersController.getAirportCodeFilter().getOptions().size();
        int newNameCount = filtersController.getAirportCodeFilter().getOptions().size();
        assertEquals(prevCodeCount - 1, newCodeCount);
        assertEquals(prevNameCount - 1, newNameCount);
        assertFalse(filtersController.getAirportCodeFilter().getOptions().contains(prevAirport.getCode()));
        assertFalse(filtersController.getAirportNameFilter().getOptions().contains(prevAirport.getName()));
    }

    @Test
    public void testAirlineFiltersUpdatedAfterDeletion() throws SQLException {
        FiltersController filtersController = FiltersController.getSingleton();
        int prevCodeCount = filtersController.getAirlineCodeFilter().getOptions().size();
        int prevNameCount = filtersController.getAirlineNameFilter().getOptions().size();
        Airline prevAirline = airlineDataController.getEntity(1);

        airlineDataController.deleteFromDatabase(1);

        int newCodeCount = filtersController.getAirlineCodeFilter().getOptions().size();
        int newNameCount = filtersController.getAirlineCodeFilter().getOptions().size();
        assertEquals(prevCodeCount - 1, newCodeCount);
        assertEquals(prevNameCount - 1, newNameCount);
        assertFalse(filtersController.getAirlineCodeFilter().getOptions().contains(prevAirline.getCode()));
        assertFalse(filtersController.getAirlineNameFilter().getOptions().contains(prevAirline.getName()));
    }


    @After
    public void tearDown() throws Exception {
        Database.databaseConnection.close();
        File db = new File("src/test/java/seng202/group8/datacontroller/testDB");
        assertTrue(db.delete());
        Database.setDatabasePath();
        airportDataController.onDBChange();
        airlineDataController.onDBChange();
        routeDataController.onDBChange();
    }
}
