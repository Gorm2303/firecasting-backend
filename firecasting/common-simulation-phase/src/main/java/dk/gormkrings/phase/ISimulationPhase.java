package dk.gormkrings.phase;

import dk.gormkrings.data.ILiveData;
import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import dk.gormkrings.tax.NotionalGainsTax;

import java.util.List;

public interface ISimulationPhase {
    ISpecification getSpecification();
    ILiveData getLiveData();
    List<ITaxRule> getTaxRules();

    default void addReturn() {
        double r = getSpecification().getReturner().calculateReturn(getLiveData().getCapital());
        getLiveData().setCurrentReturn(r);
        getLiveData().addToReturned(r);
        getLiveData().addToCapital(r);
    }

    default void addTax() {
        if (getSpecification().getTaxRule() instanceof NotionalGainsTax notionalTax) {
            double tax = notionalTax.calculateTax(getLiveData().getReturned() - notionalTax.getPreviousReturned());
            getLiveData().setCurrentTax(tax);
            if (tax <= 0) return;

            getLiveData().subtractFromCapital(tax);
            getLiveData().subtractFromReturned(tax);
            getLiveData().addToTax(tax);
            notionalTax.setPreviousReturned(getLiveData().getReturned());

        }
    }

    default void addInflation() {
        IInflation IInflation = getSpecification().getInflation();
        double inflationAmount = IInflation.calculatePercentage();
        getLiveData().addToInflation(inflationAmount);
    }
}

