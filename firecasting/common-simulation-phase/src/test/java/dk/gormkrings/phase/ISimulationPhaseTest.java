package dk.gormkrings.phase;

import dk.gormkrings.data.ILiveData;
import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.returns.IReturner;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import dk.gormkrings.tax.NotionalGainsTax;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class ISimulationPhaseTest {
    ISpecification specification = mock(ISpecification.class);
    ILiveData liveData = mock(ILiveData.class);
    IInflation inflation = mock(IInflation.class);
    IReturner returner = mock(IReturner.class);
    ITaxRule taxRule = mock(ITaxRule.class);
    NotionalGainsTax notionalGainsTax = mock(NotionalGainsTax.class);
    ISimulationPhase simulationPhase;

    @BeforeEach
    public void setup() {
        simulationPhase = new ISimulationPhase() {
            @Override
            public ISpecification getSpecification() {
                return specification;
            }
            @Override
            public ILiveData getLiveData() {
                return liveData;
            }

            @Override
            public List<ITaxRule> getTaxRules() {
                return List.of(taxRule);
            }
        };
    }

    @Test
    public void testAddReturn() {
        when(liveData.getCapital()).thenReturn(1000.0);
        when(returner.calculateReturn(1000.0)).thenReturn(50.0);
        when(specification.getReturner()).thenReturn(returner);

        simulationPhase.addReturn();

        verify(liveData).setCurrentReturn(50.0);
        verify(liveData).addToReturned(50.0);
        verify(liveData).addToCapital(50.0);
    }

    @Test
    public void testAddTax_NonNotionalGainsTax_UsingMock() {
        when(simulationPhase.getTaxRules().getFirst()).thenReturn(taxRule);

        simulationPhase.addTax();

        verify(liveData, never()).setCurrentTax(anyDouble());
        verify(liveData, never()).subtractFromCapital(anyDouble());
        verify(liveData, never()).subtractFromReturned(anyDouble());
        verify(liveData, never()).addToTax(anyDouble());
    }

    @Test
    public void testAddTax_ZeroOrNegativeTax() {
        when(simulationPhase.getTaxRules().getFirst()).thenReturn(notionalGainsTax);

        when(liveData.getReturned()).thenReturn(100.0);
        when(notionalGainsTax.getPreviousReturned()).thenReturn(100.0);
        when(notionalGainsTax.calculateTax(0.0)).thenReturn(0.0);

        simulationPhase.addTax();

        verify(liveData).setCurrentTax(0.0);
        verify(liveData, never()).subtractFromCapital(anyDouble());
        verify(liveData, never()).subtractFromReturned(anyDouble());
        verify(liveData, never()).addToTax(anyDouble());
        verify(notionalGainsTax, never()).setPreviousReturned(anyDouble());
    }

    @Test
    public void testAddTax_PositiveTax() {
        when(simulationPhase.getTaxRules().getFirst()).thenReturn(notionalGainsTax);

        when(liveData.getReturned()).thenReturn(120.0);
        when(notionalGainsTax.getPreviousReturned()).thenReturn(100.0);
        when(notionalGainsTax.calculateTax(20.0)).thenReturn(20.0);

        simulationPhase.addTax();

        verify(liveData).setCurrentTax(20.0);
        verify(liveData).subtractFromCapital(20.0);
        verify(liveData).subtractFromReturned(20.0);
        verify(liveData).addToTax(20.0);
        verify(notionalGainsTax).setPreviousReturned(120.0);
    }

    @Test
    public void testAddInflation_BasicInflationUpdate() {
        when(inflation.calculatePercentage()).thenReturn(2.5);
        when(specification.getInflation()).thenReturn(inflation);
        simulationPhase.addInflation();

        verify(liveData).addToInflation(2.5);
    }

    @Test
    public void testAddReturn_NegativeReturn() {
        when(liveData.getCapital()).thenReturn(1000.0);
        when(returner.calculateReturn(1000.0)).thenReturn(-50.0);
        when(specification.getReturner()).thenReturn(returner);

        simulationPhase.addReturn();

        verify(liveData).setCurrentReturn(-50.0);
        verify(liveData).addToReturned(-50.0);
        verify(liveData).addToCapital(-50.0);
    }

    @Test
    public void testAddTax_NegativeTax() {
        when(simulationPhase.getTaxRules().getFirst()).thenReturn(notionalGainsTax);
        // Set up a scenario where the difference is negative: 100 - 120 = -20.
        when(liveData.getReturned()).thenReturn(100.0);
        when(notionalGainsTax.getPreviousReturned()).thenReturn(120.0);
        when(notionalGainsTax.calculateTax(-20.0)).thenReturn(-20.0);

        simulationPhase.addTax();

        verify(liveData).setCurrentTax(-20.0);
        verify(liveData, never()).subtractFromCapital(anyDouble());
        verify(liveData, never()).subtractFromReturned(anyDouble());
        verify(liveData, never()).addToTax(anyDouble());
        verify(notionalGainsTax, never()).setPreviousReturned(anyDouble());
    }

    @Test
    public void testAddInflation_NegativeInflation() {
        when(inflation.calculatePercentage()).thenReturn(-1.5);
        when(specification.getInflation()).thenReturn(inflation);

        simulationPhase.addInflation();

        verify(liveData).addToInflation(-1.5);
    }

    @Test
    public void testAddInflation_ZeroInflation() {
        when(inflation.calculatePercentage()).thenReturn(0.0);
        when(specification.getInflation()).thenReturn(inflation);

        simulationPhase.addInflation();

        verify(liveData).addToInflation(0.0);
    }

    @Test
    public void testAddReturn_ZeroCapital() {
        // If capital is zero, we assume the return should be zero.
        when(liveData.getCapital()).thenReturn(0.0);
        when(returner.calculateReturn(0.0)).thenReturn(0.0);
        when(specification.getReturner()).thenReturn(returner);

        simulationPhase.addReturn();

        verify(liveData).setCurrentReturn(0.0);
        verify(liveData).addToReturned(0.0);
        verify(liveData).addToCapital(0.0);
    }

    @Test
    public void testAddTax_ZeroDifferenceWithZeros() {
        // When both returned and previous returned are zero, the tax should be zero.
        when(simulationPhase.getTaxRules().getFirst()).thenReturn(notionalGainsTax);
        when(liveData.getReturned()).thenReturn(0.0);
        when(notionalGainsTax.getPreviousReturned()).thenReturn(0.0);
        when(notionalGainsTax.calculateTax(0.0)).thenReturn(0.0);

        simulationPhase.addTax();

        verify(liveData).setCurrentTax(0.0);
        verify(liveData, never()).subtractFromCapital(anyDouble());
        verify(liveData, never()).subtractFromReturned(anyDouble());
        verify(liveData, never()).addToTax(anyDouble());
        verify(notionalGainsTax, never()).setPreviousReturned(anyDouble());
    }

    @Test
    public void testAddReturn_HighCapital() {
        // Simulate extremely high capital value.
        double highCapital = 1e9; // 1 billion
        double highReturn = 5e7; // e.g., 50 million
        when(liveData.getCapital()).thenReturn(highCapital);
        when(returner.calculateReturn(highCapital)).thenReturn(highReturn);
        when(specification.getReturner()).thenReturn(returner);

        simulationPhase.addReturn();

        verify(liveData).setCurrentReturn(highReturn);
        verify(liveData).addToReturned(highReturn);
        verify(liveData).addToCapital(highReturn);
    }

    @Test
    public void testAddTax_HighPositiveTax() {
        // Simulate a case with extremely high tax values.
        when(simulationPhase.getTaxRules().getFirst()).thenReturn(notionalGainsTax);
        // liveData.getReturned() is very high compared to previous returned.
        when(liveData.getReturned()).thenReturn(1e9);
        when(notionalGainsTax.getPreviousReturned()).thenReturn(5e8); // 500 million difference => 500 million
        when(notionalGainsTax.calculateTax(5e8)).thenReturn(5e7); // 50 million tax

        simulationPhase.addTax();

        verify(liveData).setCurrentTax(5e7);
        verify(liveData).subtractFromCapital(5e7);
        verify(liveData).subtractFromReturned(5e7);
        verify(liveData).addToTax(5e7);
        verify(notionalGainsTax).setPreviousReturned(1e9);
    }

    @Test
    public void testAddInflation_HighInflation() {
        // Simulate a very high inflation rate.
        when(inflation.calculatePercentage()).thenReturn(100.0);
        when(specification.getInflation()).thenReturn(inflation);

        simulationPhase.addInflation();

        verify(liveData).addToInflation(100.0);
    }


}
