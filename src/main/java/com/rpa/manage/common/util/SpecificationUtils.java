package com.rpa.manage.common.util;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.util.StringUtils;

public final class SpecificationUtils {

    private SpecificationUtils() {
    }

    public static void addLike(List<Predicate> predicates, String value, Function<String, Predicate> predicateBuilder) {
        if (StringUtils.hasText(value)) {
            predicates.add(predicateBuilder.apply(value.trim()));
        }
    }

    public static void addEqual(List<Predicate> predicates, Object value, Function<Object, Predicate> predicateBuilder) {
        if (value != null) {
            predicates.add(predicateBuilder.apply(value));
        }
    }

    public static <T extends Comparable<? super T>> void addBetween(
            List<Predicate> predicates,
            T start,
            T end,
            Path<T> path,
            jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        if (start != null) {
            predicates.add(cb.greaterThanOrEqualTo(path, start));
        }
        if (end != null) {
            predicates.add(cb.lessThanOrEqualTo(path, end));
        }
    }

    public static List<Predicate> newPredicates() {
        return new ArrayList<>();
    }
}
