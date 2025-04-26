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
        getLiveData().addToWithdrawn(withdrawAmount);
        getLiveData().subtractFromCapital(withdrawAmount);
        System.out.println("Withdraw amount " + withdrawAmount);
    }

    default void addTax() {
        double gainOfCapitalRate = 1 - (getLiveData().getDeposited() / getLiveData().getCapital());
        double taxableWithdrawal = getLiveData().getWithdraw() * gainOfCapitalRate;
        getLiveData().subtractFromDeposited((1 - gainOfCapitalRate) * getLiveData().getWithdraw());
        System.out.println("Taxable withdrawal " + taxableWithdrawal + " gain of " + gainOfCapitalRate * 100 + "%");
        double tax = 0;

        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof TaxExemptionCard taxExemptionCard) {
                taxableWithdrawal = taxExemptionCard.calculateTax(taxableWithdrawal);
                System.out.println("TaxExemptionCard: " + taxExemptionCard);

            }
        }

        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof StockExemptionTax stockExemptionTax) {
                float currentExemption = stockExemptionTax.getCurrentExemption();
                tax = stockExemptionTax.calculateTax(taxableWithdrawal);
                taxableWithdrawal -= (stockExemptionTax.getCurrentExemption() - currentExemption);
                System.out.println("StockExemptionTax: " + stockExemptionTax);
            }
        }

        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof CapitalGainsTax capitalTax) {
                tax += capitalTax.calculateTax(taxableWithdrawal);
                getLiveData().setCurrentTax(tax);
                getLiveData().addToTax(tax);
                System.out.println("CapitalGainsTax: " + tax);
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
