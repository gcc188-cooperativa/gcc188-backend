package br.ufla.cooperativa.shared;

import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Usuário autenticado: id, papel e vínculo (id do cooperando ou do cliente). */
public record Ator(Long id, Papel papel, Long vinculo) {
    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + papel.name()));
    }

    public boolean is(Papel p) {
        return papel == p;
    }
}
