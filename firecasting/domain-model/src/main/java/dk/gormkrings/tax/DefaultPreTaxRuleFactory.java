package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Setter
@Getter
public class DefaultPreTaxRuleFactory implements IPreTaxRuleFactory {
    ApplicationContext context;

    public DefaultPreTaxRuleFactory(ApplicationContext context) {
        this.context = context;
    }

    public ITaxRule createExemptionRule() {
        log.info("Creating Tax Exemption Card rule");
        return context.getBean(TaxExemptionCard.class);
    }

    public ITaxRule createStockRule() {
        log.info("Creating Stock Exemption rule");
        return context.getBean(StockExemptionTax.class);
    }
}
