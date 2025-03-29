package dk.gormkrings.inflation;

public interface IInflationFactory {
    Inflation createInflation();
    Inflation createInflation(float inflationPercentage);
}
