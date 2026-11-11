package br.ufla.cooperativa.shared;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
@EnableJpaAuditing(dateTimeProviderRef = "auditoriaDataHora")
public class ConfiguracaoBase {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    DateTimeProvider auditoriaDataHora(Clock clock) {
        return () -> Optional.of(clock.instant());
    }

    /** bcrypt com prefixo de algoritmo, o que permite migrar para Argon2 depois (RNF-01). */
    @Bean
    PasswordEncoder passwordEncoder(AppProperties props) {
        return new DelegatingPasswordEncoder(
                "bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder(props.auth().bcryptCusto())));
    }
}
