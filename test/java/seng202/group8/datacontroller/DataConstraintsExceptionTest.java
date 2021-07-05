package seng202.group8.datacontroller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataConstraintsExceptionTest {
    public static String PROP_1 = "PROP 1";
    public static String PROP_2 = "PROP 2";
    public static String PROP_3 = "PROP 3";
    public static String PROP_4 = "PROP 4";

    @Test
    public void testMergeExceptions() {
        String exception1Message = "Error on object 1";
        DataConstraintsException exception1 = new DataConstraintsException(PROP_1, exception1Message);
        exception1.errors.put(PROP_2, exception1Message);

        String exception2Message = "Error on object 2";
        DataConstraintsException exception2 = new DataConstraintsException(PROP_2, exception2Message);
        exception2.errors.put(PROP_3, exception2Message);

        exception1.mergeExceptions(exception2);

        assertEquals(exception1Message, exception1.error(PROP_1));
        assertEquals(exception1Message + " AND " + exception2Message, exception1.error(PROP_2));
        assertEquals(exception2Message, exception1.error(PROP_3));
        assertNull(exception1.error(PROP_4));
    }

    @Test
    public void testAttemptNotNullNotNullNull() {
        String exception1Message = "Error on object 1";
        DataConstraintsException exception1 = new DataConstraintsException(PROP_1, exception1Message);
        exception1.errors.put(PROP_2, exception1Message);

        String exception2Message = "Error on object 2";
        DataConstraintsException exception2 = new DataConstraintsException(PROP_2, exception2Message);
        exception2.errors.put(PROP_3, exception2Message);

        DataConstraintsException returned = DataConstraintsException.attempt(exception1, () -> {
            throw exception2;
        });

        assertEquals(returned, exception1);

        assertEquals(exception1Message, exception1.error(PROP_1));
        assertEquals(exception1Message + " AND " + exception2Message, exception1.error(PROP_2));
        assertEquals(exception2Message, exception1.error(PROP_3));
        assertNull(exception1.error(PROP_4));
    }

    @Test
    public void testAttemptNullNotNull() {
        DataConstraintsException passed = null;
        DataConstraintsException addedException = new DataConstraintsException(PROP_1, "Bla");
        passed = DataConstraintsException.attempt(passed, () -> {
            throw addedException;
        });

        assertEquals(passed, addedException);
    }

    @Test
    public void testAttemptNullNull() {
        DataConstraintsException passed = null;
        DataConstraintsException addedException = null;
        passed = DataConstraintsException.attempt(passed, () -> {
        });

        assertNull(passed);
    }

    @Test
    public void testAttemptNotNullNull() {
        DataConstraintsException passed = new DataConstraintsException(PROP_1, "bla");
        DataConstraintsException addedException = null;
        passed = DataConstraintsException.attempt(passed, () -> {
        });
    }

    @Test
    public void testAttempt() {
        DataConstraintsException exception = new DataConstraintsException(PROP_1, "bla");
        exception.attempt(() -> {
            throw new DataConstraintsException(PROP_2, "bla 2");
        });
        assertEquals(exception.errors.size(), 2);
    }

    @Test
    public void testMergeExceptionsWithNull() {
        DataConstraintsException exception = new DataConstraintsException(PROP_1, "bla");
        exception.mergeExceptions(null);
        assertEquals(exception.errors.size(), 1);
    }

    @Test
    public void testGetMessage() {
        DataConstraintsException passed = new DataConstraintsException(PROP_1, "bla");
        assertEquals("The following data constraints (1) were violated:\n'PROP 1': bla", passed.getMessage());
    }

    @Test
    public void testGetMessageMultipleErrors() {
        DataConstraintsException passed = new DataConstraintsException(PROP_1, "bla");
        passed.mergeExceptions(new DataConstraintsException(PROP_3, "bla3"));
        passed.mergeExceptions(new DataConstraintsException(PROP_2, "bla2"));
        // No guarantee of order

        assertEquals("The following data constraints (3) were violated:\n'PROP 1': bla\n'PROP 2': bla2\n'PROP 3': bla3", passed.getMessage());
    }
}