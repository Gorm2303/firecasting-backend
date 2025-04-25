package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Setter
@Getter
public class DefaultTaxRuleFactory implements ITaxRuleFactory {
    @Override
    public ITaxRule createTaxRule(String taxRule, float taxPercentage) {
        if ("capital".equalsIgnoreCase(taxRule)) {
            log.info("Creating Capital Gains Tax rule");
            return new CapitalGainsTax(taxPercentage);
        } else {
            log.info("Creating Notional Gains Tax rule");
            return new NotionalGainsTax(taxPercentage);
        }
    }
}
