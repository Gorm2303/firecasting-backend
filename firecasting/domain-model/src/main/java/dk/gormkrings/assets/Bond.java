package dk.gormkrings.assets;

public class Bond implements Asset, Fee {
    @Override
    public double calculateRateOfReturn() {
        return 0;
    }

    @Override
    public float calculateFee() {
        return 0;
    }
}
