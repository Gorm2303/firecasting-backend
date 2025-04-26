package dk.gormkrings.factory;

import dk.gormkrings.action.IAction;
import dk.gormkrings.data.IDate;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.PassiveCallPhase;
import dk.gormkrings.phase.eventBased.PassiveEventPhase;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DefaultPassivePhaseFactory implements IPassivePhaseFactory {
    @Value("${simulation.phase.type:call}")
    private String passivePhaseType;

    @Override
    public IPhase createPassivePhase(ISpecification specification, IDate startDate, List<ITaxRule> taxRules, long duration, IAction passive) {
        if ("event".equalsIgnoreCase(passivePhaseType)) {
            log.info("Creating event-based passive phase");
            return new PassiveEventPhase(specification, startDate, taxRules, duration, passive);
        } else {
            log.info("Creating call-based passive phase");
            return new PassiveCallPhase(specification, startDate, taxRules, duration, passive);
        }
    }
}
