package dk.gormkrings.returns;

import org.springframework.stereotype.Component;

@Component
public class DefaultReturnFactory implements IReturnFactory {
    @Override
    public IReturn createReturn(float returnPercentage) {
        return new SimpleMonthlyReturn(returnPercentage);
    }
}
