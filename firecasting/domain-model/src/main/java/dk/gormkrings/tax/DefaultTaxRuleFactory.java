package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Setter
@Getter
public class DefaultTaxRuleFactory implements ITaxRuleFactory {

    ApplicationContext context;

    public DefaultTaxRuleFactory(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public ITaxRule createCapitalTax(double taxRate) {
        log.info("Creating Capital Gains Tax rule");
        return context.getBean(CapitalGainsTax.class, taxRate);
    }

    @Override
    public ITaxRule createNotionalTax(double taxRate) {
        log.info("Creating Notional Gains Tax rule");
        return context.getBean(NotionalGainsTax.class, taxRate);
    }
}
