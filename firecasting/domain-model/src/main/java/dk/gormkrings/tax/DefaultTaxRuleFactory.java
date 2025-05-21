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
public class DefaultTaxRuleFactory implements ITaxRuleFactory {

    private final ApplicationContext context;

    public DefaultTaxRuleFactory(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public ITaxRule create(String type, double taxRate) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Tax type must not be null or empty");
        }

        return switch (type.trim().toLowerCase()) {
            case "capital" -> {
                log.info("Creating Capital Gains Tax rule");
                yield context.getBean(CapitalGainsTax.class, taxRate);
            }
            case "notional" -> {
                log.info("Creating Notional Gains Tax rule");
                yield context.getBean(NotionalGainsTax.class, taxRate);
            }
            default -> throw new IllegalArgumentException("Unsupported tax type: " + type);
        };
    }

}
