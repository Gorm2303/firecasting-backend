package dk.gormkrings.randomVariable;

import dk.gormkrings.math.distribution.IDistribution;
import dk.gormkrings.math.randomNumberGenerator.IRandomNumberGenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class DefaultRandomVariableTest {

    @Test
    public void testSampleReturnsExpectedValue() {
        IDistribution distributionMock = mock(IDistribution.class);
        IRandomNumberGenerator rngMock = mock(IRandomNumberGenerator.class);

        double expectedSample = 5.0;
        when(distributionMock.sample(rngMock)).thenReturn(expectedSample);

        DefaultRandomVariable randomVariable = new DefaultRandomVariable(distributionMock, rngMock);

        double result = randomVariable.sample();
        assertEquals(expectedSample, result, 0.0001);

        verify(distributionMock, times(1)).sample(rngMock);
    }
}
