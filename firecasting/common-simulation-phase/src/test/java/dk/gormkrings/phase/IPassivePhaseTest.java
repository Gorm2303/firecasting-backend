package dk.gormkrings.phase;

import dk.gormkrings.action.Passive;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class IPassivePhaseTest {
    Passive passive = mock(Passive.class);
    ILiveData liveData = mock(ILiveData.class);
    IPassivePhase passivePhase;

    @BeforeEach
    public void setup() {
        passivePhase = new IPassivePhase() {
            @Override
            public Passive getPassive() {
                return passive;
            }

            @Override
            public ISpecification getSpecification() {
                return null;
            }

            @Override
            public ILiveData getLiveData() {
                return liveData;
            }

            @Override
            public List<ITaxExemption> getTaxExemptions() {
                return List.of();
            }

            @Override
            public void addReturn() {
                IPassivePhase.super.addReturn();
            }

            @Override
            public void addNotionalTax() {
                IPassivePhase.super.addNotionalTax();
            }

            @Override
            public void compoundInflation() {
                IPassivePhase.super.compoundInflation();
            }
        };
    }

    @Test
    public void testCalculatePassive_BasicOperation() {
        when(liveData.getReturned()).thenReturn(500.0);
        when(passive.getPreviouslyReturned()).thenReturn(300.0);

        passivePhase.calculatePassive();

        verify(passive).setPreviouslyReturned(500.0);
        verify(liveData).setPassiveReturn(200.0);
        verify(liveData).addToPassiveReturned(200.0);
    }

    @Test
    public void testCalculatePassive_ZeroDifference() {
        when(liveData.getReturned()).thenReturn(400.0);
        when(passive.getPreviouslyReturned()).thenReturn(400.0);

        passivePhase.calculatePassive();

        verify(passive).setPreviouslyReturned(400.0);
        verify(liveData).setPassiveReturn(0.0);
        verify(liveData).addToPassiveReturned(0.0);
    }

    @Test
    public void testCalculatePassive_NegativeDifference() {
        when(liveData.getReturned()).thenReturn(300.0);
        when(passive.getPreviouslyReturned()).thenReturn(500.0);

        passivePhase.calculatePassive();

        verify(passive).setPreviouslyReturned(300.0);
        verify(liveData).setPassiveReturn(-200.0);
        verify(liveData).addToPassiveReturned(-200.0);
    }

    @Test
    public void testCalculatePassive_HighValues() {
        double highReturned = 1e9;
        double highPrevious = 5e8;
        double expectedPassiveReturn = highReturned - highPrevious;
        when(liveData.getReturned()).thenReturn(highReturned);
        when(passive.getPreviouslyReturned()).thenReturn(highPrevious);

        passivePhase.calculatePassive();

        verify(passive).setPreviouslyReturned(highReturned);
        verify(liveData).setPassiveReturn(expectedPassiveReturn);
        verify(liveData).addToPassiveReturned(expectedPassiveReturn);
    }

    @Test
    public void testInitializePreviouslyReturned_BasicOperation() {
        when(liveData.getReturned()).thenReturn(600.0);
        when(liveData.getCurrentReturn()).thenReturn(70.0);

        passivePhase.initializePreviouslyReturned();

        verify(passive).setPreviouslyReturned(600.0);
        verify(liveData).setPassiveReturn(70.0);
        verify(liveData).addToPassiveReturned(70.0);
    }

    @Test
    public void testInitializePreviouslyReturned_ZeroCurrentReturn() {
        when(liveData.getReturned()).thenReturn(600.0);
        when(liveData.getCurrentReturn()).thenReturn(0.0);

        passivePhase.initializePreviouslyReturned();

        verify(passive).setPreviouslyReturned(600.0);
        verify(liveData).setPassiveReturn(0.0);
        verify(liveData).addToPassiveReturned(0.0);
    }

    @Test
    public void testInitializePreviouslyReturned_HighValues() {
        double highReturned = 1e9;
        double highCurrentReturn = 5e7;
        when(liveData.getReturned()).thenReturn(highReturned);
        when(liveData.getCurrentReturn()).thenReturn(highCurrentReturn);

        passivePhase.initializePreviouslyReturned();

        verify(passive).setPreviouslyReturned(highReturned);
        verify(liveData).setPassiveReturn(highCurrentReturn);
        verify(liveData).addToPassiveReturned(highCurrentReturn);
    }

    @Test
    public void testInitializePreviouslyReturned_NegativeCurrentReturn() {
        when(liveData.getReturned()).thenReturn(600.0);
        when(liveData.getCurrentReturn()).thenReturn(-80.0);

        passivePhase.initializePreviouslyReturned();

        verify(passive).setPreviouslyReturned(600.0);
        verify(liveData).setPassiveReturn(-80.0);
        verify(liveData).addToPassiveReturned(-80.0);
    }

    @Test
    public void testCalculatePassive_NaNDifference() {
        when(liveData.getReturned()).thenReturn(Double.NaN);
        when(passive.getPreviouslyReturned()).thenReturn(300.0);

        passivePhase.calculatePassive();

        verify(passive).setPreviouslyReturned(Double.NaN);
        verify(liveData).setPassiveReturn(Double.NaN);
        verify(liveData).addToPassiveReturned(Double.NaN);
    }

    @Test
    public void testCalculatePassive_InfiniteReturned() {
        when(liveData.getReturned()).thenReturn(Double.POSITIVE_INFINITY);
        when(passive.getPreviouslyReturned()).thenReturn(1000.0);

        passivePhase.calculatePassive();

        verify(passive).setPreviouslyReturned(Double.POSITIVE_INFINITY);
        verify(liveData).setPassiveReturn(Double.POSITIVE_INFINITY);
        verify(liveData).addToPassiveReturned(Double.POSITIVE_INFINITY);
    }

    @Test
    public void testCalculatePassive_NegativeInfiniteReturned() {
        when(liveData.getReturned()).thenReturn(Double.NEGATIVE_INFINITY);
        when(passive.getPreviouslyReturned()).thenReturn(1000.0);

        passivePhase.calculatePassive();

        verify(passive).setPreviouslyReturned(Double.NEGATIVE_INFINITY);
        verify(liveData).setPassiveReturn(Double.NEGATIVE_INFINITY);
        verify(liveData).addToPassiveReturned(Double.NEGATIVE_INFINITY);
    }

    @Test
    public void testInitializePreviouslyReturned_NaNCurrentReturn() {
        when(liveData.getReturned()).thenReturn(600.0);
        when(liveData.getCurrentReturn()).thenReturn(Double.NaN);

        passivePhase.initializePreviouslyReturned();

        verify(passive).setPreviouslyReturned(600.0);
        verify(liveData).setPassiveReturn(Double.NaN);
        verify(liveData).addToPassiveReturned(Double.NaN);
    }

    @Test
    public void testInitializePreviouslyReturned_InfiniteCurrentReturn() {
        when(liveData.getReturned()).thenReturn(600.0);
        when(liveData.getCurrentReturn()).thenReturn(Double.POSITIVE_INFINITY);

        passivePhase.initializePreviouslyReturned();

        verify(passive).setPreviouslyReturned(600.0);
        verify(liveData).setPassiveReturn(Double.POSITIVE_INFINITY);
        verify(liveData).addToPassiveReturned(Double.POSITIVE_INFINITY);
    }

    @Test
    public void testMultipleSequentialCalls_CalculatePassive() {
        when(liveData.getReturned()).thenReturn(500.0);
        when(passive.getPreviouslyReturned()).thenReturn(300.0);
        passivePhase.calculatePassive();
        verify(passive).setPreviouslyReturned(500.0);
        verify(liveData).setPassiveReturn(200.0);
        verify(liveData).addToPassiveReturned(200.0);

        reset(passive, liveData);
        when(liveData.getReturned()).thenReturn(550.0);
        when(passive.getPreviouslyReturned()).thenReturn(500.0);
        passivePhase.calculatePassive();
        verify(passive).setPreviouslyReturned(550.0);
        verify(liveData).setPassiveReturn(50.0);
        verify(liveData).addToPassiveReturned(50.0);
    }

}
