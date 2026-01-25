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
        double capital = getLiveData().getCapital();
        if (capital < 0.0001) return;
        double withdrawAmount = getWithdraw().getMonthlyAmount(capital, getLiveData().getInflation());
        withdrawAmount += getWithdraw().getDynamicAmountOfReturn();
        if (Double.isNaN(withdrawAmount) || withdrawAmount < 0.0001) {
            withdrawAmount = 0;
        } else if (withdrawAmount > capital) {
            withdrawAmount = capital;
        }

        double realWithdrawAmount = withdrawAmount;
        if (getSpecification().getTaxRule() instanceof CapitalGainsTax capitalGainsTax && !getWithdraw().isPercentageWithdraw()) {
            for (int i = 0; i < 3; i++) {
                double estimateTaxRate = estimateCapitalTaxRate(realWithdrawAmount, capitalGainsTax);
                realWithdrawAmount = withdrawAmount / (1 - estimateTaxRate);
            }
        }

        // Safety: never withdraw more than the current capital, even after tax gross-up.
        // Otherwise we can force negative capital in edge cases (e.g., high tax rate + negative returns).
        if (!Double.isFinite(realWithdrawAmount) || realWithdrawAmount < 0.0) {
            realWithdrawAmount = 0.0;
        } else if (realWithdrawAmount > capital) {
            realWithdrawAmount = capital;
        }

        double depositRatio = getLiveData().getDeposited() / capital;
        getLiveData().subtractFromDeposited(depositRatio * realWithdrawAmount);

        getLiveData().setWithdraw(realWithdrawAmount);
        getLiveData().addToWithdrawn(realWithdrawAmount);
        getLiveData().subtractFromCapital(realWithdrawAmount);
    }

    default double estimateCapitalTaxRate(double withdraw, CapitalGainsTax capitalGainsTax) {
        if (withdraw <= 0.0) {
            return 0.0;
        }
        double gainOfCapitalRate = 1 - (getLiveData().getDeposited() / getLiveData().getCapital());
        double tax = 0;
        if (getLiveData().getCapital() > 0) {
            if (gainOfCapitalRate < 0) gainOfCapitalRate = 0.0;
            if (gainOfCapitalRate > 1) gainOfCapitalRate = 1.0;
        }
        double taxableWithdrawal = withdraw * gainOfCapitalRate;

        for (ITaxExemption rule : getTaxExemptions()) {
            if (rule instanceof TaxExemptionCard taxExemptionCard) {
                float previousExemption = taxExemptionCard.getCurrentExemption();
                taxExemptionCard.calculateTax(taxableWithdrawal);
                taxableWithdrawal -= (taxExemptionCard.getCurrentExemption() - previousExemption);
                taxExemptionCard.setCurrentExemption(previousExemption);
                if (taxableWithdrawal < 0.0001) taxableWithdrawal  = 0.0;
                break;
            }
        }

        for (ITaxExemption rule : getTaxExemptions()) {
            if (rule instanceof StockExemptionTax stockExemptionTax) {
                float previousExemption = stockExemptionTax.getCurrentExemption();
                tax += stockExemptionTax.calculateTax(taxableWithdrawal);
                taxableWithdrawal -= (stockExemptionTax.getCurrentExemption() - previousExemption);
                stockExemptionTax.setCurrentExemption(previousExemption);
                if (tax < 0.0001) tax  = 0.0;
                if (taxableWithdrawal < 0.0001) taxableWithdrawal  = 0.0;
                break;
            }
        }

        tax += capitalGainsTax.estimateTax(taxableWithdrawal);

        double rate = tax / withdraw;
        if (Double.isNaN(rate) || rate < 0.0) {
            rate = 0.0;
        } else if (rate >= 1.0) {
            rate = 0.9999;
        }
        return rate;
    }

    default void addCapitalTax() {
        double gainOfCapitalRate = 1 - (getLiveData().getDeposited() / getLiveData().getCapital());
        if (getLiveData().getCapital() > 0) {
            if (gainOfCapitalRate < 0) gainOfCapitalRate = 0.0;
            if (gainOfCapitalRate > 1) gainOfCapitalRate = 1.0;
        }
        double taxableWithdrawal = getLiveData().getWithdraw() * gainOfCapitalRate;
        if (taxableWithdrawal < 0.0001) taxableWithdrawal = 0.0;
        double tax = 0;

        // Adjust the amount of taxable withdrawal
        taxableWithdrawal = taxExemptionCardToTaxableAmount(taxableWithdrawal);

        // Apply lower tax rate
        tax = stockTaxExemptionToTax(tax, taxableWithdrawal);

        // Adjust the amount of taxable withdrawal
        taxableWithdrawal = stockTaxExemptionToTaxableAmount(taxableWithdrawal);


        if (getSpecification().getTaxRule() instanceof CapitalGainsTax capitalTax) {
            tax += capitalTax.calculateTax(taxableWithdrawal);
            getLiveData().setCurrentTax(tax);
            getLiveData().addToTax(tax);

        }
    }

    default void addNetEarnings() {
        ITaxRule taxRule = getSpecification().getTaxRule();

        if (taxRule instanceof CapitalGainsTax) {
            double net = getLiveData().getWithdraw() - getLiveData().getCurrentTax();
            if (net < 0.0001) net = 0.0;
            getLiveData().addToNetEarnings(net);
            getLiveData().setCurrentNet(net);

        } else if (taxRule instanceof NotionalGainsTax) {
            double net = getLiveData().getWithdraw();
            getLiveData().addToNetEarnings(net);
            getLiveData().setCurrentNet(net);
        }
    }

}