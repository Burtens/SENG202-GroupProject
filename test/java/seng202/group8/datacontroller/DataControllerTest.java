package seng202.group8.datacontroller;

import org.junit.Before;
import org.junit.Test;
import seng202.group8.data.Data;
import seng202.group8.io.ConstraintsError;
import seng202.group8.io.SortOrder;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

class DummyData extends Data {
    DummyData(int id) {
        super(id);
    }

    DummyData() {
        super();
    }

    @Override
    public String toString() {
        return String.format("%d", getId());
    }
}

class DummyDC extends DataController<DummyData> {
    private static int id = 1;

    public HashMap<Integer, DummyData> database = new HashMap<>();


//    public static enum ViolatedConstraintsBehaviour {
//        ErrorFound, ErrorNotFound, ExceptionThrown;
//    }
//
//    public ViolatedConstraintsBehaviour  violatedConstraintsBehaviour = ViolatedConstraintsBehaviour.ErrorFound;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onDBChange() { return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getBatchAddToDatabaseStatement() {
        return null;
    }

    @Override
    public void deleteFromDatabase(int id) throws SQLException {
    }

    @Override
    public DummyData getEntity(int id) throws SQLException {
        return database.get(id);
    }


    @Override
    public List<DummyData> getSortedFilteredEntities(String sortColumn, SortOrder order, int numRows, int offset) throws SQLException {
        return null;
    }

    @Override
    protected DummyData addToDatabase(DummyData data, boolean returnNew) throws SQLException, ConstraintsError {
        return addToDatabase(data);
    }

    @Override
    protected DummyData addToDatabase(DummyData data) throws SQLException, ConstraintsError {
        DummyData withId = new DummyData(id++);
        database.put(withId.getId(), withId);
        return withId;
    }

    @Override
    protected void updateInDatabase(DummyData data) throws SQLException, ConstraintsError {
        database.put(data.getId(), data);
    }
}

public class DataControllerTest {
    DummyDC dc = new DummyDC();

    @Before
    public void setup() {

    }

    public DummyData standardDummy() {
        return new DummyData(100);
    }

//    @Test
//    public void testFindViolatedConstraintsWrapper() {
//        DummyData data = new DummyData(10);
//
//        dc.violatedConstraintsBehaviour = DummyDC.ViolatedConstraintsBehaviour.ErrorFound;
//        assertTrue(dc.findViolatedConstraintsWrapper(data).error(DummyData.UNKNOWN_COLUMN).equals("ERROR FOUND"));
//
//        dc.violatedConstraintsBehaviour = DummyDC.ViolatedConstraintsBehaviour.ErrorNotFound;
//        assertTrue(dc.findViolatedConstraintsWrapper(data).error(DummyData.UNKNOWN_COLUMN).contains("unknown error"));
//
//        dc.violatedConstraintsBehaviour = DummyDC.ViolatedConstraintsBehaviour.ExceptionThrown;
//        assertTrue(dc.findViolatedConstraintsWrapper(data).error(DummyData.UNKNOWN_COLUMN).contains("unknown error"));
//    }
//


    @Test
    public void testGlobalObserversChange() throws SQLException {
        DummyData data = standardDummy();
        dc.save(data);
        DummyObserver observer1 = new DummyObserver<DummyData>();
        DummyObserver observer2 = new DummyObserver<DummyData>();

        dc.addObserver(data.getId(), observer1);
        dc.addObserver(DataController.OBSERVE_ALL, observer1);
        dc.addObserver(DataController.OBSERVE_ALL, observer2);

        dc.notifyObservers(data.getId());

        assertEquals(observer1.dataChanged.get(0), data);
        assertEquals(1, observer1.dataChanged.size());

        assertEquals(observer2.dataChanged.get(0), data);
        assertEquals(1, observer2.dataChanged.size());
    }

    @Test
    public void testGlobalObserversModify() throws SQLException {
        DummyData data = standardDummy();
        dc.save(data);
        DummyObserver observer1 = new DummyObserver<DummyData>();
        DummyObserver observer2 = new DummyObserver<DummyData>();

        dc.addObserver(data.getId(), observer1);
        dc.addObserver(DataController.OBSERVE_ALL, observer1);
        dc.addObserver(DataController.OBSERVE_ALL, observer2);

        dc.notifyObserversOfDeletion(data.getId());

        // observer 1: change notified
        assertNull(observer1.dataChanged.get(0));
        assertEquals(1, observer1.dataChanged.size());

        // observer 2: just change
        assertEquals(1, observer2.dataChanged.size());
    }


    @Test
    public void testAddingDuplicatesObservers() throws DataConstraintsException, SQLException {
        DummyData data = standardDummy();
        dc.save(data);

        DummyObserver observer1 = new DummyObserver<DummyData>();

        dc.addObserver(data.getId(), observer1);
        dc.addObserver(data.getId(), observer1);

        dc.notifyObservers(data.getId()); // In cache and observers exist, so true

        assertEquals(1, observer1.dataChanged.size()); // Added twice, should only notify once
        assertEquals(0, observer1.dataDeleted.size()); // Should not call deletion method
    }

    @Test
    public void testAddingDuplicatesGlobalObservers() throws DataConstraintsException, SQLException {
        DummyData data = standardDummy();
        dc.save(data);

        DummyObserver observer1 = new DummyObserver<DummyData>();

        dc.addObserver(data.getId(), observer1);
        dc.addObserver(data.getId(), observer1);
        dc.addObserver(DataController.OBSERVE_ALL, observer1);

        dc.notifyObservers(data.getId()); // In cache and observers exist, so true

        assertEquals(1, observer1.dataChanged.size()); // Added twice, should only notify once

        dc.notifyObserversOfDeletion(data.getId());

        assertEquals(2, observer1.dataChanged.size());
    }


    @Test
    public void testMultipleObserversMultipleObjects() throws DataConstraintsException, SQLException {
        DummyObserver observer1 = new DummyObserver<DummyData>();
        DummyObserver observer2 = new DummyObserver<DummyData>();
        DummyObserver observer3 = new DummyObserver<DummyData>();
        DummyData data1 = new DummyData(1);
        DummyData data2 = new DummyData(2);
        DummyData data3 = new DummyData(3);
        DummyData data4 = new DummyData(4);

        dc.save(data1);
        dc.save(data2);
        dc.save(data3);
        dc.save(data4);

        dc.addObserver(1, observer1);
        dc.addObserver(2, observer1);
        dc.addObserver(3, observer1);
        dc.addObserver(1, observer2);
        dc.addObserver(2, observer2);
        dc.addObserver(4, observer2);
        dc.addObserver(DataController.OBSERVE_ALL, observer3);

        dc.notifyObservers(1);
        dc.notifyObservers(2);
        dc.notifyObservers(3);

        Collections.sort(observer1.dataChanged, Comparator.comparing(DummyData::getId));
        Collections.sort(observer2.dataChanged, Comparator.comparing(DummyData::getId));
        Collections.sort(observer3.dataChanged, Comparator.comparing(DummyData::getId));

        assertEquals(Arrays.asList(data1, data2, data3), observer1.dataChanged);
        assertEquals(Arrays.asList(data1, data2), observer2.dataChanged);
        assertEquals(Arrays.asList(data1, data2, data3), observer3.dataChanged);
    }

    @Test
    public void testNotifyObserversExcept() throws DataConstraintsException, SQLException {
        DummyData data = standardDummy();

        DummyObserver observer1 = new DummyObserver<DummyData>();
        dc.save(data);
        DummyObserver observer2 = new DummyObserver<DummyData>();
        DummyObserver observer3 = new DummyObserver<DummyData>();

        dc.addObserver(data.getId(), observer1);
        dc.addObserver(data.getId(), observer2);
        dc.addObserver(DataController.OBSERVE_ALL, observer3);

        // All but the second observer should be notified
        dc.notifyObservers(data.getId(), observer2);
        assertEquals(1, observer1.dataChanged.size());
        assertEquals(0, observer2.dataChanged.size());
        assertEquals(1, observer3.dataChanged.size());
    }

    @Test
    public void testNotifyObserversOfDeletion() throws DataConstraintsException, SQLException {
        DummyData data1 = standardDummy();
        DummyData data2 = new DummyData(data1.getId() + 1);

        DummyObserver observer1 = new DummyObserver<DummyData>();
        DummyObserver observer2 = new DummyObserver<DummyData>();

        dc.save(data1);
        dc.save(data2);

        dc.addObserver(data1.getId(), observer1);
        dc.addObserver(data1.getId(), observer2);
        dc.addObserver(data2.getId(), observer2);

        dc.notifyObserversOfDeletion(data1.getId());
        dc.notifyObserversOfDeletion(data2.getId());

        assertEquals(0, observer1.dataChanged.size());
        assertEquals(0, observer2.dataChanged.size());
    }

    @Test
    public void testRemoveObserver() throws DataConstraintsException, SQLException {
        DummyData data1 = standardDummy();
        DummyData data2 = new DummyData(data1.getId() + 1);

        DummyObserver observer1 = new DummyObserver<DummyData>();
        DummyObserver observer2 = new DummyObserver<DummyData>();

        dc.save(data1);
        dc.save(data2);

        dc.addObserver(data1.getId(), observer1);
        dc.addObserver(data1.getId(), observer2);
        dc.addObserver(data2.getId(), observer1);
        dc.addObserver(data2.getId(), observer2);

        dc.removeObserver(data1.getId(), observer1);
        dc.removeObserver(data2.getId(), observer2);
        dc.removeObserver(data2.getId(), observer2); // Removing twice should not fail

        dc.notifyObserversOfDeletion(data1.getId());
        dc.notifyObserversOfDeletion(data2.getId());
    }

}
