package dk.gormkrings.phase;

import dk.gormkrings.action.Deposit;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class IDepositPhaseTest {
    Deposit deposit = mock(Deposit.class);
    ILiveData liveData = mock(ILiveData.class);
    IDepositPhase depositPhase;

    @BeforeEach
    public void setup() {
        depositPhase = new IDepositPhase() {
            @Override
            public Deposit getDeposit() {
                return deposit;
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
            public List<ITaxRule> getTaxRules() {
                return List.of();
            }

            @Override
            public void addReturn() {
                IDepositPhase.super.addReturn();
            }

            @Override
            public void addNotionalTax() {
                IDepositPhase.super.addNotionalTax();
            }

            @Override
            public void addInflation() {
                IDepositPhase.super.addInflation();
            }
        };
    }

    @Test
    public void testDepositMoney_BasicDepositOperation() {
        when(deposit.getMonthly()).thenReturn(100.0);

        depositPhase.depositMoney();

        verify(liveData).setDeposit(100.0);
        verify(liveData).addToDeposited(100.0);
        verify(liveData).addToCapital(100.0);
    }

    @Test
    public void testDepositInitialDeposit_BasicInitialDeposit() {
        when(deposit.getInitial()).thenReturn(500.0);

        depositPhase.depositInitialDeposit();

        verify(liveData).addToDeposited(500.0);
        verify(liveData).addToCapital(500.0);
    }

    @Test
    public void testDepositMoney_ZeroDeposit() {
        when(deposit.getMonthly()).thenReturn(0.0);

        depositPhase.depositMoney();

        verify(liveData).setDeposit(0.0);
        verify(liveData).addToDeposited(0.0);
        verify(liveData).addToCapital(0.0);
    }

    @Test
    public void testDepositMoney_NegativeDeposit() {
        when(deposit.getMonthly()).thenReturn(-50.0);

        depositPhase.depositMoney();

        verify(liveData).setDeposit(-50.0);
        verify(liveData).addToDeposited(-50.0);
        verify(liveData).addToCapital(-50.0);
    }

    @Test
    public void testDepositInitialDeposit_ZeroDeposit() {
        when(deposit.getInitial()).thenReturn(0.0);

        depositPhase.depositInitialDeposit();

        verify(liveData).addToDeposited(0.0);
        verify(liveData).addToCapital(0.0);
    }

    @Test
    public void testDepositInitialDeposit_NegativeDeposit() {
        when(deposit.getInitial()).thenReturn(-200.0);

        depositPhase.depositInitialDeposit();

        verify(liveData).addToDeposited(-200.0);
        verify(liveData).addToCapital(-200.0);
    }

    @Test
    public void testDepositMoney_HighDeposit() {
        when(deposit.getMonthly()).thenReturn(1e9);

        depositPhase.depositMoney();

        verify(liveData).setDeposit(1e9);
        verify(liveData).addToDeposited(1e9);
        verify(liveData).addToCapital(1e9);
    }

    @Test
    public void testDepositInitialDeposit_HighDeposit() {
        when(deposit.getInitial()).thenReturn(1e9);

        depositPhase.depositInitialDeposit();

        verify(liveData).addToDeposited(1e9);
        verify(liveData).addToCapital(1e9);
    }

    @Test
    public void testDepositMoney_PositiveInfinity() {
        when(deposit.getMonthly()).thenReturn(Double.POSITIVE_INFINITY);

        depositPhase.depositMoney();

        verify(liveData).setDeposit(Double.POSITIVE_INFINITY);
        verify(liveData).addToDeposited(Double.POSITIVE_INFINITY);
        verify(liveData).addToCapital(Double.POSITIVE_INFINITY);
    }

    @Test
    public void testDepositMoney_NegativeInfinity() {
        when(deposit.getMonthly()).thenReturn(Double.NEGATIVE_INFINITY);

        depositPhase.depositMoney();

        verify(liveData).setDeposit(Double.NEGATIVE_INFINITY);
        verify(liveData).addToDeposited(Double.NEGATIVE_INFINITY);
        verify(liveData).addToCapital(Double.NEGATIVE_INFINITY);
    }

    @Test
    public void testDepositMoney_NaN() {
        when(deposit.getMonthly()).thenReturn(Double.NaN);

        depositPhase.depositMoney();

        verify(liveData).setDeposit(Double.NaN);
        verify(liveData).addToDeposited(Double.NaN);
        verify(liveData).addToCapital(Double.NaN);
    }

    @Test
    public void testMultipleSequentialCalls_DepositMoney() {
        when(deposit.getMonthly()).thenReturn(100.0);

        depositPhase.depositMoney();
        depositPhase.depositMoney();

        verify(liveData, times(2)).setDeposit(100.0);
        verify(liveData, times(2)).addToDeposited(100.0);
        verify(liveData, times(2)).addToCapital(100.0);
    }

}
