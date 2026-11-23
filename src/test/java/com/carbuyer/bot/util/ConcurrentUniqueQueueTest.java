package com.carbuyer.bot.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrentUniqueQueueTest {

    private ConcurrentUniqueQueue<Long> queue;

    private static final Long EXPECTED_AMOUNT = 1L;

    @BeforeEach
    void setup() {
        queue = new ConcurrentUniqueQueue<>();
    }

    @Test
    void pollFirst_shouldReturnFirstElement_whenSetHasNextTrue() {
        queue.addLast(EXPECTED_AMOUNT);

        Long result = queue.pollFirst();

        assertThat(result).isEqualTo(EXPECTED_AMOUNT);
    }

    @Test
    void pollFirst_shouldReturnNull_whenSetHasNotNextTrue() {
        Long result = queue.pollFirst();

        assertThat(result).isNull();
    }

    @Test
    void addLast_shouldAddLastElement() {
        queue.addLast(EXPECTED_AMOUNT);
    }

    @Test
    void addAll_shouldAddAllElements() {
        List<Long> expectedAmounts = List.of(1L, 2L, 3L);

        queue.addAll(expectedAmounts);
    }

    @Test
    void remove_shouldRemoveItemFromSet() {
        queue.addLast(EXPECTED_AMOUNT);

        queue.remove(EXPECTED_AMOUNT);
    }

    @Test
    void size_shouldReturnCorrectSize() {
        queue.addLast(EXPECTED_AMOUNT);

        int result = queue.size();

        assertThat(result).isEqualTo(1);
    }

    @Test
    void syncWith_shouldSyncInputItemsWithExistsAndAddedExcludedItems() {
        queue.addLast(EXPECTED_AMOUNT);
        Set<Long> validItems = Set.of(1L, 2L, 3L);
        Set<Long> excludedItems = Set.of(1L, 2L, 4L);

        queue.syncWith(validItems, excludedItems);

        int result = queue.size();

        assertThat(result).isEqualTo(2);
    }
}
