package com.rpa.manage.common.util;

import com.rpa.manage.common.api.PageResult;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public final class PageUtils {

    private PageUtils() {
    }

    public static <T, R> PageResult<R> toPageResult(Page<T> page, Function<T, R> mapper) {
        List<R> rows = page.getContent().stream().map(mapper).toList();
        return new PageResult<>(page.getTotalElements(), page.getNumber() + 1, page.getSize(), rows);
    }
}
