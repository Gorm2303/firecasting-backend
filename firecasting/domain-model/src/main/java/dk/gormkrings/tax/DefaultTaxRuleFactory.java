package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Setter
@Getter
public class DefaultTaxRuleFactory implements ITaxRuleFactory {

    private ApplicationContext ctx;

    @Autowired
    DefaultTaxRuleFactory(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    public ITaxRule create(String ruleName) {
        return switch (ruleName) {
            case "STOCKEXEMPTION" -> {
                log.info("Creating Tax Stock Exemption rule");
                yield ctx.getBean(StockExemptionTax.class).copy();
            }
            case "EXEMPTIONCARD" -> {
                log.info("Creating Tax Exemption Card rule");
                yield ctx.getBean(TaxExemptionCard.class).copy();
            }
            default -> throw new IllegalArgumentException("Unknown tax rule: " + ruleName);
        };
    }

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
