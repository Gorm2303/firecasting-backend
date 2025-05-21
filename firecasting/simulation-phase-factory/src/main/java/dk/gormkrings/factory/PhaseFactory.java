package dk.gormkrings.factory;

import dk.gormkrings.action.IAction;
import dk.gormkrings.data.IDate;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.phase.callBased.DepositCallPhase;
import dk.gormkrings.phase.callBased.PassiveCallPhase;
import dk.gormkrings.phase.callBased.WithdrawCallPhase;
import dk.gormkrings.phase.eventBased.DepositEventPhase;
import dk.gormkrings.phase.eventBased.PassiveEventPhase;
import dk.gormkrings.phase.eventBased.WithdrawEventPhase;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PhaseFactory implements IPhaseFactory {

    @Value("${simulation.phase.type:call}")
    private String defaultPhaseType;

    public IPhase create(String phaseCategory, ISpecification specification, IDate startDate,
                         List<ITaxExemption> taxRules, long duration, IAction action) {

        String mode = defaultPhaseType.trim().toLowerCase();
        String category = phaseCategory.trim().toLowerCase();

        log.info("Creating {}-based {} phase", mode, category);

        switch (category) {
            case "deposit":
                return "event".equals(mode)
                        ? new DepositEventPhase(specification, startDate, taxRules, duration, action)
                        : new DepositCallPhase(specification, startDate, taxRules, duration, action);

            case "passive":
                return "event".equals(mode)
                        ? new PassiveEventPhase(specification, startDate, taxRules, duration, action)
                        : new PassiveCallPhase(specification, startDate, taxRules, duration, action);

            case "withdraw":
                return "event".equals(mode)
                        ? new WithdrawEventPhase(specification, startDate, taxRules, duration, action)
                        : new WithdrawCallPhase(specification, startDate, taxRules, duration, action);

            default:
                throw new IllegalArgumentException("Unsupported phase category: " + phaseCategory);
        }
    }
}
