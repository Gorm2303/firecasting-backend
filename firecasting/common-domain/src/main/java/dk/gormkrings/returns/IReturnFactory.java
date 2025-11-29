package dk.gormkrings.returns;

public interface IReturnFactory {
    IReturner createReturn(String returnType);
}
