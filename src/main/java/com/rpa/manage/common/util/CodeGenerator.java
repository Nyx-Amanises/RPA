package com.rpa.manage.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public final class CodeGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private CodeGenerator() {
    }

    public static String next(String prefix) {
        int seq = COUNTER.updateAndGet(value -> value >= 999 ? 1 : value + 1);
        return prefix + LocalDateTime.now().format(FORMATTER) + String.format("%03d", seq);
    }
}
