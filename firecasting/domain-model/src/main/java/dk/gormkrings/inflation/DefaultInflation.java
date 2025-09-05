package dk.gormkrings.inflation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class DefaultInflation implements IInflation {
        private final double averageInflation;

        public DefaultInflation(double averageInflation) {
            this.averageInflation = averageInflation;
            log.debug("DefaultInflation created with inflation average: {}", averageInflation);
        }

        @Override
        public double calculateInflation() {
            return averageInflation;
        }

        @Override
        public IInflation copy() {
            return new DefaultInflation(this.averageInflation);
        }
}
