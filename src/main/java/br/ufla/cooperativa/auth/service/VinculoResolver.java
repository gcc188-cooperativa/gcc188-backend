package br.ufla.cooperativa.auth.service;

import br.ufla.cooperativa.clientes.persistence.Cliente;
import br.ufla.cooperativa.clientes.persistence.ClienteRepository;
import br.ufla.cooperativa.cooperados.persistence.Cooperado;
import br.ufla.cooperativa.cooperados.persistence.CooperadoRepository;
import br.ufla.cooperativa.usuarios.persistence.Usuario;
import org.springframework.stereotype.Component;

/** Descobre o id do cooperado ou do cliente vinculado ao usuário (RN14). */
@Component
public class VinculoResolver {
    private final CooperadoRepository cooperados;
    private final ClienteRepository clientes;

    public VinculoResolver(CooperadoRepository cooperados, ClienteRepository clientes) {
        this.cooperados = cooperados;
        this.clientes = clientes;
    }

    public Long de(Usuario u) {
        return switch (u.getPapel()) {
            case MOTORISTA -> cooperados.findByUsuarioId(u.getId()).map(Cooperado::getId).orElse(null);
            case CLIENTE -> clientes.findByUsuarioId(u.getId()).map(Cliente::getId).orElse(null);
            default -> null;
        };
    }
}
