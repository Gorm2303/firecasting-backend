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
            case "exemptioncard":
                log.info("Creating Tax Exemption Card rule");
                return context.getBean(TaxExemptionCard.class);
            case "stockexemption":
                log.info("Creating Stock Exemption rule");
                return context.getBean(StockExemptionTax.class);
            default:
                throw new IllegalArgumentException("Unsupported exemption type: " + type);
        }
    }

    @Override
    public ITaxExemption create(String type, TaxExemptionConfig config) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Exemption type must not be null or empty");
        }

        String key = type.trim().toLowerCase();
        ITaxExemption exemption = switch (key) {
            case "exemptioncard" -> {
                log.info("Creating Tax Exemption Card rule");
                yield context.getBean(TaxExemptionCard.class);
            }
            case "stockexemption" -> {
                log.info("Creating Stock Exemption rule");
                yield context.getBean(StockExemptionTax.class);
            }
            default -> throw new IllegalArgumentException("Unsupported exemption type: " + type);
        };

        if (config == null) {
            return exemption;
        }

        if (exemption instanceof TaxExemptionCard card && config.getExemptionCard() != null) {
            var c = config.getExemptionCard();
            if (c.getLimit() != null) card.setLimit(c.getLimit());
            if (c.getYearlyIncrease() != null) card.setYearlyLimitIncrease(c.getYearlyIncrease());
        }

        if (exemption instanceof StockExemptionTax stock && config.getStockExemption() != null) {
            var c = config.getStockExemption();
            if (c.getTaxRate() != null) stock.setTaxRate(c.getTaxRate());
            if (c.getLimit() != null) stock.setLimit(c.getLimit());
            if (c.getYearlyIncrease() != null) stock.setYearlyLimitIncrease(c.getYearlyIncrease());
        }

        return exemption;
    }
}
