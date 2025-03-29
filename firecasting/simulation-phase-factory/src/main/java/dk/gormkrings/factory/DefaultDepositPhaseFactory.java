package dk.gormkrings.factory;

import dk.gormkrings.action.Action;
import dk.gormkrings.action.Deposit;
import dk.gormkrings.data.IDate;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.DepositCallPhase;
import dk.gormkrings.phase.eventBased.DepositEventPhase;
import dk.gormkrings.specification.ISpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultDepositPhaseFactory implements IDepositPhaseFactory {
    @Value("${simulation.phase.type:call}")
    private String depositPhaseType;

    @Override
    public IPhase createDepositPhase(ISpecification specification, IDate startDate, long duration, Action deposit) {
        if ("event".equalsIgnoreCase(depositPhaseType)) {
            log.debug("Creating event-based deposit phase");
            return new DepositEventPhase(specification, startDate, duration, (Deposit) deposit);
        } else {
            log.debug("Creating call-based deposit phase");
            return new DepositCallPhase(specification, startDate, duration, deposit);
        }
    }
}
