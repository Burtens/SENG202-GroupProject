package seng202.group8.datacontroller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import seng202.group8.data.*;
import seng202.group8.io.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TripDataControllerTest {

    public Connection db;
    public TripDataController tripDC;
    public AirportDataController airportDC;
    public RouteDataController routeDC;
    public Trip trip;
    public Trip savedTrip;
    public DummyCurrentTripObserver dummyObserver;

    public class DummyCurrentTripObserver implements TripDataController.CurrentTripObserver {
        Trip trip = null;
        int numNotifications = 0;
        @Override
        public void currentTripChange(Trip trip) {
            this.trip = trip;
            numNotifications++;
        }
    }


    public void testEquals(Trip trip1, Trip trip2) {
        assertEquals(trip1.getName(), trip2.getName());
        assertEquals(trip1.getComment(), trip2.getComment());

        assertEquals(trip1.getFlights().size(), trip2.getFlights().size());
        for (int i = 0; i < trip1.getFlights().size(); i++) {
            // Should be sorted already
            //System.out.printf("Time 1: %d\n", trip1.getFlights().get(i).getTakeoffTime());
            //System.out.printf("Time 2: %d\n\n", trip2.getFlights().get(i).getTakeoffTime());
            testEquals(trip1.getFlights().get(i), trip2.getFlights().get(i));
        }
    }

    public void testEquals(TripFlight flight1, TripFlight flight2) {
        assertEquals(flight1.getComment(), flight2.getComment());
        assertEquals(flight1.getAirlineCode(), flight2.getAirlineCode());
        assertEquals(flight1.getSourceCode(), flight2.getSourceCode());
        assertEquals(flight1.getDestinationCode(), flight2.getDestinationCode());

        assertEquals(flight1.getUTCTakeoffDateTime(), flight2.getUTCTakeoffDateTime());
    }


    @Before
    public void setup() throws SQLException, DataConstraintsException {
        Database.establishConnection();
        db = Database.databaseConnection;
        db.setAutoCommit(false);

        airportDC = AirportDataController.getSingleton();
        routeDC = RouteDataController.getSingleton();
        tripDC = TripDataController.getSingleton();

        dummyObserver = new DummyCurrentTripObserver();
        tripDC.setCurrentlyOpenTrip(null); // Reset back to known state

        Airport airport0000 = airportDC.save(new Airport("0000 Airport", "CITY", "Fiji", null, "0000", 0.0, 0.0, 0, 0, 'N'));
        Airport airport0001 = airportDC.save(new Airport("0001 Airport", "CITY", "Fiji", null, "0001", 2.0, 0.0, 0, 0, 'N'));
        Airport airport0002 = airportDC.save(new Airport("0002 Airport", "CITY", "Fiji", null, "0002", 4.0, 0.0, 0, 0, 'N'));
        Airport airport0003 = airportDC.save(new Airport("0003 Airport", "CITY", "Japan", null, "0003", 20.0, 0.0, 0, 0, 'N'));
        Route route00t01 = routeDC.save(new Route("00", "0000", "0001", new String[]{"777", "320"}, 200, false, 100, Arrays.asList(0, 6 * 60, 12 * 60, 15 * 60 + 40, 18 * 60)));
        Route route01t02 = routeDC.save(new Route("00", "0001", "0002", new String[]{"777", "320"}, 200, false, 120, Arrays.asList(6 * 60, 12 * 60, 18 * 60)));
        Route route02t03 = routeDC.save(new Route("00", "0002", "0003", new String[]{"777", "320"}, 400, false, 200, Arrays.asList(7 * 60, 8 * 60, 12 * 60, 16 * 60, 19 * 60, 20 * 60)));
        Route route03t00 = routeDC.save(new Route("00", "0003", "0000", new String[]{"777", "320"}, 400, false, 240, Arrays.asList(8 * 60, 11 * 60 + 30, 12 * 60, 16 * 60, 19 * 60, 20 * 60)));

        Route routeBadRoute = routeDC.save(new Route("00", "0011", "0000", new String[]{"777", "320"}, 400, false, 240, Arrays.asList(8 * 60, 11 * 6 + 30, 12 * 60, 16 * 60, 19 * 60, 20 * 60)));

        trip = new Trip("Test Trip Name", "Test Comment");
        trip.addFlight(new TripFlight("0001", "0002", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 22, 18, 0), "Test Comment for flight 2"));
        trip.addFlight(new TripFlight("0000", "0001", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 21, 12, 0), "Test Comment for flight 1"));
        // 6 hours delta, first flight 1:40. Flight 1 dest == flight 2 source. Should be no issues
        trip.addFlight(new TripFlight("0002", "0003", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 22, 19, 0), "Test comment for flight 3"));
        // 1 hour delta, second flight 2 hours. Error with takeoff before landing

        trip.addFlight(new TripFlight("0002", "0003", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 23, 8, 0), "Test comment for flight 4"));
        // Reset after error. 3:40 flight. Lands 11:40

        trip.addFlight(new TripFlight("0000", "0001", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 23, 18, 0), "Test comment for flight 5"));
        // Should warn from being too far away to drive

        trip.addFlight(new TripFlight("0001", "0002", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 23, 19, 1), "Test Comment for flight 6"));
        // Invalid takeoff time

        trip.addFlight(new TripFlight("0002", "0003", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 24, 7, 0), "Test comment for flight 7"));
        // Reset after invalid takeoff time. 3:40 duration lands 10:40

        trip.addFlight(new TripFlight("0003", "0000", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 24, 11, 30), "Test comment for flight 8"));
        // Warning: international flight, less than 2 hours. 4 hours flight, land 15:30

        trip.addFlight(new TripFlight("0000", "0001", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 24, 15, 40), "Test Comment for flight 9"));
        // Warning: domestic flight, less than 30 minutes layover. 1:40 flight, land 17:20

        trip.addFlight(new TripFlight("0011", "0001", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 24, 15, 41), "Test Comment for flight 10"));
        // Error: route doesn't exist

        trip.addFlight(new TripFlight("0011", "0000", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 24, 15, 42), "Test Comment for flight 11"));
        // Error: source airport doesn't exist

        savedTrip = tripDC.save(new Trip("Saved Test Trip", "comment"));
    }

    @After
    public void teardown() throws SQLException {
        Database.databaseConnection.rollback();
    }

    @Test
    public void testSave() throws SQLException {
        Trip returned = tripDC.save(trip);

        Trip.sortFlightsByTakeoffTime(trip.getFlights());
        testEquals(trip, returned);
        assertTrue(returned != trip);
    }

    @Test
    public void testModify() throws SQLException, DataConstraintsException {
        Trip returned = tripDC.save(trip);

        returned.setName("New Name");
        tripDC.save(returned);
        testEquals(returned, tripDC.getEntity(returned.getId()));
    }

    @Test
    public void testTripSanityCheck() throws SQLException {
        List<TripDataController.WarningError> errors = tripDC.tripSanityCheck(trip);

        for (int i : new int[]{0, 1, 3, 6}) {
            assertNull(errors.get(i));
        }

        assertTrue(errors.get(2).message.toLowerCase().contains("before you land")); // takeoff before landing
        assertTrue(errors.get(4).message.toLowerCase().contains("km away")); // prev dst/curr src too far away, too little time
        assertTrue(errors.get(5).message.toLowerCase().contains("takeoff time")); // invalid takeoff time
        assertTrue(errors.get(7).message.toLowerCase().contains("international flight")); // short layover
        assertTrue(errors.get(8).message.contains("domestic flight")); // too far, too little time
        assertTrue(errors.get(9).message.contains("route")); // route doesn't exist
        assertTrue(errors.get(10).message.contains("origin airport")); // src airport doesn't exist
    }

    public int minutes(int hours, int minutes) {
        return hours * 60 + minutes;
    }

    @Test
    public void testCanAddFlightWithoutClash() throws DataConstraintsException, SQLException {
        ArrayList<TripFlight> flights = new ArrayList<>();
        flights.add(new TripFlight("0000", "0001", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 21, 12, 0), "Test Comment for flight 1"));
        // Flight 1: 1:40 long. 1200 to 1340
        flights.add(new TripFlight("0001", "0002", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 21, 18, 0), "Test Comment for flight 2"));
        // Flight 2: 2:00 long. 1800 to 2000

        TripFlight flight = new TripFlight("0000", "0001", "00", DateTimeHelpers.generateUTCDateTime(2020, 2, 21, 14, 0), "Comment for new flight");
        assertNull(tripDC.canAddFlightWithoutClash(flights, flight));

        for (int time : new int[]{
                minutes(12, 10),
                minutes(11, 10),
                minutes(16, 40),
                minutes(17, 30),
                minutes(12, 0) // last one tests if the flight is identical
        }) {
            flight.setTakeoffTime(time);
            assertNotNull(tripDC.canAddFlightWithoutClash(flights, flight));
        }
    }


    @Test
    public void getExistingTripFromID() throws SQLException {
        Trip returned = tripDC.addToDatabase(trip);
        Trip got = tripDC.getEntity(returned.getId());
        testEquals(returned, got);
    }

    @Test
    public void getNonexistentTripFromID() throws SQLException {
        Trip got = tripDC.getEntity(42069);
        assertNull(got);
    }

    @Test
    public void testGetPrice() throws SQLException {
        tripDC.addToDatabase(trip);
        assertEquals(tripDC.getPrice(trip.getFlights().get(0)), 200); // Flight 1
        assertEquals(tripDC.getPrice(trip.getFlights().get(1)), 200); // Flight 2
        assertEquals(tripDC.getPrice(trip.getFlights().get(2)), 400); // Flight 3
    }

    @Test
    public void sortFlightsTest() throws DataConstraintsException {
        List<TripFlight> trips = new ArrayList<TripFlight>();
        trips.add(new TripFlight("ABC", "DEF", "MA", ZonedDateTime.of(2020, 2, 22, 16, 0, 0, 0, DateTimeHelpers.utcZone), "Test Comment for flight 2"));
        trips.add(new TripFlight("MAT", "TYG", "CD", ZonedDateTime.of(2020, 2, 21, 10, 0, 0, 0, DateTimeHelpers.utcZone), "Test Comment for flight 3"));
        trips.add(new TripFlight("MAT", "TYG", "CD", ZonedDateTime.of(2020, 2, 21, 12, 0, 0, 0, DateTimeHelpers.utcZone), "Test Comment for flight 1"));
        Trip.sortFlightsByTakeoffTime(trips);
//        for (TripFlight trip: trips) {
//           System.out.printf("%d\n",trip.getTakeoffTime());
//        }
        assert (trips.get(0).getTakeoffTime() < trips.get(1).getTakeoffTime());
        assert (trips.get(1).getTakeoffTime() < trips.get(2).getTakeoffTime());
    }

    @Test
    public void testDeleteFromDatabase() throws SQLException {
        DummyObserver dummy = new DummyObserver<Trip>();
        routeDC.addObserver(DataController.OBSERVE_ALL, dummy);

        Trip returned = tripDC.save(trip);
        assertNotNull(tripDC.getEntity(returned.getId()));
        tripDC.deleteFromDatabase(returned.getId());
        assertNull(tripDC.getEntity(returned.getId()));
    }

    @Test
    public void testDeleteNonexistentFromDatabase() throws SQLException {
        DummyObserver dummy = new DummyObserver<Trip>();
        tripDC.addObserver(DataController.OBSERVE_ALL, dummy);

        tripDC.deleteFromDatabase(42069);
        assertNull(tripDC.getEntity(42069));

        assertEquals(dummy.dataDeleted.size(), 0);
    }

    @Test
    public void testGetUTCLandingTime() throws SQLException, DataConstraintsException {
        Route route00t01 = routeDC.save(new Route("00", "0300", "F5A1", new String[]{"777", "320"}, 200, false, 100, Arrays.asList(0, 6 * 60, 12 * 60, 15 * 60 + 40, 18 * 60)));
        TripFlight flight = new TripFlight(route00t01.getSourceAirportCode(), route00t01.getDestinationAirportCode(), route00t01.getAirlineCode(), 0, LocalDate.now(), "");
        assertEquals(tripDC.getUTCLandingTime(flight), flight.getUTCTakeoffDateTime().plusMinutes(100));
    }

    @Test
    public void testGetUTCLandingTimeLongFlight() throws SQLException, DataConstraintsException {
        Route route00t01 = new Route("CI", "6F77", "9KDS", new String[]{"777", "320"}, 24 * 60 - 1, false, 24 * 60 - 1, Arrays.asList(0, 6 * 60, 12 * 60, 15 * 60 + 40, 18 * 60));
        route00t01 = routeDC.save(route00t01);
        TripFlight flight = new TripFlight(route00t01.getSourceAirportCode(), route00t01.getDestinationAirportCode(), route00t01.getAirlineCode(), 0, LocalDate.now(), "");
        assertEquals(tripDC.getUTCLandingTime(flight), flight.getUTCTakeoffDateTime().plusMinutes(24 * 60 - 1));
    }

    @Test
    public void testGetUTCLandingTimeNoRoute() throws SQLException, DataConstraintsException {
        TripFlight flight = new TripFlight("0269", "9854", "00", 0, LocalDate.now(), "");
        assertThrows(SQLException.class, () -> tripDC.getUTCLandingTime(flight));
    }

    @Test
    public void getEntityFromString() throws SQLException {
        testEquals(savedTrip, tripDC.getEntity("Saved Test Trip"));
    }

    @Test
    public void getAllTripNames() throws SQLException {
        assertTrue(tripDC.getAllTripNames().contains("Saved Test Trip"));
    }

    @Test
    public void testCurrentlySelectedTrip() throws SQLException {
        Trip curTrip = tripDC.save(trip);
        tripDC.setCurrentlyOpenTrip(curTrip);
        assertEquals(curTrip, tripDC.getCurrentlyOpenTrip());
    }

    @Test
    public void testCurrentlySelectedTripIsUpdatedWhenNameChanged() throws SQLException, DataConstraintsException {
        Trip curTrip = tripDC.save(trip);
        tripDC.setCurrentlyOpenTrip(curTrip);

        curTrip.setName("A different trip name");
        tripDC.save(curTrip);

        assertEquals("A different trip name", tripDC.getCurrentlyOpenTrip().getName());
    }

    @Test
    public void testCurrentlySelectedTripIsUpdatedWhenFlightsChanged() throws SQLException, DataConstraintsException {
        Trip curTrip = tripDC.save(trip);
        tripDC.setCurrentlyOpenTrip(curTrip);

        int numFlights = curTrip.getFlights().size();
        curTrip.addFlight(new TripFlight("0269", "9854", "00", 0, LocalDate.now(), ""));
        tripDC.save(curTrip);

        assertEquals(numFlights + 1, tripDC.getCurrentlyOpenTrip().getFlights().size());

        curTrip.getFlights().remove(0);
        tripDC.save(curTrip);
        assertEquals(numFlights, tripDC.getCurrentlyOpenTrip().getFlights().size());
    }

    @Test
    public void testCurrentTripObserverOnTripUpdates() throws SQLException {
        tripDC.subscribeToCurrentTrip(dummyObserver);
        trip = tripDC.save(trip);
        tripDC.setCurrentlyOpenTrip(trip);
        assertEquals(1, dummyObserver.numNotifications);

        trip.setComment("!");
        Trip returned = tripDC.save(trip);
        assertEquals(2, dummyObserver.numNotifications);
        testEquals(returned, dummyObserver.trip);

        tripDC.deleteFromDatabase(trip.getId());
        assertEquals(3, dummyObserver.numNotifications);
        assertNull(dummyObserver.trip);
    }

    @Test
    public void testCurrentTripObserversNotifiedOnSet() throws SQLException {
        tripDC.subscribeToCurrentTrip(dummyObserver);
        trip = tripDC.save(trip);

        tripDC.setCurrentlyOpenTrip(null);
        assertEquals(1, dummyObserver.numNotifications);
        assertNull(dummyObserver.trip);

        tripDC.setCurrentlyOpenTrip(trip);
        assertEquals(2, dummyObserver.numNotifications);
        testEquals(trip, dummyObserver.trip);
    }

    @Test
    public void testCurrentTripObserversUnsubscribe() throws SQLException {
        tripDC.subscribeToCurrentTrip(dummyObserver);
        trip = tripDC.save(trip);

        tripDC.setCurrentlyOpenTrip(null);
        assertEquals(1, dummyObserver.numNotifications);
        assertNull(dummyObserver.trip);

        tripDC.unsubscribeFromCurrentTrip(dummyObserver);

        trip.setComment("SDF SF ");
        tripDC.save(trip);
        assertEquals(1, dummyObserver.numNotifications);
        assertNull(dummyObserver.trip);
    }
}