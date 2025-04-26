package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Getter
@Component
@Scope("prototype")
public class CapitalGainsTax implements ITaxRule {
    private double taxRate;

    public CapitalGainsTax(double taxRate) {
        this.taxRate = taxRate;
        log.debug("Capital Gains Tax Rule Created: {}", taxRate);
    }

    @Override
    public double calculateTax(double amount) {
        return amount * taxRate / 100;
    }

    public double estimateTax(double amount) {
        return amount * taxRate / 100;
    }

    @Override
    public CapitalGainsTax copy() {
        return new CapitalGainsTax(
                this.taxRate
                );
    }

    @Override
    public void yearlyUpdate() {

    }
}
