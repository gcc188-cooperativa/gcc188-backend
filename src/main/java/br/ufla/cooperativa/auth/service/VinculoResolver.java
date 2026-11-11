package br.ufla.cooperativa.auth.service;

import br.ufla.cooperativa.clientes.persistence.Cliente;
import br.ufla.cooperativa.clientes.persistence.ClienteRepository;
import br.ufla.cooperativa.cooperandos.persistence.Cooperando;
import br.ufla.cooperativa.cooperandos.persistence.CooperandoRepository;
import br.ufla.cooperativa.usuarios.persistence.Usuario;
import org.springframework.stereotype.Component;

/** Descobre o id do cooperando ou do cliente vinculado ao usuário (RN14). */
@Component
public class VinculoResolver {
    private final CooperandoRepository cooperandos;
    private final ClienteRepository clientes;

    public VinculoResolver(CooperandoRepository cooperandos, ClienteRepository clientes) {
        this.cooperandos = cooperandos;
        this.clientes = clientes;
    }

    public Long de(Usuario u) {
        return switch (u.getPapel()) {
            case MOTORISTA -> cooperandos.findByUsuarioId(u.getId()).map(Cooperando::getId).orElse(null);
            case CLIENTE -> clientes.findByUsuarioId(u.getId()).map(Cliente::getId).orElse(null);
            default -> null;
        };
    }
}
