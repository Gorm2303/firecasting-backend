package dk.gormkrings.phase;

import dk.gormkrings.action.Withdraw;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IWithdrawPhaseTest {

    @Mock
    private ISpecification specification;
    @Mock
    private Withdraw withdraw;
    @Mock
    private ILiveData liveData;
    @Mock
    private CapitalGainsTax capitalGainsTax;
    @Mock
    private NotionalGainsTax notionalGainsTax;
    private IWithdrawPhase withdrawPhase;

    @BeforeEach
    public void setup() {
        withdrawPhase = new IWithdrawPhase() {
            @Override
            public Withdraw getWithdraw() {
                return withdraw;
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
            public ISpecification getSpecification() {
                return specification;
            }

            @Override
            public void withdrawMoney() {
                IWithdrawPhase.super.withdrawMoney();
            }

            @Override
            public void addCapitalTax() {
                IWithdrawPhase.super.addCapitalTax();
            }

            @Override
            public void addNetEarnings() {
                IWithdrawPhase.super.addNetEarnings();
            }
        };
    }

    @Test
    public void testWithdrawMoney_BasicOperation_CapitalGainsTax() {
        when(liveData.getCapital()).thenReturn(1000.0);
        when(liveData.getInflation()).thenReturn(2.5);
        when(withdraw.getMonthlyAmount(1000.0, 2.5)).thenReturn(100.0);

        withdrawPhase.withdrawMoney();

        verify(liveData).setWithdraw(100.0);
        verify(liveData).addToWithdrawn(100.0);
        verify(liveData).subtractFromCapital(100.0);
    }

    @Test
    public void testAddTax_CapitalGainsTax() {
        when(specification.getTaxRule()).thenReturn(capitalGainsTax);
        when(liveData.getWithdraw()).thenReturn(100.0);
        when(capitalGainsTax.calculateTax(100.0)).thenReturn(15.0);

        withdrawPhase.addCapitalTax();

        verify(liveData).setCurrentTax(15.0);
        verify(liveData).addToTax(15.0);
    }

    @Test
    public void testAddNetEarnings_CapitalGainsTax() {
        when(specification.getTaxRule()).thenReturn(capitalGainsTax);
        when(liveData.getWithdraw()).thenReturn(100.0);
        when(liveData.getCurrentTax()).thenReturn(20.0);

        withdrawPhase.addNetEarnings();

        verify(liveData).addToNetEarnings(80.0);
        verify(liveData).setCurrentNet(80.0);
    }

    @Test
    public void testAddNetEarnings_NotionalGainsTax() {
        when(specification.getTaxRule()).thenReturn(notionalGainsTax);
        when(liveData.getWithdraw()).thenReturn(100.0);

        withdrawPhase.addNetEarnings();

        verify(liveData).addToNetEarnings(100.0);
        verify(liveData).setCurrentNet(100.0);
    }

    @Test
    public void testWithdrawMoney_ZeroWithdrawal() {
        when(liveData.getCapital()).thenReturn(1000.0);
        when(liveData.getInflation()).thenReturn(2.5);
        when(withdraw.getMonthlyAmount(1000.0, 2.5)).thenReturn(0.0);

        withdrawPhase.withdrawMoney();

        verify(liveData).setWithdraw(0.0);
        verify(liveData).addToWithdrawn(0.0);
        verify(liveData).subtractFromCapital(0.0);
    }

    @Test
    public void testWithdrawMoney_NegativeWithdrawal() {
        when(liveData.getCapital()).thenReturn(1000.0);
        when(liveData.getInflation()).thenReturn(2.5);
        when(withdraw.getMonthlyAmount(1000.0, 2.5)).thenReturn(-50.0);

        withdrawPhase.withdrawMoney();

        verify(liveData).setWithdraw(-50.0);
        verify(liveData).addToWithdrawn(-50.0);
        verify(liveData).subtractFromCapital(-50.0);
    }

    @Test
    public void testAddNetEarnings_CapitalGainsTax_ZeroNet() {
        when(specification.getTaxRule()).thenReturn(capitalGainsTax);
        when(liveData.getWithdraw()).thenReturn(100.0);
        when(liveData.getCurrentTax()).thenReturn(100.0);

        withdrawPhase.addNetEarnings();

        verify(liveData).addToNetEarnings(0.0);
        verify(liveData).setCurrentNet(0.0);
    }

    @Test
    public void testWithdrawMoney_HighValues() {
        when(liveData.getCapital()).thenReturn(1e9);
        when(liveData.getInflation()).thenReturn(2.5);
        when(withdraw.getMonthlyAmount(1e9, 2.5)).thenReturn(1e8);

        withdrawPhase.withdrawMoney();

        verify(liveData).setWithdraw(1e8);
        verify(liveData).addToWithdrawn(1e8);
        verify(liveData).subtractFromCapital(1e8);
    }

    @Test
    public void testWithdrawMoney_PositiveInfinity() {
        when(liveData.getCapital()).thenReturn(1000.0);
        when(liveData.getInflation()).thenReturn(2.5);
        when(withdraw.getMonthlyAmount(1000.0, 2.5)).thenReturn(Double.POSITIVE_INFINITY);

        withdrawPhase.withdrawMoney();

        verify(liveData).setWithdraw(Double.POSITIVE_INFINITY);
        verify(liveData).addToWithdrawn(Double.POSITIVE_INFINITY);
        verify(liveData).subtractFromCapital(Double.POSITIVE_INFINITY);
    }

    @Test
    public void testWithdrawMoney_NaN() {
        when(liveData.getCapital()).thenReturn(1000.0);
        when(liveData.getInflation()).thenReturn(2.5);
        when(withdraw.getMonthlyAmount(1000.0, 2.5)).thenReturn(Double.NaN);

        withdrawPhase.withdrawMoney();

        verify(liveData).setWithdraw(Double.NaN);
        verify(liveData).addToWithdrawn(Double.NaN);
        verify(liveData).subtractFromCapital(Double.NaN);
    }

    @Test
    public void testMultipleSequentialCalls_WithdrawMoney() {
        when(liveData.getCapital()).thenReturn(1000.0);
        when(liveData.getInflation()).thenReturn(2.5);
        when(withdraw.getMonthlyAmount(1000.0, 2.5)).thenReturn(100.0);

        withdrawPhase.withdrawMoney();

        reset(liveData);
        when(liveData.getCapital()).thenReturn(900.0);
        when(liveData.getInflation()).thenReturn(2.5);
        when(withdraw.getMonthlyAmount(900.0, 2.5)).thenReturn(90.0);

        withdrawPhase.withdrawMoney();

        verify(liveData).setWithdraw(90.0);
        verify(liveData).addToWithdrawn(90.0);
        verify(liveData).subtractFromCapital(90.0);
    }

    @Test
    public void testAddTax_UnknownTaxRule() {
        when(withdrawPhase.getTaxExemptions().getFirst()).thenReturn(new ITaxExemption() {
            @Override
            public double calculateTax(double amount) {
                return 0;
            }

            @Override
            public ITaxExemption copy() {
                return null;
            }

            @Override
            public void yearlyUpdate() {

            }
        });

        withdrawPhase.addCapitalTax();

        verify(liveData, never()).setCurrentTax(anyDouble());
        verify(liveData, never()).addToTax(anyDouble());
    }

    @Test
    public void testAddNetEarnings_UnknownTaxRule() {
        when(withdrawPhase.getTaxExemptions().getFirst()).thenReturn(new ITaxExemption() {
            @Override
            public double calculateTax(double amount) {
                return 0;
            }

            @Override
            public ITaxExemption copy() {
                return null;
            }

            @Override
            public void yearlyUpdate() {

            }
        });

        withdrawPhase.addNetEarnings();

        verify(liveData, never()).addToNetEarnings(anyDouble());
        verify(liveData, never()).setCurrentNet(anyDouble());
    }

}
