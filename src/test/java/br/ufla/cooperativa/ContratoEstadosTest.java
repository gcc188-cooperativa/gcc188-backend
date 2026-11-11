package br.ufla.cooperativa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.ufla.cooperativa.contratos.StatusContrato;
import br.ufla.cooperativa.contratos.service.ContratoEstados;
import br.ufla.cooperativa.contratos.service.ContratoEstados.Acao;
import br.ufla.cooperativa.shared.ConflitoException;
import br.ufla.cooperativa.shared.Papel;
import org.junit.jupiter.api.Test;

class ContratoEstadosTest {

    @Test
    void cicloFeliz() {
        var s = StatusContrato.RASCUNHO;
        s = ContratoEstados.aplicar(s, Acao.SUBMETER);
        assertThat(s).isEqualTo(StatusContrato.PENDENTE_APROVACAO);
        s = ContratoEstados.aplicar(s, Acao.APROVAR);
        assertThat(s).isEqualTo(StatusContrato.ATIVO);
        s = ContratoEstados.aplicar(s, Acao.ENCERRAR);
        assertThat(s).isEqualTo(StatusContrato.ENCERRADO);
    }

    @Test
    void reprovarDevolveARascunho() {
        assertThat(ContratoEstados.aplicar(StatusContrato.PENDENTE_APROVACAO, Acao.REPROVAR)).isEqualTo(StatusContrato.RASCUNHO);
    }

    @Test
    void cancelarValeParaQualquerEstadoNaoFinal() {
        for (var s : new StatusContrato[] {StatusContrato.RASCUNHO, StatusContrato.PENDENTE_APROVACAO, StatusContrato.ATIVO}) {
            assertThat(ContratoEstados.aplicar(s, Acao.CANCELAR)).isEqualTo(StatusContrato.CANCELADO);
        }
    }

    @Test
    void estadosFinaisNaoTemTransicao() {
        for (var s : new StatusContrato[] {StatusContrato.ENCERRADO, StatusContrato.CANCELADO}) {
            for (Acao a : Acao.values()) {
                assertThatThrownBy(() -> ContratoEstados.aplicar(s, a)).isInstanceOf(ConflitoException.class);
            }
        }
    }

    @Test
    void transicoesInvalidasResultamEmConflito() {
        assertThatThrownBy(() -> ContratoEstados.aplicar(StatusContrato.RASCUNHO, Acao.APROVAR)).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> ContratoEstados.aplicar(StatusContrato.PENDENTE_APROVACAO, Acao.ENCERRAR)).isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> ContratoEstados.aplicar(StatusContrato.ATIVO, Acao.SUBMETER)).isInstanceOf(ConflitoException.class);
    }

    @Test
    void somenteAdministradorAprovaReprovaEncerraECancela() {
        for (Acao a : new Acao[] {Acao.APROVAR, Acao.REPROVAR, Acao.ENCERRAR, Acao.CANCELAR}) {
            assertThat(ContratoEstados.permitida(Papel.ADMINISTRADOR, a)).isTrue();
            assertThat(ContratoEstados.permitida(Papel.MOTORISTA, a)).isFalse();
            assertThat(ContratoEstados.permitida(Papel.FINANCEIRO, a)).isFalse();
            assertThat(ContratoEstados.permitida(Papel.CLIENTE, a)).isFalse();
        }
        assertThat(ContratoEstados.permitida(Papel.MOTORISTA, Acao.SUBMETER)).isTrue();
        assertThat(ContratoEstados.permitida(Papel.FINANCEIRO, Acao.SUBMETER)).isFalse();
    }
}
