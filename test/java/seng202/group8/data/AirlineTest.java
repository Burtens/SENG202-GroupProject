package seng202.group8.data;

import org.junit.Before;
import org.junit.Test;
import seng202.group8.datacontroller.DataConstraintsException;

import static org.junit.Assert.*;

public class AirlineTest {

    public Airline testAirline;

    @Before
    public void setup() {
        this.testAirline = new Airline(10, "Test Airline", "Test Callsign", "", "", "New Zealand");
    }

    @Test
    public void testSetName() throws DataConstraintsException {
        // Length less than 3 characters
        assertThrows(DataConstraintsException.class, () -> testAirline.setName("NE"));
        assertThrows(DataConstraintsException.class, () -> testAirline.setName(null));

        testAirline.setName("Air New Zealand");

        assertEquals("Air New Zealand", testAirline.getName());
    }

    @Test
    public void testSetCallsign() throws DataConstraintsException {
        testAirline.setCallsign("  ANZ ");
        assertEquals("ANZ", testAirline.getCallsign());

        testAirline.setCallsign(null);
        assertNull(testAirline.getCallsign());
    }

    @Test
    public void testSetIata() throws DataConstraintsException {
        testAirline.setIcao(null);

        for (String invalid : new String[]{null, "", "A", "!@$", "-12", "ba5", " ", " aFZ"}) {
            // ICAO currently null, so can't set IATA to null
            assertThrows(DataConstraintsException.class, () -> testAirline.setIata(invalid));
        }

        for (String valid : new String[]{"AB", "DE", "  1g   ", "63"}) {
            testAirline.setIata(valid);
            assertEquals(valid.trim().toUpperCase(), testAirline.getIata());
        }

        testAirline.setIcao("ABC"); // Once ICAO is set, IATA can be null

        testAirline.setIata("");
        assertNull(testAirline.getIata());
        testAirline.setIata(null);
        assertNull(testAirline.getIata());
    }

    @Test
    public void testSetIcao() throws DataConstraintsException {
        testAirline.setIata(null);

        for (String invalid : new String[]{null, "", "A", "!@$", "-12", "ba", " ", " aFSB"}) {
            // IATA currently null, so can't set ICAO to null
            assertThrows(DataConstraintsException.class, () -> testAirline.setIcao(invalid));
        }

        for (String valid : new String[]{"ABC", "DEF", "  1g4   ", "A63"}) {
            testAirline.setIcao(valid);
            assertEquals(valid.trim().toUpperCase(), testAirline.getIcao());
        }

        testAirline.setIata("AB"); // Once IATA is set, ICAO can be null

        testAirline.setIcao("");
        assertNull(testAirline.getIcao());
        testAirline.setIcao(null);
        assertNull(testAirline.getIcao());
    }

    @Test
    public void testSetIataIcao() throws DataConstraintsException {
        for (String[] invalid : new String[][]{
                new String[]{null, null},
                new String[]{"AB", "ABCD"},
                new String[]{"ABC", "ABC"},
                new String[]{null, "ABCD"}
        }) {
            assertThrows(DataConstraintsException.class, () -> testAirline.setIataIcao(invalid[0], invalid[1]));
        }

        for (String[] invalid : new String[][]{
                new String[]{null, "ABC"},
                new String[]{"DE", null}
        }) {
            testAirline.setIataIcao(invalid[0], invalid[1]);
            assertEquals(invalid[0], testAirline.getIata());
            assertEquals(invalid[1], testAirline.getIcao());
        }
    }

    @Test
    public void testSetCountry() throws DataConstraintsException {
        testAirline.setCountry("New Zealand");
        assertEquals("New Zealand", testAirline.getCountry());
    }

    @Test
    public void testSetCountryInvalid() {
        assertThrows(DataConstraintsException.class, () -> testAirline.setCountry("  "));
        assertThrows(DataConstraintsException.class, () -> testAirline.setCountry(null));
    }

    @Test
    public void testGetCode() throws DataConstraintsException {
        testAirline.setIata("AB");
        testAirline.setIcao("ABC");

        assertEquals("AB", testAirline.getCode()); // Default IATA, fallback ICAO

        testAirline.setIata(null);
        assertEquals("ABC", testAirline.getCode()); // Fallback
    }
}