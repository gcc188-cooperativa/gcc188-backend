package br.ufla.cooperativa.shared;

import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;

public final class Specs {
    private Specs() {}

    public static <T> Specification<T> todos(List<Specification<T>> filtros) {
        return (root, query, cb) -> {
            Predicate[] ps = filtros.stream()
                    .map(s -> s.toPredicate(root, query, cb))
                    .filter(Objects::nonNull)
                    .toArray(Predicate[]::new);
            return cb.and(ps);
        };
    }
}
