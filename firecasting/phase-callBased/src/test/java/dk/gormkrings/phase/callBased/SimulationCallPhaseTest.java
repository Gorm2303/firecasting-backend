package dk.gormkrings.phase.callBased;

import dk.gormkrings.inflation.IInflation;
import dk.gormkrings.returns.IReturner;
import dk.gormkrings.tax.CapitalGainsTax;
import dk.gormkrings.tax.NotionalGainsTax;
import dk.gormkrings.test.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationCallPhaseTest {
    private DummySpecification dummySpec;
    private SimulationCallPhase testPhase;

    @BeforeEach
    void setUp() {
        dummySpec = new DummySpecification();
        dummySpec.setInflation(new IInflation() {
            @Override
            public double calculatePercentage() {
                return 3;
            }

            @Override
            public IInflation copy() {
                return null;
            }
        });
        dummySpec.setReturner(new DummyReturner());
        dummySpec.setTaxRule(new NotionalGainsTax(42));
        testPhase = new TestSimulationCallPhase(dummySpec, new DummyDate(1000), 30, "TestPhase");
    }

    @Test
    void testOnMonthEnd_IntegrationOfAddReturn() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(20000);
        liveData.setReturned(0);
        double expectedReturn = (20000 * 0.10) / 252;


        testPhase.onDayEnd();

        assertEquals(expectedReturn, liveData.getCurrentReturn(), 0.001, "Current return should equal the calculated interest");
        assertEquals(expectedReturn, liveData.getReturned(), 0.001, "Returned should be increased by the calculated interest");
        assertEquals(20000 + expectedReturn, liveData.getCapital(), 0.001, "Capital should increase by the calculated interest");
    }

    @Test
    void testOnYearEnd_IntegrationOfTaxAndInflation() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(20000);
        liveData.setReturned(1000);

        dummySpec.setTaxRule(new NotionalGainsTax(42));
        dummySpec.setInflation(new IInflation() {
            @Override
            public double calculatePercentage() {
                return 3;  // 3%
            }

            @Override
            public IInflation copy() {
                return this;
            }
        });
        double expectedTax = (liveData.getReturned() - 0) * 42 / 100;
        double expectedInflation = 3;

        testPhase.onYearEnd();

        assertEquals(expectedTax, liveData.getCurrentTax(), 0.001, "Current tax should equal the calculated tax");
        assertEquals(expectedTax, liveData.getTax(), 0.001, "Total tax should equal the calculated tax");
        assertEquals(expectedInflation, liveData.getInflation(), 0.001, "Inflation should be increased by the dummy inflation percentage");
        assertEquals(0, liveData.getCurrentNet(), 0.001, "Current net should be 0 if no withdrawal occurred");
    }

    @Test
    void testOnMonthEnd_WithZeroCapital() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(0);
        liveData.setReturned(0);
        liveData.setCurrentReturn(0);

        testPhase.onMonthEnd();

        assertEquals(0, liveData.getCurrentReturn(), 0.001, "Current return should remain 0 when capital is zero");
        assertEquals(0, liveData.getReturned(), 0.001, "Returned should remain 0 when capital is zero");
        assertEquals(0, liveData.getCapital(), 0.001, "Capital should remain 0 when starting with 0");
    }

    @Test
    void testOnMonthEnd_WithNegativeCapital() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(-500);
        liveData.setReturned(-500);
        liveData.setCurrentReturn(0);
        double expectedReturn = (-500 * 0.10) / 252;

        testPhase.onDayEnd();

        double expectedCapital = -500 + expectedReturn;
        double expectedReturned = -500 + expectedReturn;

        assertEquals(expectedReturn, liveData.getCurrentReturn(), 0.001, "Current return should equal the calculated return for negative capital");
        assertEquals(expectedReturned, liveData.getReturned(), 0.001, "Returned should update correctly for negative capital");
        assertEquals(expectedCapital, liveData.getCapital(), 0.001, "Capital should be adjusted by the calculated return when negative");
    }

    @Test
    void testAddReturn_FixedReturnCalculation() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(20000);
        liveData.setReturned(0);
        double expectedReturn = (20000 * 0.10) / 252;

        testPhase.addReturn();

        assertEquals(expectedReturn, liveData.getCurrentReturn(), 0.001,
                "Current return should equal the calculated interest");
        assertEquals(expectedReturn, liveData.getReturned(), 0.001,
                "Returned should be increased by the calculated interest");
        assertEquals(20000 + expectedReturn, liveData.getCapital(), 0.001,
                "Capital should increase by the calculated interest");
    }

    @Test
    void testAddReturn_ZeroAndNegativeCapital() {
        DummyLiveData liveData = dummySpec.getLiveData();

        liveData.setCapital(0);
        liveData.setReturned(0);
        testPhase.addReturn();
        double expectedReturnZero = (0 * 0.10) / 252;
        assertEquals(expectedReturnZero, liveData.getCurrentReturn(), 0.001,
                "When capital is zero, current return should be 0");
        assertEquals(expectedReturnZero, liveData.getReturned(), 0.001,
                "When capital is zero, returned should remain 0");
        assertEquals(0, liveData.getCapital(), 0.001,
                "Capital should remain 0 when starting from 0");

        liveData.setCapital(-500);
        liveData.setReturned(-500);
        testPhase.addReturn();
        double expectedReturnNegative = (-500 * 0.10) / 252;
        assertEquals(expectedReturnNegative, liveData.getCurrentReturn(), 0.001,
                "For negative capital, current return should be correctly calculated");
        assertEquals(-500 + expectedReturnNegative, liveData.getReturned(), 0.001,
                "Returned should update correctly for negative capital");
        assertEquals(-500 + expectedReturnNegative, liveData.getCapital(), 0.001,
                "Capital should update correctly for negative capital");
    }

    @Test
    void testAddReturn_MultipleSequentialCalls() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(20000);
        liveData.setReturned(0);
        double expectedReturn1 = (20000 * 0.10) / 252;

        testPhase.addReturn();

        assertEquals(expectedReturn1, liveData.getCurrentReturn(), 0.001,
                "After first call, current return should equal expectedReturn1");
        assertEquals(expectedReturn1, liveData.getReturned(), 0.001,
                "After first call, returned should equal expectedReturn1");
        assertEquals(20000 + expectedReturn1, liveData.getCapital(), 0.001,
                "After first call, capital should increase by expectedReturn1");

        double capitalAfterFirst = liveData.getCapital();
        double expectedReturn2 = (capitalAfterFirst * 0.10) / 252;
        testPhase.addReturn();

        double cumulativeReturn = expectedReturn1 + expectedReturn2;
        assertEquals(cumulativeReturn, liveData.getReturned(), 0.001,
                "After second call, returned should equal cumulative return");
        assertEquals(expectedReturn2, liveData.getCurrentReturn(), 0.001,
                "After second call, current return should be updated to expectedReturn2 (the latest return)");
        assertEquals(20000 + cumulativeReturn, liveData.getCapital(), 0.001,
                "After second call, capital should equal initial capital plus cumulative return");
    }

    @Test
    void testIntegratedBehavior_OrderOfExecution() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(20000);
        liveData.setReturned(300);
        liveData.setCurrentReturn(0);
        liveData.setInflation(0);

        testPhase.addReturn();
        double expectedReturn = (20000 * 0.10) / 252;
        assertEquals(expectedReturn, liveData.getCurrentReturn(), 0.001, "After addReturn, current return should be as expected");
        double capitalAfterReturn = 20000 + expectedReturn;
        double returnedAfterReturn = 300 + expectedReturn;

        testPhase.addTax();
        double expectedTax = (returnedAfterReturn - 0) * 42 / 100;
        double capitalAfterTax = capitalAfterReturn - expectedTax;
        double returnedAfterTax = returnedAfterReturn - expectedTax;
        assertEquals(expectedTax, liveData.getCurrentTax(), 0.001, "Current tax should equal calculated tax");
        assertEquals(expectedTax, liveData.getTax(), 0.001, "Total tax should equal calculated tax");

        testPhase.addInflation();
        double expectedInflation = 3;
        assertEquals(expectedInflation, liveData.getInflation(), 0.001, "Inflation should be increased by 3");

        assertEquals(capitalAfterTax, liveData.getCapital(), 0.001, "Final capital should equal initial capital plus interest minus tax");
        assertEquals(returnedAfterTax, liveData.getReturned(), 0.001, "Final returned should equal initial returned plus interest minus tax");
    }

    @Test
    void testEdgeCase_OrderMismatch() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(20000);
        liveData.setReturned(300);
        liveData.setCurrentReturn(0);
        liveData.setInflation(0);

        testPhase.addTax();
        testPhase.addReturn();
        testPhase.addInflation();

        double tax = (300 * 42) / 100.0;
        double capitalAfterTax = 20000 - tax; 
        double interest = (capitalAfterTax * 0.10) / 252;
        double expectedIncorrectCapital = capitalAfterTax + interest; 

        double expectedReturnCorrect = (20000 * 0.10) / 252;
        double capitalAfterReturn = 20000 + expectedReturnCorrect; 
        double correctTax = (466.67 * 42) / 100.0; 
        double expectedCorrectCapital = capitalAfterReturn - correctTax; 

        assertEquals(expectedIncorrectCapital, liveData.getCapital(), 0.001,
                "Final capital with incorrect order should equal the value computed in the incorrect sequence");
        assertNotEquals(expectedCorrectCapital, liveData.getCapital(), 0.001,
                "Final capital with incorrect order should differ from the correct order calculation");
    }
    
    @Test
    void testAddReturn_NullReturner() {
        // Set the specification's returner to null.
        dummySpec.setReturner(null);
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(20000);
        liveData.setReturned(0);

        // Expect that addReturn() now throws a NullPointerException.
        assertThrows(NullPointerException.class, () -> {
            testPhase.addReturn();
        }, "addReturn() should throw an exception when returner is null");
    }

    @Test
    void testAddInflation_NullInflation() {
        // Set the specification's inflation to null.
        dummySpec.setInflation(null);
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setInflation(0);

        // Expect that addInflation() now throws a NullPointerException.
        assertThrows(NullPointerException.class, () -> {
            testPhase.addInflation();
        }, "addInflation() should throw an exception when inflation is null");
    }

    @Test
    void testAddReturn_InvalidComputedReturn() {
        dummySpec.setReturner(new IReturner() {
            @Override
            public double calculateReturn(double amount) {
                return Double.NaN;
            }
            @Override
            public IReturner copy() {
                return this;
            }
        });

        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(20000);
        liveData.setReturned(1000);

        testPhase.addReturn();

        assertTrue(Double.isNaN(liveData.getCurrentReturn()),
                "Current return should be NaN when computed return is NaN");
        assertTrue(Double.isNaN(liveData.getReturned()),
                "Returned should be NaN when computed return is NaN");
    }

    @Test
    void testAddInflation_InvalidComputedInflation() {
        dummySpec.setInflation(new IInflation() {
            @Override
            public double calculatePercentage() {
                return Double.POSITIVE_INFINITY;
            }
            @Override
            public IInflation copy() {
                return this;
            }
        });

        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setInflation(0);

        testPhase.addInflation();
        assertTrue(Double.isInfinite(liveData.getInflation()), "Inflation should be infinite when computed inflation is infinite");
    }

    @Test
    void testAddTax_NonApplicableTaxRule() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCurrentTax(0);
        liveData.setTax(0);
        liveData.setReturned(500);
        liveData.setCapital(20000);

        dummySpec.setTaxRule(new CapitalGainsTax(42));

        testPhase.addTax();

        assertEquals(0, liveData.getCurrentTax(), 0.001, "Current tax should remain unchanged for non-applicable tax rule");
        assertEquals(0, liveData.getTax(), 0.001, "Total tax should remain unchanged for non-applicable tax rule");

        assertEquals(20000, liveData.getCapital(), 0.001, "Capital should remain unchanged for non-applicable tax rule");
        assertEquals(500, liveData.getReturned(), 0.001, "Returned should remain unchanged for non-applicable tax rule");
    }

    @Test
    void testAddTax_EdgeCondition_ZeroDifference() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(20000);
        liveData.setReturned(800);
        NotionalGainsTax taxRule = new NotionalGainsTax(42);
        taxRule.setPreviousReturned(800);
        dummySpec.setTaxRule(taxRule);

        liveData.setCurrentTax(0);
        liveData.setTax(0);

        testPhase.addTax();

        double expectedTax = 0;
        assertEquals(expectedTax, liveData.getCurrentTax(), 0.001, "Current tax should be 0 when returned difference is 0");
        assertEquals(expectedTax, liveData.getTax(), 0.001, "Total tax should be 0 when returned difference is 0");
        assertEquals(20000, liveData.getCapital(), 0.001, "Capital should remain unchanged when tax difference is 0");
        assertEquals(800, liveData.getReturned(), 0.001, "Returned should remain unchanged when tax difference is 0");
    }

    @Test
    void testAddTax_EdgeCondition_NegativeDifference() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(20000);
        liveData.setReturned(500);

        NotionalGainsTax taxRule = new NotionalGainsTax(42);
        taxRule.setPreviousReturned(800);
        dummySpec.setTaxRule(taxRule);

        liveData.setCurrentTax(0);
        liveData.setTax(0);

        testPhase.addTax();

        double expectedTax = 0;
        assertEquals(-126, liveData.getCurrentTax(), 0.001,
                "Current tax should be -126 when the difference in returned is negative");
        assertEquals(expectedTax, liveData.getTax(), 0.001,
                "Total tax should be 0 when the difference in returned is negative");
        assertEquals(20000, liveData.getCapital(), 0.001,
                "Capital should remain unchanged when negative difference results in zero tax");
        assertEquals(500, liveData.getReturned(), 0.001,
                "Returned should remain unchanged when negative difference results in zero tax");
    }

    @Test
    void testAddTax_MultipleSequentialCalls_NoAdditionalChange() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(20000);
        liveData.setReturned(1000);

        NotionalGainsTax taxRule = new NotionalGainsTax(42);
        taxRule.setPreviousReturned(0);
        dummySpec.setTaxRule(taxRule);

        liveData.setCurrentTax(0);
        liveData.setTax(0);

        testPhase.addTax();
        double expectedTaxFirst = (1000 * 42) / 100.0; // 420
        assertEquals(expectedTaxFirst, liveData.getCurrentTax(), 0.001,
                "First call: current tax should be 420");
        assertEquals(expectedTaxFirst, liveData.getTax(), 0.001,
                "First call: total tax should be 420");
        assertEquals(20000 - expectedTaxFirst, liveData.getCapital(), 0.001,
                "Capital after first call should be 19580");
        assertEquals(1000 - expectedTaxFirst, liveData.getReturned(), 0.001,
                "Returned after first call should be 580");

        testPhase.addTax();
        double expectedTaxSecond = 0;
        assertEquals(expectedTaxSecond, liveData.getCurrentTax(), 0.001,
                "Second call: current tax should be 0 when no additional difference");
        assertEquals(expectedTaxFirst, liveData.getTax(), 0.001,
                "Total tax should remain 420 after second call with no additional difference");
        assertEquals(19580, liveData.getCapital(), 0.001,
                "Capital should remain unchanged after second call");
        assertEquals(580, liveData.getReturned(), 0.001,
                "Returned should remain unchanged after second call");
    }

    @Test
    void testAddTax_MultipleSequentialCalls_WithAdditionalReturn() {
        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setCapital(20000);
        liveData.setReturned(1000);

        NotionalGainsTax taxRule = new NotionalGainsTax(42);
        taxRule.setPreviousReturned(0);
        dummySpec.setTaxRule(taxRule);

        liveData.setCurrentTax(0);
        liveData.setTax(0);

        testPhase.addTax();
        double expectedTaxFirst = (1000 * 42) / 100.0; // 420
        assertEquals(expectedTaxFirst, liveData.getCurrentTax(), 0.001,
                "First call: current tax should be 420");
        assertEquals(20000 - expectedTaxFirst, liveData.getCapital(), 0.001,
                "Capital after first call should be 19580");
        assertEquals(1000 - expectedTaxFirst, liveData.getReturned(), 0.001,
                "Returned after first call should be 580");

        liveData.setReturned(780);

        testPhase.addTax();

        double expectedTaxSecond = ((780 - 580) * 42) / 100.0; // 200 * 0.42 = 84.
        assertEquals(expectedTaxSecond, liveData.getCurrentTax(), 0.001,
                "Second call: current tax should be 84");
        assertEquals(504, liveData.getTax(), 0.001,
                "Total tax should accumulate to 504 after two calls");
        assertEquals(19580 - expectedTaxSecond, liveData.getCapital(), 0.001,
                "Capital should reflect cumulative tax deductions after second call");
        assertEquals(780 - expectedTaxSecond, liveData.getReturned(), 0.001,
                "Returned should reflect cumulative tax deductions after second call");
    }

    @Test
    void testAddInflation_MultipleCalls() {
        dummySpec.setInflation(new IInflation() {
            @Override
            public double calculatePercentage() {
                return 3;
            }
            @Override
            public IInflation copy() {
                return this;
            }
        });

        DummyLiveData liveData = dummySpec.getLiveData();
        liveData.setInflation(0);
        testPhase.addInflation();
        assertEquals(3, liveData.getInflation(), 0.001, "After one call, inflation should be 3");
        testPhase.addInflation();
        assertEquals(6, liveData.getInflation(), 0.001, "After two calls, inflation should accumulate to 6");
        testPhase.addInflation();
        assertEquals(9, liveData.getInflation(), 0.001, "After three calls, inflation should accumulate to 9");
    }

}
