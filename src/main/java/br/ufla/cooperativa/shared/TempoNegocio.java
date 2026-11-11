package br.ufla.cooperativa.shared;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/** Único ponto de acesso ao relógio (D40). "Hoje" é calculado em America/Sao_Paulo. */
@Component
public class TempoNegocio {
    private final Clock clock;
    private final ZoneId zona;

    public TempoNegocio(Clock clock, AppProperties props) {
        this.clock = clock;
        this.zona = ZoneId.of(props.timezone());
    }

    public LocalDate hoje() {
        return LocalDate.now(clock.withZone(zona));
    }

    public Instant agora() {
        return Instant.now(clock);
    }
}
