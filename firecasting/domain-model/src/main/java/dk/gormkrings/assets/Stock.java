package dk.gormkrings.assets;

public class Stock implements Asset, Fee {
    @Override
    public double calculateRateOfReturn() {
        return 0;
    }

    @Override
    public float calculateFee() {
        return 0;
    }
}
