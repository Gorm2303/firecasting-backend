package dk.gormkrings.inflation;

public interface IInflationFactory {
    IInflation createInflation();
    IInflation createInflation(float inflationPercentage);
}
