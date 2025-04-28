package dk.gormkrings.phase;

import dk.gormkrings.data.ILiveData;
import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import dk.gormkrings.tax.NotionalGainsTax;
import dk.gormkrings.tax.StockExemptionTax;
import dk.gormkrings.tax.TaxExemptionCard;

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

    default void addNotionalTax() {
        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof NotionalGainsTax notionalTax) {
                double taxableAmount = getLiveData().getReturned() - notionalTax.getPreviousReturned();
                double tax = 0;
                if (taxableAmount > 0) {
                    // Adjust the amount of taxable withdrawal
                    taxableAmount = taxExemptionCardToTaxableAmount(taxableAmount);
                    // Apply lower tax rate
                    tax += stockTaxExemptionToTax(tax, taxableAmount);
                    // Adjust the amount of taxable withdrawal
                    taxableAmount = stockTaxExemptionToTaxableAmount(taxableAmount);

                    tax += notionalTax.calculateTax(taxableAmount);
                    if (tax < 0.0001) tax = 0;
                    getLiveData().setCurrentTax(tax);
                    getLiveData().subtractFromCapital(tax);
                    getLiveData().subtractFromReturned(tax);
                    getLiveData().addToTax(tax);
                }
                notionalTax.setPreviousReturned(getLiveData().getReturned());
                break;
            }
        }
    }

    default double taxExemptionCardToTaxableAmount(double taxableAmount) {
        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof TaxExemptionCard taxExemptionCard) {
                double newTaxableAmount = taxableAmount;
                float previousExemption = taxExemptionCard.getCurrentExemption();
                taxExemptionCard.calculateTax(taxableAmount);
                newTaxableAmount -= (taxExemptionCard.getCurrentExemption() - previousExemption);
                if (newTaxableAmount < 0.1) newTaxableAmount  = 0.0;
                return newTaxableAmount;
            }
        }
        return taxableAmount;
    }

    default double stockTaxExemptionToTax(double tax, double taxableWithdrawal) {
        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof StockExemptionTax stockExemptionTax) {
                double newTax = tax;
                float previousExemption = stockExemptionTax.getCurrentExemption();
                newTax += stockExemptionTax.calculateTax(taxableWithdrawal);
                stockExemptionTax.setCurrentExemption(previousExemption);
                if (newTax < 0.1) newTax  = 0.0;
                return newTax;
            }
        }
        return tax;
    }

    default double stockTaxExemptionToTaxableAmount(double taxableAmount) {
        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof StockExemptionTax stockExemptionTax) {
                double newTaxableAmount = taxableAmount;
                float previousExemption = stockExemptionTax.getCurrentExemption();
                stockExemptionTax.calculateTax(newTaxableAmount);
                newTaxableAmount -= (stockExemptionTax.getCurrentExemption() - previousExemption);
                if (newTaxableAmount < 0.1) newTaxableAmount  = 0.0;
                return newTaxableAmount;
            }
        }
        return taxableAmount;
    }

    default void addInflation() {
        IInflation iInflation = getSpecification().getInflation();
        double inflationAmount = iInflation.calculatePercentage();
        getLiveData().addToInflation(inflationAmount);
    }
}

