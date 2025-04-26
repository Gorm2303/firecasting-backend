package dk.gormkrings.phase.callBased;

import dk.gormkrings.data.IDate;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.event.EventType;
import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.returns.IReturner;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SimulationCallPhaseTest {

    @Test
    public void testOnDayEnd_Weekday() {
        IDate startDate = mock(IDate.class);
        IDate plusDate = mock(IDate.class);
        ISpecification specification = mock(ISpecification.class);
        ILiveData liveData = mock(ILiveData.class);
        IReturner returner = mock(IReturner.class);

        when(liveData.getSessionDuration()).thenReturn(1L);
        when(specification.getLiveData()).thenReturn(liveData);
        when(specification.getReturner()).thenReturn(returner);
        when(startDate.plusDays(1L)).thenReturn(plusDate);
        when(plusDate.getDayOfWeek()).thenReturn(3);

        SimulationCallPhase phase = spy(new SimulationCallPhase(specification, startDate, List.of(mock(ITaxRule.class)), 10, "TestPhase") {
            @Override
            public IPhase copy(ISpecification specificationCopy) {
                return null;
            }
        });

        phase.onDayEnd();
        verify(phase).addReturn();
    }

    @Test
    public void testOnDayEnd_Weekend() {
        IDate startDate = mock(IDate.class);
        IDate plusDate = mock(IDate.class);
        ISpecification specification = mock(ISpecification.class);
        ILiveData liveData = mock(ILiveData.class);

        when(liveData.getSessionDuration()).thenReturn(1L);
        when(specification.getLiveData()).thenReturn(liveData);
        when(startDate.plusDays(1L)).thenReturn(plusDate);
        when(plusDate.getDayOfWeek()).thenReturn(5);

        SimulationCallPhase phase = spy(new SimulationCallPhase(specification, startDate,  List.of(mock(ITaxRule.class)), 10, "TestPhase") {
            @Override
            public IPhase copy(ISpecification specificationCopy) {
                return null;
            }
        });

        phase.onDayEnd();
        verify(phase, never()).addReturn();
    }

    @Test
    public void testOnYearEnd_TaxAndInflationUpdates() {
        IDate startDate = mock(IDate.class);
        ISpecification specification = mock(ISpecification.class);
        ILiveData liveData = mock(ILiveData.class);
        IInflation inflation = mock(IInflation.class);
        when(specification.getLiveData()).thenReturn(liveData);
        when(specification.getInflation()).thenReturn(inflation);

        SimulationCallPhase phase = spy(new SimulationCallPhase(specification, startDate,  List.of(mock(ITaxRule.class)), 10L, "TestPhase") {
            @Override
            public IPhase copy(ISpecification specificationCopy) {
                return null;
            }
        });

        phase.onYearEnd();
        verify(phase).addTax();
        verify(phase).addInflation();
    }


    @Test
    public void testOnPhaseStart_SetsPhaseName() {
        ISpecification specification = mock(ISpecification.class);
        ILiveData liveData = mock(ILiveData.class);
        when(specification.getLiveData()).thenReturn(liveData);

        String phaseName = "TestPhase";

        SimulationCallPhase phase = new SimulationCallPhase(specification, null,  List.of(mock(ITaxRule.class)), 10L, phaseName) {
            @Override
            public IPhase copy(ISpecification specificationCopy) {
                return null;
            }
        };

        phase.onPhaseStart();
        verify(liveData).setPhaseName(phaseName);
    }

    @Test
    public void testSupportsEvent() {
        ISpecification specification = mock(ISpecification.class);
        SimulationCallPhase phase = new SimulationCallPhase(specification, null,  List.of(mock(ITaxRule.class)), 10L, "TestPhase") {
            @Override
            public IPhase copy(ISpecification specificationCopy) {
                return null;
            }
        };

        assertTrue(phase.supportsEvent(EventType.DAY_END), "Should support DAY_END events");
        assertTrue(phase.supportsEvent(EventType.YEAR_END), "Should support YEAR_END events");
        assertFalse(phase.supportsEvent(EventType.MONTH_END), "Should not support MONTH_END events");
    }

    @Test
    public void testPrettyString_DelegatesToLiveDataToString() {
        ISpecification specification = mock(ISpecification.class);
        ILiveData liveData = mock(ILiveData.class);

        String expectedString = "Expected LiveData String";
        when(liveData.toString()).thenReturn(expectedString);
        when(specification.getLiveData()).thenReturn(liveData);

        SimulationCallPhase phase = new SimulationCallPhase(specification, null,  List.of(mock(ITaxRule.class)), 10L, "TestPhase") {
            @Override
            public IPhase copy(ISpecification specificationCopy) {
                return null;
            }
        };

        String result = phase.prettyString();
        assertEquals(expectedString, result);
    }

}
