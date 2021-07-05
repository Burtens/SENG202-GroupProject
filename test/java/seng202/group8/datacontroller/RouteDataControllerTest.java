package seng202.group8.datacontroller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import seng202.group8.data.Airline;
import seng202.group8.data.Airport;
import seng202.group8.data.Country;
import seng202.group8.data.Route;
import seng202.group8.io.ConstraintsError;
import seng202.group8.io.Database;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class RouteDataControllerTest {

    public Connection db;
    public Country country;
    public RouteDataController routeDC;
    public AirportDataController airportDC;
    public AirlineDataController airlineDC;
    public Airport airport;
    public Airport airport2;
    public Airline airline;
    public Route route;
    public Path testDBPath = null;
    public boolean testDBUsed;

    public void checkRoutesEqual(Route route1, Route route2) {
        assertNotNull(route1);
        assertNotNull(route2);

        assertEquals(route1.getAirlineCode(), route2.getAirlineCode());
        assertEquals(route1.getDestinationAirportCode(), route2.getDestinationAirportCode());
        assertEquals(route1.getSourceAirportCode(), route2.getSourceAirportCode());
        assertEquals(route1.getFlightDuration(), route2.getFlightDuration());
        assertEquals(route1.getPlaneTypesRaw(), route2.getPlaneTypesRaw());
        assertEquals(route1.getPrice(), route2.getPrice());
        assertEquals(route1.getTakeoffTimes().toString(), route2.getTakeoffTimes().toString());
    }

    @Before
    public void setup() throws SQLException, DataConstraintsException, URISyntaxException, IOException {
        testDBPath = Paths.get("./", "testDB.db");
        testDBUsed = false;

        Database.establishConnection();
        db = Database.databaseConnection;
        db.setAutoCommit(false);

        routeDC = RouteDataController.getSingleton();
        airlineDC = AirlineDataController.getSingleton();
        airportDC = AirportDataController.getSingleton();

        airport = new Airport("Untitled Airport", "Null City", "New Zealand", "999", "9999", 0.0, 0.0, 123, 3, 'N');
        airport2 = new Airport("DEST", "Null City", "New Zealand", "888", "8888", 0.0, 0.0, 123, 3, 'N');
        airline = new Airline("Untitled Airline", "UNTITLED", null, "999", "New Zealand");
        route = new Route(airline.getCode(), airport.getCode(), airport2.getCode(), new String[]{"777", "320"}, 123, true, 60, new ArrayList<>(Arrays.asList(360, 720, 1080)));
    }

    @After
    public void teardown() throws SQLException, IOException, URISyntaxException {
        if (testDBUsed) {
            Database.setDatabasePath();
            Files.delete(testDBPath);
        } else {
            Database.databaseConnection.rollback();
        }
    }

    @Test
    public void testAddToDatabase() throws SQLException, ConstraintsError {
        Route returnedRoute = routeDC.addToDatabase(route);
        checkRoutesEqual(route, returnedRoute);
    }

    @Test
    public void testGetRouteViaCodes() throws SQLException {
        routeDC.save(route);
        checkRoutesEqual(route, routeDC.getEntity(route.getSourceAirportCode(), route.getDestinationAirportCode(), route.getAirlineCode()));

        assertNull(routeDC.getEntity(route.getSourceAirportCode(), route.getDestinationAirportCode(), "ABC"));
    }

    @Test
    public void getNonexistentRouteFromCode() throws SQLException {
        Route got = routeDC.getEntity(route.getSourceAirportCode(), route.getDestinationAirportCode(), route.getAirlineCode());
        assertNull(got);
    }

    @Test
    public void getRouteWithNonexistentAirport() throws SQLException {
        Route got = routeDC.getEntity("BLAH", route.getDestinationAirportCode(), route.getAirlineCode());
        assertNull(got);
    }

    @Test
    public void getRouteWithNonexistentAirline() throws SQLException {
        Route got = routeDC.getEntity(route.getSourceAirportCode(), route.getDestinationAirportCode(), "BLAH");
        assertNull(got);
    }

    @Test
    public void getNonexistentRouteFromID() throws SQLException {
        Route got = routeDC.getEntity(-100);
        assertNull(got);
    }

    @Test
    public void testUpdateInDatabase() throws SQLException, ConstraintsError, DataConstraintsException {
        Route returnedRoute = routeDC.addToDatabase(route);
        returnedRoute.setSourceAirportCode("NULL");
        returnedRoute.setTakeoffTimes(new ArrayList<>(Collections.emptyList()));
        returnedRoute.setFlightDuration(100);
        returnedRoute.setPrice(12345);

        routeDC.updateInDatabase(returnedRoute);

        checkRoutesEqual(returnedRoute, routeDC.getEntity(returnedRoute.getId()));
    }

    @Test
    public void testLotsOfTakeoffs() throws SQLException, DataConstraintsException, ConstraintsError {
        ArrayList<Integer> times = new ArrayList<>();
        int maxNumMilliseconds = 1000; // With 24 * 60 takeoffs, shouldn't slow down a ridiculous amount

        for (int i = 1; i < 24 * 60; i += 15) {    // Minimum gap is 15 minutes so this should be the max number of flights per day
            times.add(i);
        }

        route.setTakeoffTimes(times);

        long start = System.nanoTime();
        Route returned = routeDC.save(route);
        long delta = System.nanoTime() - start;

        assertTrue(String.format("Time to save and get: %d ms", delta / 1000000), delta / 1000000 < maxNumMilliseconds);
    }


    @Test
    public void testBatchGetTakeoffTimes() throws SQLException {
        ArrayList<Route> routes = routeDC.getSortedFilteredEntities(null, null, 100, 0);
        // getSortedFilteredEntities uses batch takeoff times. getEntity doesn't
        for (Route route : routes) {
            checkRoutesEqual(route, routeDC.getEntity(route.getId()));
        }
    }

    @Test
    public void testAddBatchSingle() throws SQLException, DataConstraintsException {
        routeDC.addToBatch(route);

        routeDC.executeBatch(true);
        routeDC.batchGetTakeoffTimes(Collections.singletonList(route));

        checkRoutesEqual(routeDC.getEntity(route.getSourceAirportCode(), route.getDestinationAirportCode(), route.getAirlineCode()), route);
    }

    @Test
    public void testAddBatchMultiple() throws SQLException, DataConstraintsException {
        Route route1 = new Route("CI", "ABC", "YPY", new String[]{"777", "320"}, 123, true, 60, new ArrayList<>(Arrays.asList(360, 720, 1080)));
        Route route2 = new Route("YE", "ZYYJ", "CYDQ", new String[]{"777"}, 34, true, 324, new ArrayList<>(Arrays.asList(15, 30, 45)));
        Route route3 = new Route("YC", "ZYSQ", "DEF", new String[]{"787", "H0D"}, 8764, true, 45, new ArrayList<>(Arrays.asList(360, 1080)));
        Route route4 = new Route("Y8", "ZYDQ", "CYEV", new String[]{"767", "320"}, 87, true, 24, new ArrayList<>(Collections.singletonList(1080)));
        Route route5 = new Route("CI", "BIHN", "CYND", new String[]{"A38"}, 4, true, 65, new ArrayList<>(Arrays.asList(100, 200, 300)));

        routeDC.addToBatch(route1);
        routeDC.addToBatch(route2);
        routeDC.addToBatch(route3);
        routeDC.addToBatch(route4);
        routeDC.addToBatch(route5);

        routeDC.executeBatch(true);
        routeDC.batchGetTakeoffTimes(Arrays.asList(route1, route2, route3, route4, route5));

        checkRoutesEqual(routeDC.getEntity(route1.getSourceAirportCode(), route1.getDestinationAirportCode(), route1.getAirlineCode()), route1);
        checkRoutesEqual(routeDC.getEntity(route2.getSourceAirportCode(), route2.getDestinationAirportCode(), route2.getAirlineCode()), route2);
        checkRoutesEqual(routeDC.getEntity(route3.getSourceAirportCode(), route3.getDestinationAirportCode(), route3.getAirlineCode()), route3);
        checkRoutesEqual(routeDC.getEntity(route4.getSourceAirportCode(), route4.getDestinationAirportCode(), route4.getAirlineCode()), route4);
        checkRoutesEqual(routeDC.getEntity(route5.getSourceAirportCode(), route5.getDestinationAirportCode(), route5.getAirlineCode()), route5);
    }

    @Test
    public void testRouteSanityCheckNoSourceAirport() throws DataConstraintsException, SQLException {
        airportDC.save(airport2);
        airlineDC.save(airline);
        assertTrue(routeDC.routeSanityCheck(route).toLowerCase().contains("origin airport"));
    }

    @Test
    public void testRouteSanityCheckNoDestinationAirport() throws DataConstraintsException, SQLException {
        airportDC.save(airport);
        airlineDC.save(airline);
        assertTrue(routeDC.routeSanityCheck(route).toLowerCase().contains("destination airport"));
    }

    @Test
    public void testRouteSanityCheckNoAirline() throws DataConstraintsException, SQLException {
        airportDC.save(airport);
        airportDC.save(airport2);
        assertTrue(routeDC.routeSanityCheck(route).toLowerCase().contains("airline"));
    }

    @Test
    public void testRouteSanityCheckSourceDestinationSame() throws DataConstraintsException, SQLException {
        airportDC.save(airport);
        airportDC.save(airport2);
        airlineDC.save(airline);
        route.setSourceAirportCode(airport.getIcao());
        route.setDestinationAirportCode(airport.getIata());
        assertTrue(routeDC.routeSanityCheck(route).toLowerCase().contains("same"));
    }

    @Test
    public void testRouteSanityCheckNoIssue() throws DataConstraintsException, SQLException {
        airportDC.save(airport);
        airportDC.save(airport2);
        airlineDC.save(airline);
        assertNull(routeDC.routeSanityCheck(route));
    }

    @Test
    public void testDeleteFromDatabase() throws SQLException {
        DummyObserver<Route> dummy = new DummyObserver<>();
        routeDC.addObserver(DataController.OBSERVE_ALL, dummy);

        Route returned = routeDC.save(route);
        assertNotNull(routeDC.getEntity(returned.getId()));
        routeDC.deleteFromDatabase(returned.getId());
        assertNull(routeDC.getEntity(returned.getId()));
    }

    @Test
    public void testDeleteNonexistentFromDatabase() throws SQLException {
        DummyObserver<Route> dummy = new DummyObserver<>();
        routeDC.addObserver(DataController.OBSERVE_ALL, dummy);

        routeDC.deleteFromDatabase(42069);
        assertNull(routeDC.getEntity(42069));

        assertEquals(dummy.dataDeleted.size(), 0);
    }



    @Test
    public void testAutoGenerateValuesForAllRoutesWithZeroPrice() throws DataConstraintsException, SQLException, URISyntaxException, IOException {
        Database.setDatabasePath(testDBPath.toUri());
        testDBUsed = true;

        Route route9999to9998 = new Route("ABC", "9999", "9998", new String[]{}, 0, false, 0, new ArrayList<>());
        Route route9998to9997 = new Route("ABC", "9998", "9997", new String[]{}, 0, false, 0, new ArrayList<>());

        route9999to9998 = routeDC.save(route9999to9998);
        route9998to9997 = routeDC.save(route9998to9997);

        assertEquals(0, route9999to9998.getPrice());
        assertEquals(0, route9998to9997.getPrice());

        Airport airport9999 = new Airport("test 1", "asdf", "New Zealand", null, "9999", 10, 20, 10, 5.5, 'N');
        Airport airport9998 = new Airport("test 1", "asdf", "New Zealand", null, "9998", 10, 30, 10, 5.5, 'N');

        airport9999 = airportDC.save(airport9999);
        airport9998 = airportDC.save(airport9998);

        routeDC.autoGenerateValuesForAllRoutesWithPriceZero();

        route9999to9998 = routeDC.getEntity(route9999to9998.getId());
        route9998to9997 = routeDC.getEntity(route9998to9997.getId());

        assertEquals(0, route9998to9997.getPrice());
        assertNotEquals(0, route9999to9998.getPrice());

        assertEquals(0, route9998to9997.getFlightDuration());
        assertNotEquals(0, route9999to9998.getFlightDuration());

        assertEquals(0, route9998to9997.getTakeoffTimes().size());
        assertNotEquals(0, route9999to9998.getTakeoffTimes().size());

        Database.setDatabasePath();

    }
}