package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Setter
@Component
@Scope("prototype")
public class NotionalGainsTax implements ITaxRule {
    private final double taxRate;
    private double previousReturned = 0;

    public NotionalGainsTax(double taxRate) {
        this.taxRate = taxRate;
        log.debug("Notional Gains Tax Rule Created: {}", taxRate);
    }

    @Override
    public double calculateTax(double amount) {
        return amount * taxRate / 100;
    }

    @Override
    public NotionalGainsTax copy() {
        NotionalGainsTax notionalGainsTax = new NotionalGainsTax(
                this.taxRate
        );
        notionalGainsTax.setPreviousReturned(this.previousReturned);
        return notionalGainsTax;
    }
}
