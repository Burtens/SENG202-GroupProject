package seng202.group8.data;

import javafx.collections.FXCollections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import seng202.group8.data.filters.*;
import seng202.group8.datacontroller.AirlineDataController;
import seng202.group8.datacontroller.AirportDataController;
import seng202.group8.datacontroller.DataConstraintsException;
import seng202.group8.datacontroller.FiltersController;
import seng202.group8.io.ConstraintsError;
import seng202.group8.io.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.Assert.*;

class dummyFilterObserver implements FilterChangeObserver {
    public int timesCalled = 0;

    @Override
    public void filterChangedEvent() {
        timesCalled++;
    }
}


public class FilterTests {

    public FiltersController filtersC = FiltersController.getSingleton();
    public Connection db;

    @Before
    public void setup() throws SQLException {
        Database.establishConnection();
        db = Database.databaseConnection;
        db.setAutoCommit(false);

        try {
            // Ensures it works even if DB is empty
            AirportDataController.getSingleton().save(new Airport("Christchurch International Airport", "Christchurch", "New Zealand", "CHC", "NZCH", -43.4893989562988, 172.531997680664, 123, 12.0, 'Z'));
            AirlineDataController.getSingleton().save(new Airline("Air New Zealand", "NEW ZEALAND", "NZ", "ANZ", "New Zealand"));
        } catch (DataConstraintsException | ConstraintsError e) {
//            System.out.println("Could not add CHCH airport and/or Air NZ to DB; probably okay, likely already in the DB");
        }
    }


    @After
    public void teardown() throws SQLException {
        db.rollback();
    }

    @Test
    public void testGetSetTextualFilterOptions() {
        ArrayList<String> list = new ArrayList<>();
        TextualFilter filter = new TextualFilter("Blah", FXCollections.observableList(list));
        assertEquals(0, filter.getOptions().size());
        list.add("ASDASD");
        filter.setOptions(FXCollections.observableList(list));
        assertEquals(filter.getOptions(), FXCollections.observableList(list));
    }


    @Test
    public void testTextualFilterSelectAndRemoveOption() {
        ArrayList<String> list = new ArrayList<>();
        list.add("ASDASD");
        list.add("AB");

        TextualFilter filter = new TextualFilter("Blah", FXCollections.observableList(list));
        assertEquals(0, filter.getSelectedOptions().size());
        filter.selectOption("ASDASD");
        HashSet<String> expected = new HashSet<>();
        expected.add("ASDASD");
        assertEquals(filter.getSelectedOptions(), expected);

        filter.removeOption("ASDASD");
        assertEquals(0, filter.getSelectedOptions().size());

    }

    @Test
    public void testTextualFilterSelectNonexistentOption() {
        ArrayList<String> list = new ArrayList<>();
        list.add("ASDASD");
        list.add("AB");

        TextualFilter filter = new TextualFilter("Blah", FXCollections.observableList(list));
        filter.selectOption("BDEFS");
        assertEquals(0, filter.getSelectedOptions().size());
    }

    @Test
    public void testTextualFilterRemoveUnselectedOptions() {
        ArrayList<String> list = new ArrayList<>();
        list.add("ASDASD");
        list.add("AB");

        TextualFilter filter = new TextualFilter("Blah", FXCollections.observableList(list));
        filter.removeOption("BDEFS");
        assertEquals(0, filter.getSelectedOptions().size());
        filter.removeOption("ASDASD");
        assertEquals(0, filter.getSelectedOptions().size());
    }

    @Test
    public void testNumericFilterConstructor() {
        NumericFilter filter = new NumericFilter("BLAH", 0, 100, 5);
        assertEquals(filter.getMax(), (Integer) 100);
        assertEquals(filter.getMin(), (Integer) 0);
        assertEquals(filter.getStepBy(), (Integer) 5);
        assertEquals(filter.getFilterName(), "BLAH");
    }

    @Test
    public void testNumericFilterMaxLessThanMin() {
        assertThrows(IllegalArgumentException.class, () -> {
            NumericFilter filter = new NumericFilter("BLAH", 10, 9, 5);
        });
    }

    @Test
    public void testNumericFilterMaxEqualMin() {
        assertThrows(IllegalArgumentException.class, () -> {
            NumericFilter filter = new NumericFilter("BLAH", 10, 10, 5);
        });
    }

    @Test
    public void testNumericFilterSetGetBounds() {
        FilterRange<Integer> range = new FilterRange<>(69, 42);
        NumericFilter filter = new NumericFilter("BLAH", 0, 100, 5);
        filter.setRange(range);
        assertEquals(filter.getBounds(), range);

        range.max = 420;
        range.min = -100;
        filter.setRange(range);
        assertEquals(filter.getBounds(), range);
    }

    @Test
    public void testTextualFilterPredicate() {
        TextualFilterPredicate predicate = new TextualFilterPredicate("ABCD");
        assertTrue(predicate.test("ABCD"));
        assertTrue(predicate.test("ABCDED"));
        assertTrue(predicate.test("}:?#$987435.32ABCD*#^[."));
        assertTrue(predicate.test("---ABCD---ABCD"));
        assertTrue(predicate.test("abcd"));

        assertFalse(predicate.test("ABC"));
        assertFalse(predicate.test("G^$@09C}>'3.6?l0"));
        assertFalse(predicate.test(""));
    }

    @Test
    public void testTextualFilterPredicateEmptyString() {
        TextualFilterPredicate predicate = new TextualFilterPredicate("");
        assertTrue(predicate.test(""));
        assertTrue(predicate.test("ABCDED"));
        assertTrue(predicate.test("--3546BCD0<>#$^%"));
        assertTrue(predicate.test("abcd"));
        assertTrue(predicate.test("ABC"));
        assertTrue(predicate.test("GA5B7Cl0"));
        assertTrue(predicate.test("436575647"));
    }

    @Test
    public void testFilterObserver() {
        dummyFilterObserver observer = new dummyFilterObserver();

        FiltersController.getSingleton().addObserver(observer);
        assertEquals(0, observer.timesCalled);

        FiltersController.getSingleton().notifyAllObservers();
        assertEquals(1, observer.timesCalled);

        FiltersController.getSingleton().notifyAllObservers();
        assertEquals(2, observer.timesCalled);
    }

    @Test
    public void testFiltersUpdatedByDataAdded() throws SQLException, DataConstraintsException {
        assertFalse(filtersC.getAirportCodeFilter().getOptions().contains("999"));
        AirportDataController.getSingleton().save(new Airport("Matty G Airport", "Christchurch", "New Zealand", "999", "4269", 10, 10, 0, 0, 'Z'));
        assertTrue(filtersC.getAirportCodeFilter().getOptions().contains("999"));
    }

    @Test
    public void testFiltersUpdatedByDataDeleted() throws SQLException {
        // See setup() if fails
        assertTrue(filtersC.getAirlineNameFilter().getOptions().contains("Air New Zealand"));
        Airline airline = AirlineDataController.getSingleton().getEntityByName("Air New Zealand");
        AirlineDataController.getSingleton().deleteFromDatabase(airline.getId());
        assertFalse(filtersC.getAirlineNameFilter().getOptions().contains("Air New Zealand"));
    }

    @Test
    public void testFiltersUpdatedByDataChanged() throws SQLException, DataConstraintsException {
        // See setup() if fails
        assertTrue(filtersC.getAirportNameFilter().getOptions().contains("Christchurch International Airport"));
        assertFalse(filtersC.getAirportNameFilter().getOptions().contains("Mattias"));

        Airport airport = AirportDataController.getSingleton().getEntity("CHC");
        airport.setName("Mattias");
        AirportDataController.getSingleton().save(airport);
        assertTrue(filtersC.getAirportNameFilter().getOptions().contains("Mattias"));
    }
}
