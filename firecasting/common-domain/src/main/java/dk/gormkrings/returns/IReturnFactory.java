package dk.gormkrings.returns;

public interface IReturnFactory {
    IReturner createReturn(float returnPercentage);
}
