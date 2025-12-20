package dk.gormkrings.returns;

public interface IReturnFactory {
    IReturner createReturn(String returnType);

    /**
     * Creates a returner with optional configuration. Implementations should fall back to defaults
     * when {@code config} is null or missing fields.
     */
    IReturner createReturn(String returnType, ReturnerConfig config);
}
