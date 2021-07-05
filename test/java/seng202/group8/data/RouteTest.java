package seng202.group8.data;

import org.junit.Before;
import org.junit.Test;
import seng202.group8.datacontroller.DataConstraintsException;
import seng202.group8.io.Database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class RouteTest {
    Route route;
    Airline airline;

    @Before
    public void setup() throws DataConstraintsException, SQLException {
        Database.establishConnection();
        route = new Route(
                "IA",
                "SRC",
                "DST",
                new String[]{"747", "320"},
                100,
                true,
                100,
                new ArrayList<>()
        );
    }

    /**
     * Converts an array of integers into an arraylist
     *
     * @param integers array of integers
     * @return arraylist in the same order
     */
    protected ArrayList<Integer> toIntArrayList(Integer[] integers) {
        ArrayList<Integer> intArrList = new ArrayList<Integer>();
        for (Integer num : integers) {
            intArrList.add(num);
        }
        return intArrList;
    }

    @Test
    public void testSetTakeoffTimesSet() throws DataConstraintsException {
        List<Integer> values = toIntArrayList(new Integer[]{100, 500, 300, 800});
        route.setTakeoffTimes(values);

        List<Integer> returned = route.getTakeoffTimes();
        Collections.sort(values);

        assertArrayEquals(route.toIntArray(values), route.toIntArray(returned));
    }

    @Test
    public void testSetTakeoffTimesSetInvalid() throws DataConstraintsException {
        Integer[][] testCases = new Integer[][]{
                new Integer[]{100, 200, 500, 13451, 534},
                null,
                new Integer[]{-100, 234, 23},
                new Integer[]{null, 134},
                new Integer[]{100, 300, 400, 100}
        };

        for (Integer[] testCase : testCases) {
            assertThrows(DataConstraintsException.class, () -> {
                if (testCase != null) {
                    route.setTakeoffTimes(toIntArrayList(testCase));
                } else {
                    route.setTakeoffTimes(null);
                }

            });
        }
    }

    @Test
    public void testCheckTakeoffTimesValid() {
        List<Integer> test = toIntArrayList(new Integer[]{-10, -1, 0, 100, 100, 40, 24 * 60 - 1, 24 * 60, 40, 24 * 60 + 1, 100, null});
        //                                                rng, rng, rng, ok, dup, ok, rng        , rng    , dup, rng       , dup, null
        //                                                0    1    2    3   4    5   ok           7        8     9          10, 11
        List<String> result = Route.checkTakeoffTimesValid(test);
        for (int i : new int[]{0, 1, 7, 9}) {
            assertTrue(result.get(i).toLowerCase().contains("between"));
        }

        for (int i : new int[]{2, 3, 5, 6}) {
            assertNull(result.get(i));
        }

        for (int i : new int[]{4, 8, 10}) {
            assertTrue(result.get(i).toLowerCase().contains(("duplicate")));
        }

        for (int i : new int[]{11}) {
            assertTrue(result.get(i).contains("null"));
        }
    }

    @Test
    public void testCheckTakeoffTimesAllValid() {
        assertNull(Route.checkTakeoffTimesValid(toIntArrayList(new Integer[]{100, 120, 150, 1})));
    }

    @Test
    public void testSetAirline() throws DataConstraintsException {
//        throw new Error("Need to redo this test (once constructor is changed and airline becomes a String property)");
        // Fail on null

        String[] valid = new String[]{"ABC", "AB", "12", "  BET", "  14 "};

        for (String test : valid) {
            route.setAirlineCode(test);
            assertEquals(test.toUpperCase().trim(), route.getAirlineCode());
        }

        String[] invalid = new String[]{null, "ABSF", "#W%", "A!", "A"};
        for (String test : invalid) {
            assertThrows(DataConstraintsException.class, () -> {
                route.setAirlineCode(test);
            });
        }
    }

    @Test
    public void testSetSourceAndDestinationAirportCode() throws DataConstraintsException {
        // Source and destination will have the same behaviour

        String[] invalidCodes = new String[]{null, "", "!A", "2B", "FK_"};
        for (String code : invalidCodes) {
            assertThrows(DataConstraintsException.class, () -> {
                route.setSourceAirportCode(code);
            });
            assertThrows(DataConstraintsException.class, () -> {
                route.setDestinationAirportCode(code);
            });
        }

        String[] validCodes = new String[]{"JFK", "AKL", "CHCH", "AB2", "  asb  ", "           1121  "};

        // Assumes can set source and destination to the same
        for (String code : validCodes) {
            route.setSourceAirportCode(code);
            route.setDestinationAirportCode(code);

            String formatted = code.strip().toUpperCase();
            assertEquals(formatted, route.getSourceAirportCode());
            assertEquals(formatted, route.getDestinationAirportCode());
        }
    }

    @Test
    public void testSetPlaneTypes() throws DataConstraintsException {
        String[] planes = new String[]{"777", "AB2", "380"};
        route.setPlaneTypes(planes);
        assertArrayEquals(planes, route.getPlaneTypes());
    }

    @Test
    public void testSetPlaneTypesInvalid() throws DataConstraintsException {
        ArrayList<String> planes = new ArrayList<>();
        planes.add("350");
        planes.add("280");

        String[] invalidValues = new String[]{"    ", null, "!$#@", "SSSSSSSSS", "SD", "B"};
        for (String val : invalidValues) {
            planes.add(val);
            assertThrows(DataConstraintsException.class, () -> {
                route.setPlaneTypes(planes.toArray(new String[planes.size()]));
            });
            planes.remove(planes.size() - 2); // Remove the last element again
        }
    }

    @Test
    public void testSetPlaneTypesNull() throws DataConstraintsException {
        route.setPlaneTypes(new String[]{});
        assertEquals(0, route.getPlaneTypes().length); // empty

        route.setPlaneTypes((String) null);
        assertEquals(0, route.getPlaneTypes().length); // empty
    }

    @Test
    public void testSetPrice() throws DataConstraintsException {
        int[] invalidPrices = new int[]{-100, -10, -1};
        for (int price : invalidPrices) {
            assertThrows(DataConstraintsException.class, () -> {
                route.setPrice(price);
            });
        }

        int[] validPrices = new int[]{0, 1, 100000, 112315245};
        for (int price : validPrices) {
            route.setPrice(price);
            assertEquals(price, route.getPrice());
        }
    }

    @Test
    public void testSetCodeShare() {
        route.setCodeShare(Route.IS_CODE_SHARE_CHAR);
        assertTrue(route.isCodeShare());

        route.setCodeShare(true);
        assertTrue(route.isCodeShare());

        route.setCodeShare('b'); // Anything other than IS_CODE_SHARE
        assertFalse(route.isCodeShare());
    }

    @Test
    public void testSetFlightDuration() throws DataConstraintsException {
        int[] invalidValues = new int[]{-1, 24 * 60, 24 * 60 + 1};
        for (int duration : invalidValues) {
            assertThrows(DataConstraintsException.class, () -> {
                route.setFlightDuration(duration);
            });
        }

        int[] validValues = new int[]{1, 2, 24 * 60 - 1};
        for (int duration : validValues) {
            route.setFlightDuration(duration);
            assertEquals(duration, route.getFlightDuration());
        }
    }

    @Test
    public void testGeneratingDurationAndPrices() throws SQLException, DataConstraintsException {
        List<Integer> listA = new ArrayList<>();
        listA.add(5);
        listA.add(6);
        listA.add(7);
        listA.add(8);

        String[] listB = {"YOT", "YOE", "YET", "NOT"};
        Route testRoute = new Route("GNL", "GKA", "HGU", listB, 0, false, 200, listA);

        int duration = Route.generateFlightDuration(Route.calculateDistanceBetweenAirports("GKA", "213"), 900);
    }
}