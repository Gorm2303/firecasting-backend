package dk.gormkrings.phase;

import dk.gormkrings.action.IWithdraw;
import dk.gormkrings.data.ILiveData;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.*;

public interface IWithdrawPhase extends ISimulationPhase {
    IWithdraw getWithdraw();
    ILiveData getLiveData();
    ISpecification getSpecification();

    default void withdrawMoney() {
        double withdrawAmount = getWithdraw().getMonthlyAmount(getLiveData().getCapital(), getLiveData().getInflation());
        withdrawAmount += getWithdraw().getDynamicAmountOfReturn();
        if (withdrawAmount <= 0) withdrawAmount = 0;
        if (withdrawAmount > getLiveData().getCapital()) withdrawAmount = getLiveData().getCapital();

        getLiveData().setWithdraw(withdrawAmount);

        double gainOfCapitalRate = 1 - (getLiveData().getDeposited() / getLiveData().getCapital());
        getLiveData().subtractFromDeposited((1 - gainOfCapitalRate) * getLiveData().getWithdraw());

        double estimateTaxRate = estimateTaxRate(withdrawAmount);
        withdrawAmount = withdrawAmount / (1 - estimateTaxRate);
        getLiveData().setWithdraw(withdrawAmount);

        getLiveData().addToWithdrawn(withdrawAmount);
        getLiveData().subtractFromCapital(withdrawAmount);
    }

    default double estimateTaxRate(double withdraw) {
        if (withdraw <= 0.0) {
            return 0.0;
        }
        double gainOfCapitalRate = 1 - (getLiveData().getDeposited() / getLiveData().getCapital());
        double taxableWithdrawal = withdraw * gainOfCapitalRate;
        double tax = 0;

        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof TaxExemptionCard taxExemptionCard) {
                float previousExemption = taxExemptionCard.getCurrentExemption();
                taxExemptionCard.calculateTax(taxableWithdrawal);
                taxableWithdrawal -= (taxExemptionCard.getCurrentExemption() - previousExemption);
                taxExemptionCard.setCurrentExemption(previousExemption);
                if (taxableWithdrawal < 0.1) taxableWithdrawal  = 0.0;
                break;
            }
        }

        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof StockExemptionTax stockExemptionTax) {
                float previousExemption = stockExemptionTax.getCurrentExemption();
                tax += stockExemptionTax.calculateTax(taxableWithdrawal);
                taxableWithdrawal -= (stockExemptionTax.getCurrentExemption() - previousExemption);
                stockExemptionTax.setCurrentExemption(previousExemption);
                if (tax < 0.1) tax  = 0.0;
                if (taxableWithdrawal < 0.1) taxableWithdrawal  = 0.0;
                break;
            }
        }

        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof CapitalGainsTax capitalTax) {
                tax += capitalTax.estimateTax(taxableWithdrawal);
                break;
            }
        }
        double rate = tax / withdraw;
        // Clamp to [0, 1)
        if (Double.isNaN(rate) || rate < 0.0) {
            rate = 0.0;
        } else if (rate >= 1.0) {
            rate = 0.9999;  // prevent divide-by-zero or negative denom
        }
        return rate;
    }

    @Override
    default void addTax() {
        double gainOfCapitalRate = 1 - (getLiveData().getDeposited() / getLiveData().getCapital());
        double taxableWithdrawal = getLiveData().getWithdraw() * gainOfCapitalRate;
        double tax = 0;

        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof TaxExemptionCard taxExemptionCard) {
                float previousExemption = taxExemptionCard.getCurrentExemption();
                taxExemptionCard.calculateTax(taxableWithdrawal);
                taxableWithdrawal -= (taxExemptionCard.getCurrentExemption() - previousExemption);
                if (taxableWithdrawal < 0.1) taxableWithdrawal  = 0.0;
                break;
            }
        }

        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof StockExemptionTax stockExemptionTax) {
                float previousExemption = stockExemptionTax.getCurrentExemption();
                tax += stockExemptionTax.calculateTax(taxableWithdrawal);
                taxableWithdrawal -= (stockExemptionTax.getCurrentExemption() - previousExemption);
                if (tax < 0.1) tax  = 0.0;
                if (taxableWithdrawal < 0.1) taxableWithdrawal  = 0.0;
                break;
            }
        }

        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof CapitalGainsTax capitalTax) {
                tax += capitalTax.calculateTax(taxableWithdrawal);
                getLiveData().setCurrentTax(tax);
                getLiveData().addToTax(tax);
                break;
            }
        }
    }

    default void addNetEarnings() {
        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof CapitalGainsTax) {
                double net = getLiveData().getWithdraw() - getLiveData().getCurrentTax();
                getLiveData().addToNetEarnings(net);
                getLiveData().setCurrentNet(net);
                System.out.println("NetEarnings: " + net);
            } else if (rule instanceof NotionalGainsTax) {
                double net = getLiveData().getWithdraw();
                getLiveData().addToNetEarnings(net);
                getLiveData().setCurrentNet(net);
            }
        }
    }

}