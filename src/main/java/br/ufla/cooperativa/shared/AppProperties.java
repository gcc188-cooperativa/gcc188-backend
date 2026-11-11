package br.ufla.cooperativa.shared;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app")
public record AppProperties(
        @DefaultValue Auth auth,
        @DefaultValue Bootstrap bootstrap,
        @DefaultValue("America/Sao_Paulo") String timezone) {

    public record Auth(
            String jwtSecret,
            @DefaultValue("true") boolean cookieSecure,
            @DefaultValue("15m") Duration accessTtl,
            @DefaultValue("7d") Duration refreshTtl,
            @DefaultValue("12") int senhaMinima,
            @DefaultValue("5") int maxFalhas,
            @DefaultValue("15m") Duration bloqueio,
            @DefaultValue("12") int bcryptCusto) {}

    public record Bootstrap(@DefaultValue Admin admin) {}

    public record Admin(String email, String password) {}
}
