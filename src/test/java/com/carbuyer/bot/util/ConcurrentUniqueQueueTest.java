package com.carbuyer.bot.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrentUniqueQueueTest {

    private ConcurrentUniqueQueue<Long> queue;

    @BeforeEach
    void setup() {
        queue = new ConcurrentUniqueQueue<>();
    }

    @Test
    void pollFirst_shouldReturnFirstElement_whenSetHasNextTrue() {
        Long expectedAmount = 1L;
        queue.addLast(expectedAmount);

        Long result = queue.pollFirst();

        assertThat(result).isEqualTo(expectedAmount);
    }

    @Test
    void pollFirst_shouldReturnNull_whenSetHasNotNextTrue() {
        Long result = queue.pollFirst();

        assertThat(result).isNull();
    }

    @Test
    void addLast_shouldAddLastElement() {
        Long expectedAmount = 1L;

        queue.addLast(expectedAmount);
    }

    @Test
    void addAll_shouldAddAllElements() {
        List<Long> expectedAmounts = List.of(1L, 2L, 3L);

        queue.addAll(expectedAmounts);
    }

    @Test
    void remove_shouldRemoveItemFromSet() {
        Long expectedAmount = 1L;
        queue.addLast(expectedAmount);

        queue.remove(expectedAmount);
    }

    @Test
    void size_shouldReturnCorrectSize() {
        Long expectedAmount = 1L;
        queue.addLast(expectedAmount);

        int result = queue.size();

        assertThat(result).isEqualTo(1);
    }

    @Test
    void syncWith_shouldSyncInputItemsWithExistsAndAddedExcludedItems() {
        Long expectedAmount = 1L;
        queue.addLast(expectedAmount);
        Set<Long> validItems = Set.of(1L, 2L, 3L);
        Set<Long> excludedItems = Set.of(1L, 2L, 4L);

        queue.syncWith(validItems, excludedItems);

        int result = queue.size();

        assertThat(result).isEqualTo(2);
    }
}
