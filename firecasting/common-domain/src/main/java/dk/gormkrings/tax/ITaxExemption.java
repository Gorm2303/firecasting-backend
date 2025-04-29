package dk.gormkrings.tax;

public interface ITaxExemption extends ITaxRule {
    void yearlyUpdate();
    ITaxExemption copy();
}
