package dk.gormkrings.config;

import dk.gormkrings.calendar.TradingCalendar;
import dk.gormkrings.calendar.WeekdayTradingCalendar;
import dk.gormkrings.simulation.ReturnStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class SimulationRuntimeConfig {

    @Bean
    public ReturnStep simulationReturnStep(
            @Value("${simulation.return.step:daily}") String configuredStep
    ) {
        ReturnStep step = ReturnStep.fromProperty(configuredStep);
        log.info("Simulation return step configured: {} (dt={})", step, step.toDt());
        return step;
    }

    @Bean
    public TradingCalendar tradingCalendar(
            @Value("${simulation.trading.calendar:weekday}") String configuredCalendar
    ) {
        String key = (configuredCalendar == null) ? "" : configuredCalendar.trim().toLowerCase();
        TradingCalendar calendar = switch (key) {
            case "", "weekday", "weekdays" -> new WeekdayTradingCalendar();
            default -> {
                log.warn("Unknown simulation.trading.calendar='{}'; falling back to weekday calendar", configuredCalendar);
                yield new WeekdayTradingCalendar();
            }
        };
        log.info("Simulation trading calendar configured: {}", calendar.getClass().getSimpleName());
        return calendar;
    }
}
