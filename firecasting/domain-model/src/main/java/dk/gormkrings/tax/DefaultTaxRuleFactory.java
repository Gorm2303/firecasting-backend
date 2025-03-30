package dk.gormkrings.tax;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultTaxRuleFactory implements ITaxRuleFactory {

    @Value("${simulation.phase.tax:notional}")
    private String taxRule;

    @Override
    public ITaxRule createTaxRule(double taxRate) {
        if ("capital".equalsIgnoreCase(taxRule)) {
            log.debug("Creating Capital Gains Tax rule");
            return new CapitalGainsTax(taxRate);
        } else {
            log.debug("Creating Notional Gains Tax rule");
            return new NotionalGainsTax(taxRate);
        }
    }
}
