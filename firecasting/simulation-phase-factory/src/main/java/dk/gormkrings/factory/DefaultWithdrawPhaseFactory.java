package dk.gormkrings.factory;

import dk.gormkrings.action.IAction;
import dk.gormkrings.data.IDate;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.WithdrawCallPhase;
import dk.gormkrings.phase.eventBased.WithdrawEventPhase;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;
import dk.gormkrings.tax.ITaxRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DefaultWithdrawPhaseFactory implements IWithdrawPhaseFactory {
    @Value("${simulation.phase.type:call}")
    private String withdrawPhaseType;

    @Override
    public IPhase createWithdrawPhase(ISpecification specification, IDate startDate, List<ITaxExemption> taxRules, long duration, IAction withdraw) {
        if ("event".equalsIgnoreCase(withdrawPhaseType)) {
            log.info("Creating event-based withdraw phase");
            return new WithdrawEventPhase(specification, startDate,taxRules, duration, withdraw);
        } else {
            log.info("Creating call-based withdraw phase");
            return new WithdrawCallPhase(specification, startDate, taxRules, duration, withdraw);
        }
    }
}
