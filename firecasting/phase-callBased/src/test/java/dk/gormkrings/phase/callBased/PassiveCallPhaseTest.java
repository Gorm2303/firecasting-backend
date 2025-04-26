package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.Passive;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.data.IDate;
import dk.gormkrings.event.EventType;
import dk.gormkrings.returns.IReturner;
import dk.gormkrings.specification.ISpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PassiveCallPhaseTest {

    @Mock
    private ISpecification specification;
    @Mock
    private IDate startDate;
    @Mock
    private ILiveData liveData;
    @Mock
    private Passive passive;

    private PassiveCallPhase passiveCallPhase;
    private final long duration = 30L;

    @BeforeEach
    public void setup() {
        lenient().when(specification.getLiveData()).thenReturn(liveData);
        passiveCallPhase = new PassiveCallPhase(specification, startDate, new ArrayList<>(), duration, passive);
    }

    @Test
    public void testOnPhaseStart_CallsInitializePreviouslyReturned() {
        double returnedAmount = 600.0;
        double currentReturn = 70.0;
        when(liveData.getReturned()).thenReturn(returnedAmount);
        when(liveData.getCurrentReturn()).thenReturn(currentReturn);

        passiveCallPhase.onPhaseStart();

        verify(liveData).setPhaseName("Passive");
        verify(passive).setPreviouslyReturned(returnedAmount);
        verify(liveData).setPassiveReturn(currentReturn);
        verify(liveData).addToPassiveReturned(currentReturn);
    }

    @Test
    public void testOnDayEnd_CallsCalculatePassive() {
        double returnedAmount = 500.0;
        double previousReturned = 300.0;
        double expectedPassiveReturn = returnedAmount - previousReturned;

        when(liveData.getReturned()).thenReturn(returnedAmount);
        when(passive.getPreviouslyReturned()).thenReturn(previousReturned);
        IReturner returner = mock(IReturner.class);
        when(specification.getReturner()).thenReturn(returner);

        IDate dayAfter = mock(IDate.class);
        when(startDate.plusDays(anyLong())).thenReturn(dayAfter);
        when(dayAfter.getDayOfWeek()).thenReturn(3);

        passiveCallPhase.onDayEnd();

        verify(passive).setPreviouslyReturned(returnedAmount);
        verify(liveData).setPassiveReturn(expectedPassiveReturn);
        verify(liveData).addToPassiveReturned(expectedPassiveReturn);
    }

    @Test
    public void testSupportsEvent() {
        assertTrue(passiveCallPhase.supportsEvent(EventType.MONTH_END), "Should support MONTH_END events");
        assertTrue(passiveCallPhase.supportsEvent(EventType.DAY_END), "Should support DAY_END events");
        assertTrue(passiveCallPhase.supportsEvent(EventType.YEAR_END), "Should support YEAR_END events");
    }

    @Test
    public void testCopy_ReturnsNewInstance() {
        Passive passiveCopy = mock(Passive.class);
        when(passive.copy()).thenReturn(passiveCopy);
        ISpecification specCopy = mock(ISpecification.class);

        PassiveCallPhase copyPhase = passiveCallPhase.copy(specCopy);

        assertNotSame(passiveCallPhase, copyPhase, "copy() should return a new instance");
        assertSame(specCopy, copyPhase.getSpecification(), "Specification should be replaced by the provided copy");
        assertSame(startDate, copyPhase.getStartDate(), "Start date should be preserved");
        assertEquals(duration, copyPhase.getDuration(), "Duration should be preserved");
        assertSame(passiveCopy, copyPhase.getPassive(), "Passive should be the result of passive.copy()");
    }

    @Test
    public void testOnDayEnd_CallsCalculatePassive_EvenIfNonWeekday() {
        IDate plusDate = mock(IDate.class);
        when(startDate.plusDays(anyLong())).thenReturn(plusDate);
        when(plusDate.getDayOfWeek()).thenReturn(6);

        PassiveCallPhase spyPhase = spy(passiveCallPhase);

        spyPhase.onDayEnd();

        verify(spyPhase).calculatePassive();
    }

    @Test
    public void testSupportsEvent_UnsupportedEvent() {
        assertFalse(passiveCallPhase.supportsEvent(EventType.WEEK_END),
                "Should not support WEEK_END events if not implemented");
    }

    @Test
    public void testOnPhaseStart_ExtremeCurrentReturn() {
        when(liveData.getReturned()).thenReturn(1000.0);
        when(liveData.getCurrentReturn()).thenReturn(Double.NaN);

        passiveCallPhase.onPhaseStart();

        verify(liveData).setPassiveReturn(Double.NaN);
        verify(liveData).addToPassiveReturned(Double.NaN);
        verify(passive).setPreviouslyReturned(1000.0);
    }
}
