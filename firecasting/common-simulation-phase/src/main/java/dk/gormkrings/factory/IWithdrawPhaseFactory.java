package dk.gormkrings.factory;

import dk.gormkrings.action.IAction;
import dk.gormkrings.data.IDate;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;
import dk.gormkrings.tax.ITaxRule;

import java.util.List;

public interface IWithdrawPhaseFactory {
    IPhase createWithdrawPhase(ISpecification specification, IDate startDate, List<ITaxExemption> taxRules, long duration, IAction withdraw);
}
