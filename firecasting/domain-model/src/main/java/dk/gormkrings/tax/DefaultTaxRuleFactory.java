package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConfigurationProperties(prefix = "simulation")
@Setter
@Getter
public class DefaultTaxRuleFactory implements ITaxRuleFactory {
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
