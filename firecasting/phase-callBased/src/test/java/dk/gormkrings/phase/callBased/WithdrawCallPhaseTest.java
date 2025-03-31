package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.IDate;
import dk.gormkrings.tax.CapitalGainsTax;
import dk.gormkrings.tax.NotionalGainsTax;
import dk.gormkrings.test.DummyDate;
import dk.gormkrings.test.DummyLiveData;
import dk.gormkrings.test.DummyReturner;
import dk.gormkrings.test.DummySpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WithdrawCallPhaseTest {
    private DummySpecification dummySpec;
    private WithdrawCallPhase withdrawPhase;

    @BeforeEach
    void setUp() {
        dummySpec = new DummySpecification();
        dummySpec.setReturner(new DummyReturner());
        IDate dummyDate = new DummyDate(1000);
        Withdraw withdraw = new Withdraw(5000, 0);
        withdrawPhase = new WithdrawCallPhase(dummySpec, dummyDate, 30, withdraw);
    }

    @Test
    void testInitialStateBeforeWithdrawal() {
        DummyLiveData liveData = (DummyLiveData) dummySpec.getLiveData();
        assertEquals(0, liveData.getWithdraw(), "Initial withdraw should be 0");
        assertEquals(0, liveData.getWithdrawn(), "Initial withdrawn should be 0");
        assertEquals(0, liveData.getCurrentTax(), "Initial current tax should be 0");
        assertEquals(0, liveData.getCurrentNet(), "Initial current net should be 0");
    }

    @Test
    void testOnMonthEnd_WithdrawalCalculation_WithReturn() {
        DummyLiveData liveData = (DummyLiveData) dummySpec.getLiveData();
        liveData.setCapital(20000);
        double expectedWithdrawal = withdrawPhase.getWithdraw().getMonthlyAmount(liveData.getCapital());
        double expectedReturn = (20000 * 0.10) / 12; //

        withdrawPhase.onMonthEnd();

        double expectedCapital = (20000 + expectedReturn) - expectedWithdrawal;

        assertEquals(expectedWithdrawal, liveData.getWithdraw(), 0.001,
                "Withdraw should be set to the expected monthly withdrawal");
        assertEquals(expectedWithdrawal, liveData.getWithdrawn(), 0.001,
                "Withdrawn should increase by the expected withdrawal amount");
        assertEquals(expectedCapital, liveData.getCapital(), 0.001,
                "Capital should be decreased correctly after adding return and subtracting withdrawal");
    }

    @Test
    void testOnMonthEnd_TaxCalculation_CapitalGainsTax() {
        dummySpec.setTaxRule(new CapitalGainsTax(42));
        DummyLiveData liveData = (DummyLiveData) dummySpec.getLiveData();
        liveData.setCapital(20000);

        double expectedWithdrawal = withdrawPhase.getWithdraw().getMonthlyAmount(liveData.getCapital());
        double expectedTax = (expectedWithdrawal * 42) / 100;

        withdrawPhase.onMonthEnd();

        assertEquals(expectedWithdrawal, liveData.getWithdraw(), 0.001,
                "Withdraw should equal the expected monthly withdrawal amount");
        assertEquals(expectedWithdrawal, liveData.getWithdrawn(), 0.001,
                "Withdrawn should equal the expected monthly withdrawal amount");
        assertEquals(expectedTax, liveData.getCurrentTax(), 0.001,
                "Current tax should equal withdrawal * taxRate/100 for CapitalGainsTax");
        assertEquals(expectedTax, liveData.getTax(), 0.001,
                "Total tax should equal the calculated tax for CapitalGainsTax");

        double expectedNet = expectedWithdrawal - expectedTax;
        assertEquals(expectedNet, liveData.getCurrentNet(), 0.001,
                "Current net earnings should equal withdrawal minus current tax for CapitalGainsTax");
        assertEquals(expectedNet, liveData.getNet(), 0.001,
                "Net earnings should equal withdrawal minus tax for CapitalGainsTax");
    }


    @Test
    void testOnMonthEnd_TaxCalculation_NotionalGainsTax() {
        dummySpec.setTaxRule(new NotionalGainsTax(42));
        DummyLiveData liveData = (DummyLiveData) dummySpec.getLiveData();
        liveData.setCapital(20000);
        double expectedWithdrawal = withdrawPhase.getWithdraw().getMonthlyAmount(liveData.getCapital());
        double expectedTax = 0;

        withdrawPhase.onMonthEnd();

        assertEquals(expectedTax, liveData.getCurrentTax(), 0.001,
                "For NotionalGainsTax, current tax should be 0");
        assertEquals(expectedTax, liveData.getTax(), 0.001,
                "For NotionalGainsTax, total tax should be 0");
        assertEquals(expectedWithdrawal, liveData.getCurrentNet(), 0.001,
                "For NotionalGainsTax, current net earnings should equal the withdrawal amount");
        assertEquals(expectedWithdrawal, liveData.getNet(), 0.001,
                "For NotionalGainsTax, net earnings should equal the withdrawal amount");
    }

    @Test
    void testOnMonthEnd_IntegrationOfMethods_OrderVerification() {
        dummySpec.setTaxRule(new CapitalGainsTax(42));
        DummyLiveData liveData = (DummyLiveData) dummySpec.getLiveData();
        liveData.setCapital(20000);

        double expectedWithdrawal = withdrawPhase.getWithdraw().getMonthlyAmount(liveData.getCapital());
        assertEquals(5000, expectedWithdrawal, 0.001, "Expected withdrawal should be 5000");
        double expectedReturn = (20000 * 0.10) / 12;

        withdrawPhase.onMonthEnd();

        double expectedCapital = (20000 + expectedReturn) - expectedWithdrawal;
        double expectedTax = (expectedWithdrawal * 42) / 100;
        double expectedNet = expectedWithdrawal - expectedTax;

        assertEquals(expectedWithdrawal, liveData.getWithdraw(), 0.001,
                "Withdraw field should be set to the expected withdrawal amount");
        assertEquals(expectedWithdrawal, liveData.getWithdrawn(), 0.001,
                "Withdrawn field should equal the expected withdrawal amount");
        assertEquals(expectedTax, liveData.getCurrentTax(), 0.001,
                "Current tax should equal the withdrawal amount multiplied by tax rate/100");
        assertEquals(expectedTax, liveData.getTax(), 0.001,
                "Overall tax should equal the calculated tax");
        assertEquals(expectedNet, liveData.getCurrentNet(), 0.001,
                "Current net earnings should equal withdrawal minus tax");
        assertEquals(expectedNet, liveData.getNet(), 0.001,
                "Net earnings should equal withdrawal minus tax");
        assertEquals(expectedCapital, liveData.getCapital(), 0.001,
                "Capital should be increased by the return and then decreased by the withdrawal");
    }

    @Test
    void testCopyCreatesIndependentInstanceForWithdrawCallPhase() {
        WithdrawCallPhase copyPhase = withdrawPhase.copy(dummySpec);

        assertNotSame(withdrawPhase, copyPhase, "Copy should be a distinct instance");
        assertEquals(withdrawPhase.getStartDate().getEpochDay(), copyPhase.getStartDate().getEpochDay(),
                "Start dates should be equal");
        assertEquals(withdrawPhase.getDuration(), copyPhase.getDuration(),
                "Durations should be equal");

        double capital = 10000;

        Withdraw originalAction = withdrawPhase.getWithdraw();
        Withdraw copyAction = copyPhase.getWithdraw();
        assertNotSame(originalAction, copyAction, "Copy should be a distinct instance");
        assertEquals(originalAction.getMonthlyAmount(capital), copyAction.getMonthlyAmount(capital), 0.001,
                "Monthly withdrawal amounts should be equal in the copy");

        originalAction.setMonthlyAmount(6000);

        assertNotEquals(originalAction.getMonthlyAmount(capital), copyAction.getMonthlyAmount(capital), 0.001,
                "Changes to the original withdraw action should not affect the copy");
    }

    @Test
    void testOnMonthEnd_ZeroMonthlyWithdrawal() {
        Withdraw zeroWithdraw = new Withdraw(0, 0);
        WithdrawCallPhase zeroWithdrawPhase = new WithdrawCallPhase(dummySpec, new DummyDate(1000), 30, zeroWithdraw);
        DummyLiveData liveData = (DummyLiveData) dummySpec.getLiveData();

        liveData.setCapital(20000);

        double initialWithdraw = liveData.getWithdraw();
        double initialWithdrawn = liveData.getWithdrawn();
        double initialCapital = liveData.getCapital();
        double initialTax = liveData.getCurrentTax();
        double initialNet = liveData.getCurrentNet();

        zeroWithdrawPhase.onMonthEnd();

        assertEquals(initialWithdraw, liveData.getWithdraw(), 0.001, "Withdraw should remain unchanged");
        assertEquals(initialWithdrawn, liveData.getWithdrawn(), 0.001, "Withdrawn should remain unchanged");
        assertEquals(initialTax, liveData.getCurrentTax(), 0.001, "Current tax should remain unchanged");
        assertEquals(initialNet, liveData.getCurrentNet(), 0.001, "Current net earnings should remain unchanged");
        assertEquals(initialCapital+ liveData.getCurrentReturn(), liveData.getCapital(), 0.001, "Capital should remain unchanged");
    }

    @Test
    void testOnMonthEnd_WithLowOrNegativeCapital_WithInterest() {
        DummyLiveData liveData = (DummyLiveData) dummySpec.getLiveData();

        liveData.setCapital(100);
        liveData.setCurrentReturn(0);

        withdrawPhase.getWithdraw().setMonthlyAmount(0);

        withdrawPhase.onMonthEnd();

        assertEquals(0, liveData.getWithdraw(), 0.001, "Withdraw should be 0 when capital is very low");
        assertEquals(0, liveData.getWithdrawn(), 0.001, "Withdrawn should be 0 when capital is very low");
        assertEquals(0, liveData.getCurrentTax(), 0.001, "Current tax should be 0 when withdrawal is 0");
        assertEquals(0, liveData.getCurrentNet(), 0.001, "Net earnings should be 0 when withdrawal is 0");
        assertEquals(100 + liveData.getCurrentReturn(), liveData.getCapital() , 0.001, "Capital should remain unchanged when withdrawal is 0");

        liveData.setCapital(-500);
        liveData.setCurrentReturn(0);
        withdrawPhase.getWithdraw().setMonthlyAmount(0);

        withdrawPhase.onMonthEnd();

        assertEquals(0, liveData.getWithdraw(), 0.001, "Withdraw should be 0 when capital is negative");
        assertEquals(0, liveData.getWithdrawn(), 0.001, "Withdrawn should be 0 when capital is negative");
        assertEquals(0, liveData.getCurrentTax(), 0.001, "Current tax should be 0 when capital is negative");
        assertEquals(0, liveData.getCurrentNet(), 0.001, "Net earnings should be 0 when capital is negative");
        assertEquals(-500 + liveData.getCurrentReturn(), liveData.getCapital(), 0.001, "Capital should remain unchanged when negative");
    }


}
