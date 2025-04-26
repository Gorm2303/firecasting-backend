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

        double estimateTaxRate = estimateTaxRate(withdrawAmount);
        double reEstimateTaxRate = estimateTaxRate(withdrawAmount / (1 - estimateTaxRate));
        double lastEstimateTaxRate = estimateTaxRate(withdrawAmount / (1 - reEstimateTaxRate));
        System.out.println("Estimated Tax Adjusted: " + lastEstimateTaxRate + ", Estimated new withdrawal: " + withdrawAmount/(1 - lastEstimateTaxRate));

        getLiveData().setWithdraw(withdrawAmount / (1 - lastEstimateTaxRate));
        getLiveData().addToWithdrawn(withdrawAmount);
        getLiveData().subtractFromCapital(withdrawAmount);

        double gainOfCapitalRate = 1 - (getLiveData().getDeposited() / getLiveData().getCapital());
        getLiveData().subtractFromDeposited((1 - gainOfCapitalRate) * getLiveData().getWithdraw());
        System.out.println("Withdraw amount " + withdrawAmount);
    }

    default double estimateTaxRate(double withdraw) {
        double gainOfCapitalRate = 1 - (getLiveData().getDeposited() / getLiveData().getCapital());
        double taxableWithdrawal = withdraw * gainOfCapitalRate;
        System.out.println("Taxable withdrawal " + taxableWithdrawal + " gain of " + gainOfCapitalRate * 100 + "%");
        double tax = 0;

        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof TaxExemptionCard taxExemptionCard) {
                float previousExemption = taxExemptionCard.getCurrentExemption();
                taxExemptionCard.calculateTax(taxableWithdrawal);
                taxableWithdrawal -= (taxExemptionCard.getCurrentExemption() - previousExemption);
                taxExemptionCard.setCurrentExemption(previousExemption);
                if (taxableWithdrawal < 0.1) taxableWithdrawal  = 0.0;
                System.out.println("Estimate TaxExemptionCard: " + taxExemptionCard);
                System.out.println("Estimate Card - Added tax " + tax);
                System.out.println("Estimate Card - Taxble Withdrawal " + taxableWithdrawal);
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
                System.out.println("Estimate StockExemptionTax: " + stockExemptionTax);
                System.out.println("Estimate Stock - Added tax " + tax);
                System.out.println("Estimate Stock - Taxble Withdrawal " + taxableWithdrawal);
                break;
            }
        }

        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof CapitalGainsTax capitalTax) {
                tax += capitalTax.estimateTax(taxableWithdrawal);
                System.out.println("Estimate CapitalGainsTax: " + tax);
                System.out.println("Estimate Capital - Added tax " + tax);
                System.out.println("Estimate Capital - Taxble Withdrawal " + taxableWithdrawal);
                break;
            }
        }
        System.out.println("Tax: " + tax + ", Old Withdraw: " + withdraw);
        return tax / withdraw;
    }

    default void addTax() {
        double gainOfCapitalRate = 1 - (getLiveData().getDeposited() / getLiveData().getCapital());
        double taxableWithdrawal = getLiveData().getWithdraw() * gainOfCapitalRate;
        System.out.println("Taxable withdrawal " + taxableWithdrawal + " gain of " + gainOfCapitalRate * 100 + "%");
        double tax = 0;

        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof TaxExemptionCard taxExemptionCard) {
                float previousExemption = taxExemptionCard.getCurrentExemption();
                taxExemptionCard.calculateTax(taxableWithdrawal);
                taxableWithdrawal -= (taxExemptionCard.getCurrentExemption() - previousExemption);
                if (taxableWithdrawal < 0.1) taxableWithdrawal  = 0.0;
                System.out.println("Calculation TaxExemptionCard: " + taxExemptionCard);
                System.out.println("Calculation Card - Added tax " + tax);
                System.out.println("Calculation Card - Taxble Withdrawal " + taxableWithdrawal);
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
                System.out.println("Calculation StockExemptionTax: " + stockExemptionTax);
                System.out.println("Calculation Stock - Added tax " + tax);
                System.out.println("Calculation Stock - Taxble Withdrawal " + taxableWithdrawal);
                break;
            }
        }

        for (ITaxRule rule : getTaxRules()) {
            if (rule instanceof CapitalGainsTax capitalTax) {
                tax += capitalTax.calculateTax(taxableWithdrawal);
                getLiveData().setCurrentTax(tax);
                getLiveData().addToTax(tax);
                System.out.println("Calculation CapitalGainsTax: " + tax);
                System.out.println("Calculation Capital - Added tax " + tax);
                System.out.println("Calculation Capital - Taxble Withdrawal " + taxableWithdrawal);
                break;
            }
        }
        System.out.println("Tax: " + tax + ", New Withdraw: " + getLiveData().getWithdraw());
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
