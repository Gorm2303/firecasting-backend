package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.data.IDate;
import dk.gormkrings.test.DummyDate;
import dk.gormkrings.test.DummyLiveData;
import dk.gormkrings.test.DummyReturner;
import dk.gormkrings.test.DummySpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DepositCallPhaseTest {

    private DummySpecification dummySpec;
    private DepositCallPhase depositPhase;

    @BeforeEach
    void setUp() {
        dummySpec = new DummySpecification();
        dummySpec.setReturner(new DummyReturner());
        IDate dummyDate = new DummyDate(1000);
        Deposit deposit = new Deposit(10000, 5000);
        depositPhase = new DepositCallPhase(dummySpec, dummyDate, 30, deposit);
    }

    @Test
    void testOnPhaseStart_DepositsInitialAmount() {
        DummyLiveData liveData = dummySpec.getLiveData();
        assertEquals(0, liveData.getDeposited(), "Deposited should start at 0");
        assertEquals(0, liveData.getCapital(), "Capital should start at 0");

        depositPhase.onPhaseStart();

        assertEquals(10000, liveData.getDeposited(), 0.001, "Deposited should be 10000 after phase start");
        assertEquals(10000, liveData.getCapital(), 0.001, "Capital should be 10000 after phase start");
    }

    @Test
    void testOnMonthEnd_SubsequentCallsIncludeReturnAndMonthlyDeposit() {
        DummyLiveData liveData = dummySpec.getLiveData();

        depositPhase.onPhaseStart();
        assertEquals(10000, liveData.getDeposited(), 0.001, "Deposited should be 10000 after phase start");
        assertEquals(10000, liveData.getCapital(), 0.001, "Capital should be 10000 after phase start");

        double expectedReturn = dummySpec.getReturner().calculateReturn(liveData.getCapital());
        depositPhase.onMonthEnd();

        assertEquals(15000, liveData.getDeposited(), 0.001, "Deposited should be 15000 after first onMonthEnd");
        assertEquals(10000 + expectedReturn + 5000, liveData.getCapital(), 0.001, "Capital should reflect initial plus return and monthly deposit");

        double capitalAfterFirst = liveData.getCapital();
        double expectedReturn2 = dummySpec.getReturner().calculateReturn(capitalAfterFirst);

        depositPhase.onMonthEnd();

        assertEquals(15000 + 5000, liveData.getDeposited(), 0.001, "Deposited should increase by 5000 on subsequent onMonthEnd calls");
        double expectedCapitalAfterSecond = capitalAfterFirst + expectedReturn2 + 5000;
        assertEquals(expectedCapitalAfterSecond, liveData.getCapital(), 0.001, "Capital should be increased by return and monthly deposit on subsequent calls");
    }

    @Test
    void testCopyCreatesIndependentInstance() {
        DepositCallPhase copyPhase = depositPhase.copy(dummySpec);

        assertNotSame(depositPhase, copyPhase, "The copy should be a distinct instance");

        assertEquals(depositPhase.getStartDate().getEpochDay(), copyPhase.getStartDate().getEpochDay(),
                "Start dates should be equal");
        assertEquals(depositPhase.getDuration(), copyPhase.getDuration(), "Durations should be equal");

        Deposit originalDeposit = depositPhase.getDeposit();
        Deposit copyDeposit = copyPhase.getDeposit();
        assertEquals(originalDeposit.getInitial(), copyDeposit.getInitial(), 0.001,
                "Initial deposit values should be equal");
        assertEquals(originalDeposit.getMonthly(), copyDeposit.getMonthly(), 0.001,
                "Monthly deposit values should be equal");

        originalDeposit.setMonthly(6000);
        assertNotEquals(originalDeposit.getMonthly(), copyDeposit.getMonthly(),
                "Changes to the original deposit should not affect the copy");
        assertEquals(dummySpec, copyPhase.getSpecification(), "Copy specification should be equal");
    }

    @Test
    void testOnMonthEnd_CallsAddReturnAndUpdatesLiveData() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.addToCapital(10000);

        double expectedReturn = (20000 * 0.10) / 12;

        depositPhase.onPhaseStart();
        depositPhase.onMonthEnd();

        assertEquals(expectedReturn, liveData.getCurrentReturn(), 0.001,
                "Current return should be updated by addReturn()");

        assertEquals(expectedReturn, liveData.getReturned(), 0.001,
                "Returned value should be increased by the calculated return");

        double capitalAfterAddReturn = 10000 + expectedReturn;
        assertTrue(liveData.getCapital() >= capitalAfterAddReturn,
                "Capital should be increased by at least the calculated return after onMonthEnd()");
    }

    @Test
    void testDepositMoney_DirectInvocation() {
        DummyLiveData liveData = dummySpec.getLiveData();

        depositPhase.onPhaseStart();

        double depositedAfterInitial = liveData.getDeposited();
        double capitalAfterInitial = liveData.getCapital();

        depositPhase.depositMoney();

        double expectedMonthlyDeposit = 5000;

        assertEquals(depositedAfterInitial + expectedMonthlyDeposit, liveData.getDeposited(), 0.001,
                "Deposited should increase by monthly deposit amount on direct depositMoney() call");
        assertEquals(capitalAfterInitial + expectedMonthlyDeposit, liveData.getCapital(), 0.001,
                "Capital should increase by monthly deposit amount on direct depositMoney() call");
    }

    @Test
    void testDepositMoney_WithZeroMonthlyDeposit() {
        Deposit zeroMonthlyDeposit = new Deposit(10000, 0);
        DepositCallPhase phaseWithZeroMonthly = new DepositCallPhase(dummySpec, new DummyDate(1000), 30, zeroMonthlyDeposit);
        DummyLiveData liveData = dummySpec.getLiveData();

        phaseWithZeroMonthly.onPhaseStart();
        assertEquals(10000, liveData.getDeposited(), 0.001, "Deposited should be 10000 after phase start");
        assertEquals(10000, liveData.getCapital(), 0.001, "Capital should be 10000 after phase start");

        phaseWithZeroMonthly.depositMoney();

        assertEquals(10000, liveData.getDeposited(), 0.001, "Deposited should remain 10000 when monthly deposit is zero");
        assertEquals(10000, liveData.getCapital(), 0.001, "Capital should remain 10000 when monthly deposit is zero");
    }


}
