package dk.gormkrings.phase;

import dk.gormkrings.action.IWithdraw;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.CapitalGainsTax;
import dk.gormkrings.tax.NotionalGainsTax;

public interface IWithdrawPhase extends ISimulationPhase{
    IWithdraw getWithdraw();
    ILiveData getLiveData();
    ISpecification getSpecification();

    default void withdrawMoney() {
        double withdrawAmount = getWithdraw().getMonthlyAmount(getLiveData().getCapital(), getLiveData().getInflation());
        getLiveData().setWithdraw(withdrawAmount);
        getLiveData().addToWithdrawn(withdrawAmount);
        getLiveData().subtractFromCapital(withdrawAmount);
    }

    default void addTax() {
        if (getSpecification().getTaxRule() instanceof CapitalGainsTax capitalTax) {
            double tax = capitalTax.calculateTax(getLiveData().getWithdraw());
            getLiveData().setCurrentTax(tax);
            getLiveData().addToTax(tax);
        }
    }

    default void addNetEarnings() {
        if (getSpecification().getTaxRule() instanceof CapitalGainsTax) {
            double net = getLiveData().getWithdraw() - getLiveData().getCurrentTax();
            getLiveData().addToNetEarnings(net);
            getLiveData().setCurrentNet(net);
        } else if (getSpecification().getTaxRule() instanceof NotionalGainsTax) {
            double net = getLiveData().getWithdraw();
            getLiveData().addToNetEarnings(net);
            getLiveData().setCurrentNet(net);
        }
    }

}
