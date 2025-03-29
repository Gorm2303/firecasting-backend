package dk.gormkrings.simulation.factory;

import dk.gormkrings.factory.IResultFactory;
import dk.gormkrings.result.IResult;
import dk.gormkrings.simulation.result.Result;
import org.springframework.stereotype.Component;

@Component
public class DefaultResultFactory implements IResultFactory {
    @Override
    public IResult newResult() {
        return new Result();
    }
}
