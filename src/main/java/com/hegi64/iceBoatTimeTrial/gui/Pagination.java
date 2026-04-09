package com.hegi64.iceBoatTimeTrial.gui;

import java.util.Collections;
import java.util.List;

public final class Pagination {
    private Pagination() {
    }

    public static <T> List<T> page(List<T> source, int pageIndex, int pageSize) {
        if (source == null || source.isEmpty() || pageSize <= 0 || pageIndex < 0) {
            return List.of();
        }
        int from = pageIndex * pageSize;
        if (from >= source.size()) {
            return List.of();
        }
        int to = Math.min(source.size(), from + pageSize);
        return Collections.unmodifiableList(source.subList(from, to));
    }

    public static int lastPageIndex(int totalItems, int pageSize) {
        if (totalItems <= 0 || pageSize <= 0) {
            return 0;
        }
        return Math.max(0, (int) Math.ceil(totalItems / (double) pageSize) - 1);
    }
}

