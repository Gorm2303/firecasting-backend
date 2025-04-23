package dk.gormkrings.simulation.factory;

import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.result.IRunResult;
import dk.gormkrings.simulation.result.RunResult;
import org.springframework.stereotype.Component;

@Component
public class DefaultResultFactory implements IResultFactory {
    @Override
    public IRunResult newResult() {
        return new RunResult();
    }
}
