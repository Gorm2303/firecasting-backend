package dk.gormkrings;

import dk.gormkrings.randomness.DefaultRandomNumberGenerator;

public class Main {
    public static void main(String[] args) {
        DefaultRandomNumberGenerator rng = new DefaultRandomNumberGenerator();

        System.out.println("Hello and welcome! This is the number:");
        System.out.println(rng.nextDouble());
        }
}