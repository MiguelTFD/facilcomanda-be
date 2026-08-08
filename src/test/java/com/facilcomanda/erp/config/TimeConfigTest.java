package com.facilcomanda.erp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Feature 033 — CA9: el valor por defecto de {@code app.timezone} es parte del
 * contrato. Si alguien despliega sin el {@code application.yml}, la aplicación
 * debe seguir estampando las fechas en hora de Lima.
 */
class TimeConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(TimeConfig.class);

    @Test
    void sinPropiedadDefinida_elBeanClockResuelveAmericaLima() {
        contextRunner.run(context -> assertThat(context.getBean(Clock.class).getZone())
                .isEqualTo(ZoneId.of("America/Lima")));
    }

    @Test
    void conPropiedadDefinida_elBeanClockUsaLaZonaConfigurada() {
        contextRunner.withPropertyValues("app.timezone=America/Bogota")
                .run(context -> assertThat(context.getBean(Clock.class).getZone())
                        .isEqualTo(ZoneId.of("America/Bogota")));
    }
}
