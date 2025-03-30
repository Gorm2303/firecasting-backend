package dk.gormkrings.factory;

import dk.gormkrings.action.IAction;
import dk.gormkrings.data.IDate;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.WithdrawCallPhase;
import dk.gormkrings.phase.eventBased.WithdrawEventPhase;
import dk.gormkrings.specification.ISpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultWithdrawPhaseFactory implements IWithdrawPhaseFactory {
    @Value("${simulation.phase.type:call}")
    private String withdrawPhaseType;

    @Override
    public IPhase createWithdrawPhase(ISpecification specification, IDate startDate, long duration, IAction withdraw) {
        if ("event".equalsIgnoreCase(withdrawPhaseType)) {
            log.debug("Creating event-based withdraw phase");
            return new WithdrawEventPhase(specification, startDate, duration, withdraw);
        } else {
            log.debug("Creating call-based withdraw phase");
            return new WithdrawCallPhase(specification, startDate, duration, withdraw);
        }
    }
}
