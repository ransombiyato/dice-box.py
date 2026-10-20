package io.github.ransombiyato.dicebox.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EventSchedulerTest {
    @Test
    void processesInTimeThenScheduleOrder() {
        EventScheduler<String> scheduler = new EventScheduler<>();
        scheduler.scheduleAt(10L, "c");
        scheduler.scheduleAt(5L, "a");
        scheduler.scheduleAt(5L, "b");

        assertEquals(List.of("a", "b", "c"), scheduler.runUntil(100L, 10));
        assertEquals(100L, scheduler.now());
        assertEquals(0, scheduler.pendingCount());
    }

    @Test
    void budgetStopsProcessingAndKeepsRemainingEvents() {
        EventScheduler<String> scheduler = new EventScheduler<>();
        scheduler.scheduleAt(1L, "one");
        scheduler.scheduleAt(2L, "two");
        scheduler.scheduleAt(3L, "three");

        assertEquals(List.of("one", "two"), scheduler.runUntil(10L, 2));
        // Time stops at the last processed event: resuming loses nothing.
        assertEquals(2L, scheduler.now());
        assertEquals(1, scheduler.pendingCount());
        assertEquals(Optional.of("three"), scheduler.runNext());
        assertEquals(3L, scheduler.now());
    }

    @Test
    void eventBeyondWindowStaysQueuedAndTimeAdvancesToWindowEnd() {
        EventScheduler<String> scheduler = new EventScheduler<>();
        scheduler.scheduleAt(50L, "later");

        assertEquals(List.of(), scheduler.runUntil(10L, 5));
        assertEquals(10L, scheduler.now());
        assertEquals(1, scheduler.pendingCount());
        assertEquals(50L, scheduler.nextEventTime().orElseThrow());
    }

    @Test
    void schedulingInThePastIsRejected() {
        EventScheduler<String> scheduler = new EventScheduler<>();
        scheduler.runUntil(10L, 1);
        assertThrows(IllegalArgumentException.class, () -> scheduler.scheduleAt(5L, "x"));
        assertThrows(IllegalArgumentException.class, () -> scheduler.scheduleAfter(-1L, "x"));
        assertThrows(IllegalArgumentException.class, () -> scheduler.runUntil(5L, 1));
        assertThrows(IllegalArgumentException.class, () -> scheduler.runUntil(10L, 0));
    }

    @Test
    void runNextOnEmptySchedulerDoesNotMoveTime() {
        EventScheduler<String> scheduler = new EventScheduler<>();
        scheduler.scheduleAfter(4L, "tick");
        assertEquals(Optional.of("tick"), scheduler.runNext());
        assertEquals(4L, scheduler.now());
        assertTrue(scheduler.runNext().isEmpty());
        assertEquals(4L, scheduler.now());
    }
}
