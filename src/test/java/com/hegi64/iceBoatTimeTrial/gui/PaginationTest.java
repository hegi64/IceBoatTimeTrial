package com.hegi64.iceBoatTimeTrial.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaginationTest {

    @Test
    void returnsExpectedPageSlice() {
        List<Integer> page = Pagination.page(List.of(1, 2, 3, 4, 5, 6, 7), 1, 3);
        assertEquals(List.of(4, 5, 6), page);
    }

    @Test
    void returnsEmptyForOutOfBounds() {
        List<Integer> page = Pagination.page(List.of(1, 2), 5, 3);
        assertEquals(List.of(), page);
    }

    @Test
    void computesLastPageIndex() {
        assertEquals(0, Pagination.lastPageIndex(0, 9));
        assertEquals(0, Pagination.lastPageIndex(5, 9));
        assertEquals(1, Pagination.lastPageIndex(10, 9));
        assertEquals(2, Pagination.lastPageIndex(27, 9));
    }
}

