package br.ufla.cooperativa.shared;

import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Paginacao {
    private Paginacao() {}

    /** @param permitidos mapa nome-no-contrato (snake_case) para propriedade da entidade. */
    public static Pageable de(int page, int size, String sort, Map<String, String> permitidos, Sort padrao) {
        if (page < 0) throw new RequisicaoInvalidaException("page deve ser maior ou igual a 0");
        if (size < 1 || size > 100) throw new RequisicaoInvalidaException("size deve estar entre 1 e 100");
        Sort s = padrao;
        if (sort != null && !sort.isBlank()) {
            String[] partes = sort.split(",");
            String campo = partes[0].trim();
            String propriedade = permitidos.get(campo);
            if (propriedade == null) throw new RequisicaoInvalidaException("Campo de ordenação não permitido: " + campo);
            Sort.Direction dir = Sort.Direction.ASC;
            if (partes.length > 1) {
                String d = partes[1].trim().toLowerCase();
                if (d.equals("desc")) dir = Sort.Direction.DESC;
                else if (!d.equals("asc")) throw new RequisicaoInvalidaException("Direção de ordenação inválida: " + d);
            }
            s = Sort.by(dir, propriedade).and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, size, s);
    }
}
