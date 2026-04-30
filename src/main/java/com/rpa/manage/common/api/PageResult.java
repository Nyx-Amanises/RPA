package com.rpa.manage.common.api;

import java.util.List;

public record PageResult<T>(
        long total,
        int pageNum,
        int pageSize,
        List<T> list
) {
}
