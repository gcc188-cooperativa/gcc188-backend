package br.ufla.cooperativa.shared;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * RN10: restringe a consulta ao escopo do usuário. Registro fora do escopo simplesmente não
 * é encontrado (404). Os caminhos indicam como chegar ao id do cooperando ou do cliente.
 */
public final class EscopoDoUsuario {
    private EscopoDoUsuario() {}

    public static <T> Specification<T> de(Ator ator, List<String> viaCooperando, List<String> viaCliente) {
        return (root, query, cb) -> switch (ator.papel()) {
            case ADMINISTRADOR, FINANCEIRO -> cb.conjunction();
            case MOTORISTA -> (viaCooperando == null || ator.vinculo() == null)
                    ? cb.disjunction()
                    : cb.equal(caminho(root, viaCooperando), ator.vinculo());
            case CLIENTE -> (viaCliente == null || ator.vinculo() == null)
                    ? cb.disjunction()
                    : cb.equal(caminho(root, viaCliente), ator.vinculo());
        };
    }

    private static Path<?> caminho(Root<?> root, List<String> partes) {
        Path<?> path = root;
        for (String p : partes) path = path.get(p);
        return path;
    }
}
