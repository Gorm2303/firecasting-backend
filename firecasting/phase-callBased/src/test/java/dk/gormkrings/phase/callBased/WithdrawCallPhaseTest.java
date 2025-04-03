package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.data.IDate;
import dk.gormkrings.event.EventType;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.CapitalGainsTax;
import dk.gormkrings.tax.NotionalGainsTax;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WithdrawCallPhaseTest {

    @Mock
    private ISpecification specification;
    @Mock
    private IDate startDate;
    @Mock
    private ILiveData liveData;
    @Mock
    private Withdraw withdraw;
    @Mock
    private CapitalGainsTax capitalGainsTax;
    @Mock
    private NotionalGainsTax notionalGainsTax;

    private WithdrawCallPhase withdrawCallPhase;
    private final long duration = 30L;

    @BeforeEach
    public void setup() {
        lenient().when(specification.getLiveData()).thenReturn(liveData);
        withdrawCallPhase = new WithdrawCallPhase(specification, startDate, duration, withdraw);
    }

    @Test
    public void testOnMonthEnd_BasicOperation_CapitalGainsTax() {
        double withdrawAmount = 100.0;
        when(liveData.getCapital()).thenReturn(1000.0);
        when(liveData.getInflation()).thenReturn(2.5);
        when(withdraw.getMonthlyAmount(1000.0, 2.5)).thenReturn(withdrawAmount);
        when(liveData.getWithdraw()).thenReturn(withdrawAmount);
        when(specification.getTaxRule()).thenReturn(capitalGainsTax);
        when(capitalGainsTax.calculateTax(withdrawAmount)).thenReturn(20.0);
        when(liveData.getCurrentTax()).thenReturn(20.0);

        withdrawCallPhase.onMonthEnd();

        verify(liveData).setWithdraw(withdrawAmount);
        verify(liveData).addToWithdrawn(withdrawAmount);
        verify(liveData).subtractFromCapital(withdrawAmount);
        verify(liveData).setCurrentTax(20.0);
        verify(liveData).addToTax(20.0);
        verify(liveData).addToNetEarnings(80.0);
        verify(liveData).setCurrentNet(80.0);
    }

    @Test
    public void testSupportsEvent() {
        assertTrue(withdrawCallPhase.supportsEvent(EventType.MONTH_END), "Should support MONTH_END events");
        assertTrue(withdrawCallPhase.supportsEvent(EventType.DAY_END), "Should support DAY_END events");
        assertTrue(withdrawCallPhase.supportsEvent(EventType.YEAR_END), "Should support YEAR_END events");
    }

    @Test
    public void testCopy_ReturnsNewInstance() {
        Withdraw withdrawCopy = mock(Withdraw.class);
        when(withdraw.copy()).thenReturn(withdrawCopy);
        ISpecification specCopy = mock(ISpecification.class);

        WithdrawCallPhase copyPhase = withdrawCallPhase.copy(specCopy);

        assertNotSame(withdrawCallPhase, copyPhase, "copy() should return a new instance");
        assertSame(specCopy, copyPhase.getSpecification(), "Specification should be replaced by provided copy");
        assertSame(startDate, copyPhase.getStartDate(), "Start date should be preserved");
        assertEquals(duration, copyPhase.getDuration(), "Duration should be preserved");
        assertSame(withdrawCopy, copyPhase.getWithdraw(), "Withdraw should be the result of withdraw.copy()");
    }

    @Test
    public void testAddNetEarnings_NotionalGainsTax() {
        double withdrawAmount = 100.0;
        when(specification.getTaxRule()).thenReturn(notionalGainsTax);
        when(liveData.getWithdraw()).thenReturn(withdrawAmount);

        withdrawCallPhase.addNetEarnings();

        verify(liveData).addToNetEarnings(withdrawAmount);
        verify(liveData).setCurrentNet(withdrawAmount);
    }

    @Test
    public void testWithdrawMoney_ExceedsCapital_NoClamping() {
        when(liveData.getCapital()).thenReturn(500.0);
        when(liveData.getInflation()).thenReturn(2.5);
        when(withdraw.getMonthlyAmount(500.0, 2.5)).thenReturn(600.0);
        when(liveData.getWithdraw()).thenReturn(600.0);
        when(specification.getTaxRule()).thenReturn(capitalGainsTax);
        when(capitalGainsTax.calculateTax(600.0)).thenReturn(60.0);
        when(liveData.getCurrentTax()).thenReturn(60.0);

        withdrawCallPhase.onMonthEnd();

        verify(liveData).setWithdraw(600.0);
        verify(liveData).addToWithdrawn(600.0);
        verify(liveData).subtractFromCapital(600.0);
        verify(liveData).setCurrentTax(60.0);
        verify(liveData).addToTax(60.0);
        verify(liveData).addToNetEarnings(540.0);
        verify(liveData).setCurrentNet(540.0);
    }

    @Test
    public void testOnMonthEnd_OrderOfOperations() {
        double withdrawAmount = 100.0;
        when(liveData.getCapital()).thenReturn(1000.0);
        when(liveData.getInflation()).thenReturn(2.5);
        when(withdraw.getMonthlyAmount(1000.0, 2.5)).thenReturn(withdrawAmount);
        when(liveData.getWithdraw()).thenReturn(withdrawAmount);
        when(specification.getTaxRule()).thenReturn(capitalGainsTax);
        when(capitalGainsTax.calculateTax(withdrawAmount)).thenReturn(20.0);
        when(liveData.getCurrentTax()).thenReturn(20.0);

        withdrawCallPhase.onMonthEnd();

        InOrder inOrder = inOrder(liveData);
        inOrder.verify(liveData).setWithdraw(withdrawAmount);
        inOrder.verify(liveData).addToWithdrawn(withdrawAmount);
        inOrder.verify(liveData).subtractFromCapital(withdrawAmount);
        inOrder.verify(liveData).setCurrentTax(20.0);
        inOrder.verify(liveData).addToTax(20.0);
        inOrder.verify(liveData).addToNetEarnings(80.0);
        inOrder.verify(liveData).setCurrentNet(80.0);
    }
}
