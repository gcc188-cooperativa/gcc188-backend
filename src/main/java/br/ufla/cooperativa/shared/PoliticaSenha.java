package br.ufla.cooperativa.shared;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Tamanho mínimo configurável (12), máximo de 72 bytes (limite do bcrypt), sem composição obrigatória. */
@Component
public class PoliticaSenha {
    private static final Set<String> COMUNS = Set.of(
            "123456789012", "1234567890123", "senha1234567", "password1234", "passwordpassword",
            "qwertyuiop12", "administrador", "administrador123", "cooperativa123", "mudar123456");

    private final int minimo;

    public PoliticaSenha(AppProperties props) {
        this.minimo = props.auth().senhaMinima();
    }

    public void validar(String senha, String campo) {
        if (senha == null || senha.length() < minimo)
            throw new RegraNegocioException("SENHA_FRACA", campo, "A senha deve ter ao menos " + minimo + " caracteres");
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new RegraNegocioException("SENHA_FRACA", campo, "A senha deve ter no máximo 72 bytes");
        if (COMUNS.contains(senha.toLowerCase()))
            throw new RegraNegocioException("SENHA_FRACA", campo, "Senha muito comum");
    }
}
