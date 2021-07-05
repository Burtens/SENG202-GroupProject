package seng202.group8.datacontroller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import seng202.group8.data.Airport;
import seng202.group8.data.Country;
import seng202.group8.data.Route;
import seng202.group8.io.ConstraintsError;
import seng202.group8.io.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class AirportDataControllerTest {
    public Connection db;
    public Country country;
    public AirportDataController controller;
    public Airport airport;
    public RouteDataController routeDC;

    @Before
    public void setup() throws SQLException, DataConstraintsException {
        Database.establishConnection();
        db = Database.databaseConnection;
        db.setAutoCommit(false);

        controller = AirportDataController.getSingleton();
        routeDC = RouteDataController.getSingleton();
        airport = new Airport("Matty G Airport", "Christchurch", "New Zealand", "999", "9999", 10, 10, 0, 0, 'Z');
    }

    @After
    public void teardown() throws SQLException {
        db.rollback();
    }

    public void checkAirportsEqual(Airport a, Airport b) {
        assertEquals(a.getIata(), b.getIata());
        assertEquals(a.getDst(), b.getDst());
        assertEquals(a.getIcao(), b.getIcao());
        assertEquals(a.getName(), b.getName());
        assertEquals(a.getAltitude(), b.getAltitude());
        assertEquals(a.getLatitude(), b.getLatitude(), 0.001);
        assertEquals(a.getLongitude(), b.getLongitude(), 0.001);
        assertEquals(a.getCity(), b.getCity());
        assertEquals(a.getCode(), b.getCode());
    }

    @Test
    public void getExistingAirportFromID() throws SQLException {
        Airport returned = controller.addToDatabase(airport);
        Airport got = controller.getEntity(returned.getId());
        checkAirportsEqual(returned, got);
    }

    @Test
    public void getNonexistentAirportFromID() throws SQLException {
        Airport got = controller.getEntity(42069);
        assertEquals(null, got);
    }

    @Test
    public void getExistingAirportFromCode() throws SQLException {
        Airport returned = controller.addToDatabase(airport);
        Airport got = controller.getEntity(returned.getCode());
        checkAirportsEqual(returned, got);
    }

    @Test
    public void getNonexistentAirportFromName() throws SQLException {
        Airport got = controller.getEntity("No name");
        assertEquals(null, got);
    }

    @Test
    public void addDuplicateToDatabase() throws SQLException, DataConstraintsException {
        Airport returned = controller.addToDatabase(airport);

        assertThrows(ConstraintsError.class, () -> {
            controller.addToDatabase(airport);
        }); // Duplicate, so fails
    }


    @Test
    public void testAddBatchSingle() throws SQLException {
        controller.addToBatch(airport);

        controller.executeBatch(true);

        checkAirportsEqual(controller.getEntity(airport.getCode()), airport);
    }

    @Test
    public void testAddBatchMultiple() throws SQLException, DataConstraintsException {
        Airport airport1 = new Airport("Matty G Airport", "Christchurch", "New Zealand", "999", "9999", 10, 10, 0, 0, 'Z');
        Airport airport2 = new Airport("Test Airport 1", "Sydney", "Australia", "9A9", "3GD7", 45, 24, 0, 0, 'Z');
        Airport airport3 = new Airport("Test Airport 2", "Auckland", "New Zealand", "AA2", "DSF3", -76, 76, 0, 0, 'Z');
        Airport airport4 = new Airport("Test Airport 3", "Pyongyang", "North Korea", "ZPF", "98DS", 65, 4, 0, 0, 'Z');
        Airport airport5 = new Airport("Test Airport 4", "Pyongyang", "North Korea", null, "7GD3", 65, 4, 0, 0, 'Z');


        controller.addToBatch(airport1);
        controller.addToBatch(airport2);
        controller.addToBatch(airport3);
        controller.addToBatch(airport4);
        controller.addToBatch(airport5);

        controller.executeBatch(true);

        checkAirportsEqual(controller.getEntity("9999"), airport1);
        checkAirportsEqual(controller.getEntity("3GD7"), airport2);
        checkAirportsEqual(controller.getEntity("DSF3"), airport3);
        checkAirportsEqual(controller.getEntity("98DS"), airport4);
        checkAirportsEqual(controller.getEntity("7GD3"), airport5);
    }


    @Test
    public void testUpdateInDatabaseUniquenessViolated() throws SQLException, DataConstraintsException {
        Airport returned = controller.addToDatabase(airport);
        controller.addToDatabase(new Airport("Patty I Airport", "Sydney", "Australia", "998", "9988", -50, 80, 0, 0, 'Z'));

        returned.setIcao("9988");
        assertThrows(ConstraintsError.class, () ->
                controller.updateInDatabase(returned));
        returned.setIcao("9999");

        returned.setIata("998");
        assertThrows(ConstraintsError.class, () ->
                controller.updateInDatabase(returned));
        returned.setIata("999");
    }

    @Test
    public void updateInDatabase() throws SQLException, DataConstraintsException {
        Airport returned = controller.addToDatabase(airport);

        returned.setName("CHANGED NAME");
        returned.setIata(null);
        returned.setIcao("NULL");
        controller.updateInDatabase(returned);

        Airport returned2 = controller.getEntity(returned.getId());
        checkAirportsEqual(returned, returned2);
    }

    @Test
    public void testDeleteFromDatabase() throws SQLException {
        DummyObserver dummy = new DummyObserver<Airport>();
        controller.addObserver(DataController.OBSERVE_ALL, dummy);

        Airport returned = controller.save(airport);
        assertNotNull(controller.getEntity(returned.getId()));
        controller.deleteFromDatabase(returned.getId());
        assertNull(controller.getEntity(returned.getId()));
    }

    @Test
    public void testDeleteNonexistentFromDatabase() throws SQLException {
        DummyObserver dummy = new DummyObserver<Airport>();
        controller.addObserver(DataController.OBSERVE_ALL, dummy);

        controller.deleteFromDatabase(42069);
        assertNull(controller.getEntity(42069));

        assertEquals(dummy.dataDeleted.size(), 0);
    }

    @Test
    public void testGetTotalRoutesZero() throws SQLException {
        Airport returned = controller.addToDatabase(airport);
        assertEquals(controller.getTotalRoutes(returned.getCode()), 0);
    }

    @Test
    public void testGetTotalRoutesSingle() throws SQLException, DataConstraintsException {
        Airport anAirport = controller.save(airport);
        Airport otherAirport = controller.save(new Airport("Patty I Airport", "Sydney", "Australia", "998", "9988", -50, 80, 0, 0, 'Z'));

        routeDC.addToDatabase(new Route("AB", "999", "998", new String[]{"BLA"}, 100, false, 200, new ArrayList<Integer>()));

        assertEquals(1, controller.getTotalRoutes(anAirport.getCode()));
        assertEquals(0, controller.getTotalRoutes(otherAirport.getCode()));
    }

    @Test
    public void testGetTotalRoutesMultiple() throws SQLException, DataConstraintsException {
        Airport anAirport = controller.save(airport);

        routeDC.addToDatabase(new Route("AB", "999", "998", new String[]{"BLA"}, 100, false, 200, new ArrayList<Integer>()));
        routeDC.addToDatabase(new Route("AB", "999", "997", new String[]{"BLA"}, 100, false, 200, new ArrayList<Integer>()));

        assertEquals(controller.getTotalRoutes(anAirport.getCode()), 2);
    }

}
