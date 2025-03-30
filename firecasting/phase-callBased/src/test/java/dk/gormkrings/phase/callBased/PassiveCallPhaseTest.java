package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.Passive;
import dk.gormkrings.data.IDate;
import dk.gormkrings.test.DummyDate;
import dk.gormkrings.test.DummyLiveData;
import dk.gormkrings.test.DummySpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PassiveCallPhaseTest {
    private DummySpecification dummySpec;
    private PassiveCallPhase passivePhase;

    @BeforeEach
    void setUp() {
        dummySpec = new DummySpecification();
        IDate dummyDate = new DummyDate(1000);
        Passive passive = new Passive();
        passivePhase = new PassiveCallPhase(dummySpec, dummyDate, 30, passive);
    }

    @Test
    void testOnPhaseStart_InitializesPreviouslyReturned() {
        DummyLiveData liveData = (DummyLiveData) dummySpec.getLiveData();
        liveData.setReturned(300);
        liveData.setCurrentReturn(100);

        passivePhase.onPhaseStart();

        assertEquals(300, passivePhase.getPassive().getPreviouslyReturned(), 0.001,
                "Passive previouslyReturned should equal liveData.getReturned() after onPhaseStart");
        assertEquals(100, liveData.getPassiveReturn(), 0.001,
                "Passive return should equal liveData.getCurrentReturn() after onPhaseStart");
        assertEquals(100, liveData.getPassiveReturned(), 0.001,
                "PassiveReturned should be increased by currentReturn after onPhaseStart");
    }

    @Test
    void testOnMonthEnd_CalculatesPassiveReturn() {
        DummyLiveData liveData = (DummyLiveData) dummySpec.getLiveData();
        liveData.setReturned(300);
        liveData.setCurrentReturn(50);

        passivePhase.onPhaseStart();

        double prevReturned = passivePhase.getPassive().getPreviouslyReturned();
        double passiveReturnedBefore = liveData.getPassiveReturned();
        liveData.setReturned(prevReturned + 250);
        liveData.addToCapital(250);

        passivePhase.onMonthEnd();

        double newReturned = liveData.getReturned();
        double expectedPassiveReturn = newReturned - prevReturned;

        assertEquals(expectedPassiveReturn, liveData.getPassiveReturn(), 0.001,
                "Passive return should equal the increase in returned since phase start");
        assertEquals(passiveReturnedBefore + expectedPassiveReturn, liveData.getPassiveReturned(), 0.001,
                "PassiveReturned should be increased by the calculated passive return after onMonthEnd()");
    }

    @Test
    void testCopyCreatesIndependentInstanceForPassiveCallPhase() {
        DummyLiveData liveData = (DummyLiveData) dummySpec.getLiveData();
        liveData.setReturned(300);
        liveData.setCurrentReturn(50);
        passivePhase.onPhaseStart();

        PassiveCallPhase copyPhase = passivePhase.copy(dummySpec);

        assertNotSame(passivePhase, copyPhase, "The copy should be a distinct instance");

        assertEquals(passivePhase.getStartDate().getEpochDay(), copyPhase.getStartDate().getEpochDay(), "Start dates should be equal");
        assertEquals(passivePhase.getDuration(), copyPhase.getDuration(), "Durations should be equal");
        assertNotEquals(passivePhase.getPassive(), copyPhase.getPassive(), "Passive should not be equal");

        assertEquals(passivePhase.getPassive().getPreviouslyReturned(), copyPhase.getPassive().getPreviouslyReturned(),
                0.001, "Passive previouslyReturned should be equal in copy");

        passivePhase.getPassive().setPreviouslyReturned(500);

        assertNotEquals(passivePhase.getPassive().getPreviouslyReturned(), copyPhase.getPassive().getPreviouslyReturned(),
                "Changes to the original passive should not affect the copy");
    }

    @Test
    void testOnMonthEnd_IntegrationOfAddReturnAndCalculatePassive() {
        DummyLiveData liveData = (DummyLiveData) dummySpec.getLiveData();

        liveData.setCapital(10000);
        liveData.setReturned(300);
        liveData.setCurrentReturn(0);

        passivePhase.onPhaseStart();
        double initialPrevReturned = passivePhase.getPassive().getPreviouslyReturned();
        assertEquals(300, initialPrevReturned, 0.001, "Initially, previouslyReturned should be 300");

        passivePhase.onMonthEnd();

        double expectedReturn = (10000 * 0.10) / 12;
        double expectedReturned = 300 + expectedReturn;
        double expectedPassiveReturn = expectedReturned - 300;

        assertEquals(expectedReturn, liveData.getCurrentReturn(), 0.001,
                "Current return should be updated to the calculated expected return");
        assertEquals(expectedReturned, liveData.getReturned(), 0.001,
                "Returned should be updated with the calculated return");
        assertEquals(expectedPassiveReturn, liveData.getPassiveReturn(), 0.001,
                "Passive return should equal the increase in returned since phase start");
        assertEquals(expectedPassiveReturn, liveData.getPassiveReturned(), 0.001,
                "PassiveReturned should equal the calculated passive return after onMonthEnd()");
    }

    @Test
    void testOnMonthEnd_ReturnCalculationAndPassiveReturn() {
        DummyLiveData liveData = (DummyLiveData) dummySpec.getLiveData();

        liveData.setReturned(300);
        liveData.setCurrentReturn(50);

        passivePhase.onPhaseStart();
        double storedPreviouslyReturned = passivePhase.getPassive().getPreviouslyReturned();
        assertEquals(300, storedPreviouslyReturned, 0.001, "Initially, previouslyReturned should be 300");

        liveData.setReturned(liveData.getReturned() + 200);
        liveData.addToCapital(200);

        passivePhase.onMonthEnd();

        double expectedPassiveReturn = liveData.getReturned() - storedPreviouslyReturned;

        assertEquals(expectedPassiveReturn, liveData.getPassiveReturn(), 0.001,
                "Passive return should equal the difference between new returned and previouslyReturned");
    }

    @Test
    void testOnMonthEnd_NoChangeInReturnedResultsInZeroPassiveReturn() {
        DummyLiveData liveData = (DummyLiveData) dummySpec.getLiveData();

        liveData.setReturned(300);
        liveData.setCurrentReturn(0);

        passivePhase.onPhaseStart();
        double storedPreviouslyReturned = passivePhase.getPassive().getPreviouslyReturned();
        assertEquals(300, storedPreviouslyReturned, 0.001, "PreviouslyReturned should be 300 after onPhaseStart()");

        passivePhase.onMonthEnd();

        assertEquals(0, liveData.getPassiveReturn(), 0.001, "Passive return should be 0 if no change in returned value");
        assertEquals(0, liveData.getPassiveReturned(), 0.001, "PassiveReturned should remain 0 if no change in returned value");
    }

}
