package dk.gormkrings.factory;

import dk.gormkrings.action.IAction;
import dk.gormkrings.data.IDate;
import dk.gormkrings.phase.IPhase;
import dk.gormkrings.specification.ISpecification;
import dk.gormkrings.tax.ITaxExemption;

import java.util.List;

public interface IPhaseFactory {
    IPhase create(String phaseCategory, ISpecification specification, IDate startDate,
                  List<ITaxExemption> taxRules, long duration, IAction action);
}
