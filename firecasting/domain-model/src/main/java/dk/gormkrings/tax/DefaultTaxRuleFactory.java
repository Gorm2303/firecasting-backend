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
            log.info("Creating Capital Gains Tax rule");
            return new CapitalGainsTax(taxRate);
        } else {
            log.info("Creating Notional Gains Tax rule");
            return new NotionalGainsTax(taxRate);
        }
    }
}
