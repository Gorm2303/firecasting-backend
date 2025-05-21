package dk.gormkrings.tax;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Setter
@Component
public class DefaultTaxExemptionFactory implements ITaxExemptionFactory {
    private final ApplicationContext context;

    public DefaultTaxExemptionFactory(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public ITaxExemption create(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Exemption type must not be null or empty");
        }

        switch (type.trim().toLowerCase()) {
            case "card":
                log.info("Creating Tax Exemption Card rule");
                return context.getBean(TaxExemptionCard.class);
            case "stock":
                log.info("Creating Stock Exemption rule");
                return context.getBean(StockExemptionTax.class);
            default:
                throw new IllegalArgumentException("Unsupported exemption type: " + type);
        }
    }
}
