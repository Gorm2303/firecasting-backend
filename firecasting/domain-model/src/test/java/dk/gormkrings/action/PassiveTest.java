package dk.gormkrings.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PassiveTest {

    @Test
    void testDefaultPreviouslyReturned() {
        Passive passive = new Passive();
        assertEquals(0, passive.getPreviouslyReturned(), "Default previouslyReturned should be 0");
    }

    @Test
    void testSetPreviouslyReturned() {
        Passive passive = new Passive();
        passive.setPreviouslyReturned(150);
        assertEquals(150, passive.getPreviouslyReturned(), "previouslyReturned should be updated to 150");
    }

    @Test
    void testCopy() {
        Passive original = new Passive();
        original.setPreviouslyReturned(200);
        Passive copy = original.copy();
        assertEquals(original.getPreviouslyReturned(), copy.getPreviouslyReturned(), "Copy should have same previouslyReturned value");
        assertNotSame(original, copy, "Copy should be a different instance");
    }
}
