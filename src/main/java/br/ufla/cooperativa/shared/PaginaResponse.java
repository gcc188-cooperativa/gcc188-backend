package br.ufla.cooperativa.shared;

import java.util.List;
import org.springframework.data.domain.Page;

/** Corpo padronizado de lista: { content, page, size, total_elements, total_pages }. */
public record PaginaResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> PaginaResponse<T> de(Page<T> p) {
        return new PaginaResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
