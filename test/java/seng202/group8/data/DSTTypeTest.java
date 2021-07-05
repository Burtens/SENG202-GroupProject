package seng202.group8.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DSTTypeTest {

    /**
     * Tests to ensure the mapping from char code to enum value and then back returns the same value
     */
    @Test
    public void testBidirectionalMapping() {
        for (DSTType value : DSTType.values()) {
            assertEquals(value, DSTType.fromCode(DSTType.toCode(value)));
        }
    }
}