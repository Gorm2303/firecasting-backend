package dk.gormkrings.returns;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component("defaultReturnFactory")
public class DefaultReturnFactory implements IReturnFactory {
    @Value("${returner.selected:simpleDailyReturn}")
    private String returner;

    private final ApplicationContext context;

    @Autowired
    public DefaultReturnFactory (ApplicationContext context) {
        this.context = context;
    }

    @Override
    public IReturner createReturn() {
        IReturner returnerClass = switch (returner) {
            case "distributionReturn" -> context.getBean(DistributionReturn.class);
            case "dataDrivenReturn" -> context.getBean(DataDrivenReturn.class);
            default -> context.getBean(SimpleDailyReturn.class);
        };

        log.info("Creating new {} returner", returner.toUpperCase());
        return returnerClass;
    }
}
