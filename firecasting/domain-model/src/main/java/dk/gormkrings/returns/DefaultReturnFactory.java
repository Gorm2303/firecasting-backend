package dk.gormkrings.returns;

import org.springframework.stereotype.Component;

@Component
public class DefaultReturnFactory implements IReturnFactory {
    @Override
    public IReturner createReturn(float returnPercentage) {
        return new SimpleMonthlyReturn(returnPercentage);
    }
}
