package br.ufla.cooperativa;

import static org.assertj.core.api.Assertions.assertThat;

import br.ufla.cooperativa.shared.Documentos;
import org.junit.jupiter.api.Test;

class DocumentosTest {
    @Test
    void cpf() {
        assertThat(Documentos.cpfValido("52998224725")).isTrue();
        assertThat(Documentos.cpfValido("52998224726")).isFalse();
        assertThat(Documentos.cpfValido("11111111111")).isFalse();
        assertThat(Documentos.cpfValido("123")).isFalse();
    }

    @Test
    void cnpj() {
        assertThat(Documentos.cnpjValido("11222333000181")).isTrue();
        assertThat(Documentos.cnpjValido("11222333000180")).isFalse();
        assertThat(Documentos.cnpjValido("00000000000000")).isFalse();
    }

    @Test
    void documentoGenerico() {
        assertThat(Documentos.cpfOuCnpjValido("52998224725")).isTrue();
        assertThat(Documentos.cpfOuCnpjValido("11222333000181")).isTrue();
        assertThat(Documentos.cpfOuCnpjValido("1234567890123")).isFalse();
    }
}
