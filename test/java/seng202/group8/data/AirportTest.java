package seng202.group8.data;

import org.junit.Test;
import seng202.group8.datacontroller.DataConstraintsException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;


public class AirportTest {
    @Test
    public void testTestSetName() throws DataConstraintsException {
        Airport testAirport1 = new Airport(-1, "", "", null, "", "", 0,
                0, 0, 0, 'Z');

        // Length less than 3 characters
        assertThrows(DataConstraintsException.class, () -> testAirport1.setName("NE"));

        testAirport1.setName("Christchurch");

        assertEquals("Christchurch", testAirport1.getName()); //Should return the name and be true
    }

    @Test
    public void testSetCountry() throws DataConstraintsException {
        Airport testAirport1 = new Airport(-1, "", "", null, "", "", 0,
                0, 0, 0, 'Z');

        testAirport1.setCountry("New Zealand");

        assertEquals("New Zealand", testAirport1.getCountry()); //Should return the same country object newZealand

        assertThrows(DataConstraintsException.class, () -> testAirport1.setCountry("  "));
    }

    @Test
    public void testSetCity() throws DataConstraintsException {
        Airport testAirport1 = new Airport(-1, "", "", null, "", "", 0,
                0, 0, 0, 'Z');

        testAirport1.setCity("Lego City");

        assertEquals("Lego City", testAirport1.getCity());

        assertThrows(DataConstraintsException.class, () -> testAirport1.setCity("  "));
        assertThrows(DataConstraintsException.class, () -> testAirport1.setCity(null));
    }

    @Test
    public void testSetIata() throws DataConstraintsException {
        Airport testAirport1 = new Airport(-1, "", "", null, "", "", 0,
                0, 0, 0, 'Z');
        // IATA can be set to null
        testAirport1.setIata(null);
        testAirport1.setIata("");

        testAirport1.setIata("CHC");
        // IATA cannot be less than 3 characters long
        assertThrows(DataConstraintsException.class, () -> testAirport1.setIata("ny"));

        assertEquals("CHC", testAirport1.getIata()); //Should return the same IATA code "CHC"
    }

    @Test
    public void testSetIcao() throws DataConstraintsException {
        Airport testAirport1 = new Airport(-1, "", "", null, "", "", 0,
                0, 0, 0, 'Z');
        testAirport1.setIcao("CHCH");
        // ICAO cannot be less than 3 characters long
        assertThrows(DataConstraintsException.class, () -> testAirport1.setIcao("nyk"));

        // ICAO cannot be null
        assertThrows(DataConstraintsException.class, () -> testAirport1.setIcao(""));
        assertThrows(DataConstraintsException.class, () -> testAirport1.setIcao(null));
        assertEquals("CHCH", testAirport1.getIcao()); //Should return the same ICAO code "CHCH"
    }

    @Test
    public void testSetLatitude() throws DataConstraintsException {
        Airport testAirport1 = new Airport(-1, "", "", null, "", "", 0,
                0, 0, 0, 'Z');
        // Latitude cannot be larger than 90 or less than -90
        assertThrows(DataConstraintsException.class, () -> testAirport1.setLatitude(-91.786961));
        assertThrows(DataConstraintsException.class, () -> testAirport1.setLatitude(95.846364));

        testAirport1.setLatitude(-6.081689);

        assertEquals(-6.081689, testAirport1.getLatitude(), 0.00001); //Should return the same latitude -6.081689
    }

    @Test
    public void testSetLongitude() throws DataConstraintsException {
        Airport testAirport1 = new Airport(-1, "", "", null, "", "", 0,
                0, 0, 0, 'Z');
        // Longitude cannot be larger than 180 or less than -180
        assertThrows(DataConstraintsException.class, () -> testAirport1.setLongitude(187.467812));
        assertThrows(DataConstraintsException.class, () -> testAirport1.setLongitude(-194.986312));

        testAirport1.setLongitude(64.190922);

        assertEquals(64.190922, testAirport1.getLongitude(), 0.00001); //Should return the same longitude 64.190922
    }

    @Test
    public void testSetAltitude() throws DataConstraintsException {
        Airport testAirport1 = new Airport(-1, "", "", null, "", "", 0,
                0, 0, 0, 'Z');
        // Altitude cannot be less than -1240 ft
        assertThrows(DataConstraintsException.class, () -> testAirport1.setAltitude(-2000));

        testAirport1.setAltitude(357);

        assertEquals(357, testAirport1.getAltitude()); //Should return the same altitude of 357
    }

    @Test
    public void testSetTimezone() throws DataConstraintsException {
        Airport testAirport1 = new Airport(-1, "", "", null, "", "", 0,
                0, 0, 0, 'Z');
        // Timezone cannot be greater than 14 or less than -12
        assertThrows(DataConstraintsException.class, () -> testAirport1.setTimezone(15));
        assertThrows(DataConstraintsException.class, () -> testAirport1.setTimezone(-13));

        testAirport1.setTimezone(8);

        assertEquals(8.0, testAirport1.getTimezone(), 0.0001);
    }

    @Test
    public void testSetDst() throws DataConstraintsException {
        //DST must be one of these given character (one of 'E', 'A', 'S', "O', 'Z' and 'N')
        Airport testAirport1 = new Airport(-1, "", "", null, "", "", 0,
                0, 0, 0, 'Z');
        Airport testAirport2 = new Airport(-1, "", "", null, "", "", 0,
                0, 0, 0, 'Z');

        testAirport1.setDst('Z');
        testAirport2.setDst('E');

        assertEquals(DSTType.NEW_ZEALAND, testAirport1.getDst());
        assertEquals(DSTType.EUROPE, testAirport2.getDst());
    }
}


