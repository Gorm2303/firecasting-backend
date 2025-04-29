package dk.gormkrings.factory;

import dk.gormkrings.action.IAction;
import dk.gormkrings.data.IDate;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.DepositCallPhase;
import dk.gormkrings.phase.eventBased.DepositEventPhase;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;
import dk.gormkrings.tax.ITaxRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DefaultDepositPhaseFactory implements IDepositPhaseFactory {
    @Value("${simulation.phase.type:call}")
    private String depositPhaseType;

    @Override
    public IPhase createDepositPhase(ISpecification specification, IDate startDate, List<ITaxExemption> taxRules, long duration, IAction deposit) {
        if ("event".equalsIgnoreCase(depositPhaseType)) {
            log.info("Creating event-based deposit phase");
            return new DepositEventPhase(specification, startDate, taxRules, duration, deposit);
        } else {
            log.info("Creating call-based deposit phase");
            return new DepositCallPhase(specification, startDate, taxRules, duration, deposit);
        }
    }
}
