package seng202.group8.datacontroller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import seng202.group8.data.Airline;
import seng202.group8.io.ConstraintsError;
import seng202.group8.io.Database;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.*;


public class AirlineDataControllerTest {
    public Connection db;
    public String country = "New Zealand";
    public AirlineDataController controller;
    public Airline airline;

    @Before
    public void setup() throws SQLException, IOException, URISyntaxException {
        Database.establishConnection();
        Database.setDatabasePath(); // Need this for some reason.
        db = Database.databaseConnection;
        db.setAutoCommit(false);

        controller = AirlineDataController.getSingleton();
        airline = new Airline(-1, "Test Airline 1", "TEST CALLSIGN 1", "ZZ", "ZZZ", country);
    }


    @After
    public void teardown() throws SQLException {
        db.rollback();
    }

    public void checkAirlinesEqual(Airline a, Airline b) {
        assertEquals(a.getName(), b.getName());
        assertEquals(a.getIata(), b.getIata());
        assertEquals(a.getIcao(), b.getIcao());
        assertEquals(a.getCallsign(), b.getCallsign());
        assertEquals(a.getCode(), b.getCode());
    }

    @Test
    public void testAddToDatabase() throws SQLException {
        Airline returned = controller.addToDatabase(airline);

        assertNotEquals(airline.getId(), returned.getId());

        assertEquals(airline.getName(), returned.getName());
        assertEquals(airline.getCallsign(), returned.getCallsign());
        assertEquals(airline.getIata(), returned.getIata());
        assertEquals(airline.getIcao(), returned.getIcao());
    }

    @Test
    public void testUpdateInDatabase() throws SQLException, DataConstraintsException {
        Airline returned = controller.addToDatabase(airline);
        returned.setName("Test Airline 2");
        returned.setCallsign("TEST CALLSIGN 2");
        returned.setIata(null);
        returned.setIcao("ZZZ");
        controller.updateInDatabase(returned);

        Airline dup = controller.getEntity(returned.getId());

        checkAirlinesEqual(returned, dup);
    }

    @Test
    public void testUpdateInDatabaseUniquenessViolated() throws SQLException, DataConstraintsException {
        Airline returned = controller.addToDatabase(airline);
        controller.addToDatabase(new Airline("Test Airline 2", "TEST CALLSIGN 2", "9Z", "Z9Z", country));

        returned.setIcao("Z9Z");
        assertThrows(ConstraintsError.class, () ->
                controller.updateInDatabase(returned));
        returned.setIcao("ZZZ");

        returned.setIata("9Z");
        assertThrows(ConstraintsError.class, () ->
                controller.updateInDatabase(returned));
        returned.setIata("ZZ");

        returned.setName("Test Airline 2");
        assertThrows(ConstraintsError.class, () ->
                controller.updateInDatabase(returned));
        returned.setName("Test Airline 1");

        returned.setCallsign("TEST CALLSIGN 2");
        assertThrows(ConstraintsError.class, () ->
                controller.updateInDatabase(returned));
        returned.setCallsign("TEST CALLSIGN 1");
    }

    @Test
    public void testAddDuplicatesToDatabase() throws SQLException {
        Airline airline = new Airline(-1, "Test Airline 1", "TEST CALLSIGN 1", "ZZ", "ZZZ", country);
        Airline airline2 = new Airline(-1, "Test Airline 1", "TEST CALLSIGN 2", null, "999", country); // Name
        Airline airline3 = new Airline(-1, "Test Airline 2", "TEST CALLSIGN 1", "99", null, country); // Callsign
        Airline airline4 = new Airline(-1, "Test Airline 2", null, "ZZ", null, country); // IATA
        Airline airline5 = new Airline(-1, "Test Airline 2", "TEST CALLSIGN 2", null, "ZZZ", country); // ICAO

        Airline returned = controller.addToDatabase(airline);
        assertThrows(ConstraintsError.class, () -> {
            controller.addToDatabase(airline);
        }); // exact duplicate
        assertThrows(ConstraintsError.class, () -> {
            controller.addToDatabase(airline2);
        }); // name
        assertThrows(ConstraintsError.class, () -> {
            controller.addToDatabase(airline3);
        }); // callsign
        assertThrows(ConstraintsError.class, () -> {
            controller.addToDatabase(airline4);
        }); // iata
        assertThrows(ConstraintsError.class, () -> {
            controller.addToDatabase(airline5);
        }); // icao
    }

    @Test
    public void testAddBatchSingle() throws SQLException {
        Airline airline1 = new Airline(-1, "Test Airline 1", "TEST CALLSIGN 1", "ZZ", "ZZZ", "New Zealand");

        controller.addToBatch(airline1);

        controller.executeBatch(true);

        checkAirlinesEqual(controller.getEntity("ZZZ"), airline1);
    }

    @Test
    public void testAddBatchMultiple() throws SQLException {
        Airline airline1 = new Airline(-1, "Test Airline 1", "TEST CALLSIGN 1", "ZZ", "ZZZ", "New Zealand");
        Airline airline2 = new Airline(-1, "Test Airline 2", "TEST CALLSIGN 2", null, "999", "Australia");
        Airline airline3 = new Airline(-1, "Test Airline 3", "TEST CALLSIGN 3", "ZC", "888", "Australia");
        Airline airline4 = new Airline(-1, "Test Airline 4", "TEST CALLSIGN 4", "ZD", null, "Russia");
        Airline airline5 = new Airline(-1, "Test Airline 5", "TEST CALLSIGN 5", "Y2", "9XY", "New Zealand");

        controller.addToBatch(airline3);
        controller.addToBatch(airline4);
        controller.addToBatch(airline5);
        controller.addToBatch(airline1);
        controller.addToBatch(airline2);

        controller.executeBatch(true);

        checkAirlinesEqual(controller.getEntity("ZZZ"), airline1);
        checkAirlinesEqual(controller.getEntity("999"), airline2);
        checkAirlinesEqual(controller.getEntity("888"), airline3);
        checkAirlinesEqual(controller.getEntity("ZD"), airline4);
        checkAirlinesEqual(controller.getEntity("9XY"), airline5);
    }

    @Test
    public void getExistingAirlineFromID() throws SQLException {
        Airline returned = controller.addToDatabase(airline);
        Airline got = controller.getEntity(returned.getId());
        checkAirlinesEqual(returned, got);
    }

    @Test
    public void getNonexistentAirlineFromID() throws SQLException {
        Airline got = controller.getEntity(42069);
        assertNull(got);
    }

    @Test
    public void getExistingAirlineFromName() throws SQLException {
        Airline returned = controller.addToDatabase(airline);
        Airline got = controller.getEntityByName(returned.getName());
        checkAirlinesEqual(returned, got);
    }

    @Test
    public void getNonexistentAirlineFromName() throws SQLException {
        Airline got = controller.getEntityByName("MATTY G AIR");
        assertNull(got);
    }

    @Test
    public void testGetEntity() throws SQLException {
        Airline returned = controller.save(airline);
        Airline icao = controller.getEntity(returned.getIcao());
        checkAirlinesEqual(returned, icao);

        Airline iata = controller.getEntity(returned.getIata());
        checkAirlinesEqual(returned, iata);
    }

    @Test
    public void testGetEntityNonExistent() throws SQLException {
        Airline icao = controller.getEntity(airline.getIcao());
        assertNull(icao);
    }

    @Test
    public void testDeleteFromDatabase() throws SQLException {
        DummyObserver dummy = new DummyObserver<Airline>();
        controller.addObserver(DataController.OBSERVE_ALL, dummy);

        Airline returned = controller.save(airline);
        assertNotNull(controller.getEntity(returned.getId()));
        controller.deleteFromDatabase(returned.getId());
        assertNull(controller.getEntity(returned.getId()));
    }

    @Test
    public void testDeleteNonexistentFromDatabase() throws SQLException {
        DummyObserver dummy = new DummyObserver<Airline>();
        controller.addObserver(DataController.OBSERVE_ALL, dummy);

        controller.deleteFromDatabase(42069);
        assertNull(controller.getEntity(42069));

        assertEquals(dummy.dataDeleted.size(), 0);
    }
/*
    @Test
    public void testGetEmptyAllEntities() throws SQLException {
        assertTrue(controller.getAllEntities().isEmpty());
    }

    @Test
    public void testGetAllEntities() throws SQLException {
        Airline airline  = new Airline(-1, "Test Airline 1", "TEST CALLSIGN 1", "ZZ", "ZZZ", country);
        Airline airline2 = new Airline(-1, "Test Airline 2", "TEST CALLSIGN 2", "1Z", null, country);
        airline = controller.save(airline);
        airline2 = controller.save(airline2);
        List<Airline> airlineList = controller.getAllEntities();
        checkAirlinesEqual(airlineList.get(0), airline);
        checkAirlinesEqual(airlineList.get(1), airline2);
    }*/
}