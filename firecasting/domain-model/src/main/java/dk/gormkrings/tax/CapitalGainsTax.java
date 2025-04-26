package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
public class CapitalGainsTax implements ITaxRule {
    private final double taxRate;

    public CapitalGainsTax(double taxRate) {
        this.taxRate = taxRate;
        log.debug("Capital Gains Tax Rule Created: {}", taxRate);
    }

    @Override
    public double calculateTax(double amount) {
        return amount * taxRate / 100;
    }

    @Override
    public void yearlyUpdate() {
    }

    @Override
    public CapitalGainsTax copy() {
        return new CapitalGainsTax(
                this.taxRate
                );
    }
}
