package dk.gormkrings.factory;

import dk.gormkrings.action.Action;
import dk.gormkrings.data.IDate;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.specification.ISpecification;

public interface IWithdrawPhaseFactory {
    IPhase createWithdrawPhase(ISpecification specification, IDate startDate, long duration, Action withdraw);
}
