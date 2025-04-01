package dk.gormkrings.factory;

import dk.gormkrings.action.IAction;
import dk.gormkrings.data.IDate;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.specification.ISpecification;

public interface IDepositPhaseFactory {
    IPhase createDepositPhase(ISpecification specification, IDate startDate, long duration, IAction deposit);

}
