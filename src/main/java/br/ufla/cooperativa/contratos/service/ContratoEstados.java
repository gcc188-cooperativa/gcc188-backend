package br.ufla.cooperativa.contratos.service;

import br.ufla.cooperativa.contratos.StatusContrato;
import br.ufla.cooperativa.shared.ConflitoException;
import br.ufla.cooperativa.shared.Papel;
import java.util.Map;

/** RN12: mapa de transições permitidas por estado e papel. */
public final class ContratoEstados {
    private ContratoEstados() {}

    public enum Acao { SUBMETER, APROVAR, REPROVAR, ENCERRAR, CANCELAR }

    private static final Map<StatusContrato, Map<Acao, StatusContrato>> TRANSICOES = Map.of(
            StatusContrato.RASCUNHO, Map.of(Acao.SUBMETER, StatusContrato.PENDENTE_APROVACAO, Acao.CANCELAR, StatusContrato.CANCELADO),
            StatusContrato.PENDENTE_APROVACAO, Map.of(Acao.APROVAR, StatusContrato.ATIVO, Acao.REPROVAR, StatusContrato.RASCUNHO,
                    Acao.CANCELAR, StatusContrato.CANCELADO),
            StatusContrato.ATIVO, Map.of(Acao.ENCERRAR, StatusContrato.ENCERRADO, Acao.CANCELAR, StatusContrato.CANCELADO),
            StatusContrato.ENCERRADO, Map.of(),
            StatusContrato.CANCELADO, Map.of());

    public static StatusContrato aplicar(StatusContrato atual, Acao acao) {
        StatusContrato destino = TRANSICOES.get(atual).get(acao);
        if (destino == null) {
            throw new ConflitoException("Transição inválida: " + acao + " a partir de " + atual + " (RN12)", null);
        }
        return destino;
    }

    public static boolean permitida(Papel papel, Acao acao) {
        return switch (acao) {
            case SUBMETER -> papel == Papel.MOTORISTA || papel == Papel.ADMINISTRADOR;
            default -> papel == Papel.ADMINISTRADOR;
        };
    }
}
