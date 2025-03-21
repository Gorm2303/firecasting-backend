package dk.gormkrings.investment;


public class SimpleMonthlyReturn implements Return {
    private final float averagePercentage;

    public SimpleMonthlyReturn(float averagePercentage) {
        this.averagePercentage = averagePercentage;
        System.out.println("Initializing SimpleMonthlyReturn");
    }

    @Override
    public double calculateReturn(double amount) {
        return (amount * averagePercentage / 100) / 12;
    }

    public SimpleMonthlyReturn copy() {
        return new SimpleMonthlyReturn(averagePercentage);
    }
}
