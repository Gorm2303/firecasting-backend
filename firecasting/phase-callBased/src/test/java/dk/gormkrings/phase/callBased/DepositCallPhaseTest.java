package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.data.IDate;
import dk.gormkrings.event.EventType;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.simulation.util.Formatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DepositCallPhaseTest {

    @Mock
    private ISpecification specification;
    @Mock
    private IDate startDate;
    @Mock
    private ILiveData liveData;
    @Mock
    private Deposit deposit;

    private DepositCallPhase depositCallPhase;
    private final long duration = 30L;

    @BeforeEach
    public void setup() {
        lenient().when(specification.getLiveData()).thenReturn(liveData);
        depositCallPhase = new DepositCallPhase(specification, startDate, duration, deposit);
    }

    @Test
    public void testOnPhaseStart_CallsDepositInitialDeposit() {
        double initialAmount = 500.0;
        when(deposit.getInitial()).thenReturn(initialAmount);

        depositCallPhase.onPhaseStart();

        verify(liveData).setPhaseName("Deposit");
        verify(liveData).addToDeposited(initialAmount);
        verify(liveData).addToCapital(initialAmount);
    }

    @Test
    public void testOnMonthEnd_CallsDepositMoney() {
        double monthlyAmount = 100.0;
        when(deposit.getMonthly()).thenReturn(monthlyAmount);

        depositCallPhase.onMonthEnd();

        verify(liveData).setDeposit(monthlyAmount);
        verify(liveData).addToDeposited(monthlyAmount);
        verify(liveData).addToCapital(monthlyAmount);
    }

    @Test
    public void testSupportsEvent() {
        assertTrue(depositCallPhase.supportsEvent(EventType.MONTH_END), "Should support MONTH_END events");
        assertTrue(depositCallPhase.supportsEvent(EventType.DAY_END), "Should support DAY_END events");
        assertTrue(depositCallPhase.supportsEvent(EventType.YEAR_END), "Should support YEAR_END events");
    }

    @Test
    public void testCopy_ReturnsNewInstance() {
        Deposit depositCopy = mock(Deposit.class);
        when(deposit.copy()).thenReturn(depositCopy);
        ISpecification specCopy = mock(ISpecification.class);

        DepositCallPhase copyPhase = depositCallPhase.copy(specCopy);

        assertNotSame(depositCallPhase, copyPhase, "copy() should return a new instance");
        assertSame(specCopy, copyPhase.getSpecification(), "The specification should be replaced by the provided copy");
        assertSame(startDate, copyPhase.getStartDate(), "Start date should be preserved");
        assertEquals(duration, copyPhase.getDuration(), "Duration should be preserved");
        assertSame(depositCopy, copyPhase.getDeposit(), "Deposit should be the result of deposit.copy()");
    }

    @Test
    public void testSupportsEvent_Unsupported() {
        assertFalse(depositCallPhase.supportsEvent(EventType.WEEK_END),
                "Should not support WEEK_END events if not implemented");
    }
}
