package dk.gormkrings.phase.callBased;

import dk.gormkrings.action.IWithdraw;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.phase.IWithdrawPhase;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;

public class WithdrawPhaseTestHelper {
    public static IWithdrawPhase makePhaseWithRules(
            double startingCapital,
            double deposited,
            double monthlyWithdrawal,
            ITaxRule... rules
    ) {
        // Fake ILiveData that holds capital, deposited, etc.
        ILiveData liveData = mock(ILiveData.class);
        ISpecification spec = mock(ISpecification.class);
                IWithdraw withdraw = mock(IWithdraw.class);
        DepositCallPhase dummy = null; // you can use a minimal subclass:
        return new IWithdrawPhase() {
            public IWithdraw getWithdraw()       { return withdraw; }
            public ILiveData getLiveData()       { return liveData; }
            public ISpecification getSpecification() { return spec; }
            public List<ITaxRule> getTaxRules()  { return Arrays.asList(rules); }
            // no‐ops for onMonthEnd/onPhaseStart/etc.
        };
    }
}

