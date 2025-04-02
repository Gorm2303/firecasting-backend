package dk.gormkrings.returns;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component("defaultReturnFactory")
public class DefaultReturnFactory implements IReturnFactory {
    @Value("${domain.returns.selected:simpleMonthlyReturn}")
    private String returner;

    private final ApplicationContext context;

    @Autowired
    public DefaultReturnFactory (ApplicationContext context) {
        this.context = context;
    }

    @Override
    public IReturner createReturn(float returnPercentage) {
        if ("distributionReturn".equalsIgnoreCase(returner)) {
            log.info("Creating new Distribution Returner");
            return context.getBean(DistributionReturn.class);
        } else {
            log.info("Creating new Simple Monthly Returner");
            return context.getBean(SimpleDailyReturn.class, returnPercentage);
        }
    }
}
