package com.facilcomanda.erp.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fuente única de la hora con la que la aplicación estampa toda fecha
 * (feature 033). Los servicios reciben este {@link Clock} por constructor y
 * usan {@code LocalDateTime.now(clock)}: la zona deja de depender de la del
 * proceso, que en el contenedor de producción era UTC.
 *
 * <p>El valor por defecto {@code America/Lima} vive en el código a propósito:
 * si alguien despliega sin el {@code application.yml}, la zona sigue siendo la
 * correcta. Una garantía que solo vive en la configuración del entorno se
 * pierde en la siguiente migración de servidor.</p>
 *
 * <p>Se configura con un identificador IANA, nunca con un offset fijo como
 * {@code -05:00}: aunque hoy Perú no aplique horario de verano, un offset fijo
 * es una afirmación que caduca.</p>
 */
@Configuration
public class TimeConfig {

    private final String appTimezone;

    public TimeConfig(@Value("${app.timezone:America/Lima}") String appTimezone) {
        this.appTimezone = appTimezone;
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(appTimezone));
    }
}
