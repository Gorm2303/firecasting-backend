package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.IWithdraw;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.phase.IWithdrawPhase;
import dk.gormkrings.simulation.data.LiveData;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;
import dk.gormkrings.tax.ITaxRule;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WithdrawPhaseTestHelper {
    public static IWithdrawPhase makePhaseWithRules(
            double startingCapital,
            double deposited,
            double monthlyWithdrawal,
            ITaxRule taxRule,
            ITaxExemption... rules
    ) {
        LiveData liveData = new LiveData(0);
        liveData.addToCapital(startingCapital);
        liveData.addToDeposited(deposited);
        liveData.setWithdraw(monthlyWithdrawal);

        ISpecification spec = mock(ISpecification.class);
        when(spec.getTaxRule()).thenReturn(taxRule);

        IWithdraw withdraw = mock(IWithdraw.class);
        return new IWithdrawPhase() {
            public IWithdraw getWithdraw()       { return withdraw; }
            public ILiveData getLiveData()       { return liveData; }
            public ISpecification getSpecification() { return spec; }
            public List<ITaxExemption> getTaxExemptions()  { return Arrays.asList(rules); }
            // no‐ops for onMonthEnd/onPhaseStart/etc.
        };
    }
}

