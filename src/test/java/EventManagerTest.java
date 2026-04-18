import org.example.core.EventManager;
import org.example.model.Event;
import org.example.model.Effects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

public class EventManagerTest {
    private EventManager eventManager;

    @BeforeEach
    void setUp() {
        eventManager = new EventManager(new Random(42));
    }

    // ==========================================
    // Random Event Generation
    // ==========================================

    @Test
    void getRandomEvent_returnsNonNull() {
        Event event = eventManager.getRandomEvent();
        assertNotNull(event);
    }

    @Test
    void getRandomEvent_hasDescription() {
        Event event = eventManager.getRandomEvent();
        assertNotNull(event.description());
        assertFalse(event.description().isEmpty());
    }

    @Test
    void getRandomEvent_hasEffects() {
        Event event = eventManager.getRandomEvent();
        assertNotNull(event.choice1Effects());
        assertNotNull(event.choice2Effects());
    }

    @Test
    void getRandomEvent_withSeed_isDeterministic() {
        EventManager em1 = new EventManager(new Random(99));
        EventManager em2 = new EventManager(new Random(99));

        Event event1 = em1.getRandomEvent();
        Event event2 = em2.getRandomEvent();

        assertEquals(event1.description(), event2.description());
    }

    @Test
    void getRandomEvent_differentSeeds_canProduceDifferentEvents() {
        EventManager em1 = new EventManager(new Random(1));
        EventManager em2 = new EventManager(new Random(999));

        // 多次调用增加概率得到不同事件
        boolean foundDifferent = false;
        for (int i = 0; i < 20; i++) {
            Event event1 = em1.getRandomEvent();
            Event event2 = em2.getRandomEvent();
            if (!event1.description().equals(event2.description())) {
                foundDifferent = true;
                break;
            }
        }
        assertTrue(foundDifferent);
    }

    // ==========================================
    // Event Choices
    // ==========================================

    @Test
    void eventWithChoices_hasBothLabels() {
        // 不断拿事件直到找到一个有选择的
        EventManager em = new EventManager(new Random(0));
        boolean foundChoiceEvent = false;
        for (int i = 0; i < 50; i++) {
            Event event = em.getRandomEvent();
            if (event.choice1() != null) {
                assertNotNull(event.choice2());
                foundChoiceEvent = true;
                break;
            }
        }
        assertTrue(foundChoiceEvent);
    }

    @Test
    void nothingEvent_hasNullChoices() {
        EventManager em = new EventManager(new Random(0));
        boolean foundNothingEvent = false;
        for (int i = 0; i < 50; i++) {
            Event event = em.getRandomEvent();
            if (event.description().contains("Nothing eventful")) {
                assertNull(event.choice1());
                assertNull(event.choice2());
                foundNothingEvent = true;
                break;
            }
        }
        assertTrue(foundNothingEvent);
    }

    @Test
    void nothingEvent_hasZeroEffects() {
        EventManager em = new EventManager(new Random(0));
        for (int i = 0; i < 50; i++) {
            Event event = em.getRandomEvent();
            if (event.description().contains("Nothing eventful")) {
                Effects effects = event.choice1Effects();
                assertEquals(0, effects.cash());
                assertEquals(0, effects.morale());
                assertEquals(0, effects.compute());
                assertEquals(0, effects.coffee());
                assertEquals(0, effects.hype());
                assertEquals(0, effects.bugs());
                return;
            }
        }
        fail("Nothing event not found in 50 attempts");
    }

    // ==========================================
    // Event Effects Validity
    // ==========================================

    @Test
    void allEvents_haveBothEffects() {
        EventManager em = new EventManager(new Random(0));
        for (int i = 0; i < 100; i++) {
            Event event = em.getRandomEvent();
            assertNotNull(event.choice1Effects());
            assertNotNull(event.choice2Effects());
        }
    }

    @Test
    void choiceA_andChoiceB_canHaveDifferentEffects() {
        EventManager em = new EventManager(new Random(0));
        boolean foundDifferent = false;
        for (int i = 0; i < 50; i++) {
            Event event = em.getRandomEvent();
            if (event.choice1() != null) {
                Effects a = event.choice1Effects();
                Effects b = event.choice2Effects();
                if (a.cash() != b.cash() || a.morale() != b.morale()) {
                    foundDifferent = true;
                    break;
                }
            }
        }
        assertTrue(foundDifferent);
    }
}