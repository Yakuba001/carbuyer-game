package com.carbuyer.bot.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentUniqueQueue<T> {

    private final Set<T> set = new LinkedHashSet<>();
    private final ReentrantLock lock = new ReentrantLock();

    public T pollFirst() {
        lock.lock();
        try {
            Iterator<T> iterator = set.iterator();
            if (!iterator.hasNext()) return null;
            T first = iterator.next();
            iterator.remove();
            return first;
        } finally {
            lock.unlock();
        }
    }

    public void addLast(T item) {
        lock.lock();
        try {
            set.add(item);
        } finally {
            lock.unlock();
        }
    }

    public void addAll(Collection<T> items) {
        lock.lock();
        try {
            set.addAll(items);
        } finally {
            lock.unlock();
        }
    }

    public boolean remove(T item) {
        lock.lock();
        try {
            return set.remove(item);
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return set.size();
        } finally {
            lock.unlock();
        }
    }

    public void syncWith(Set<T> validItems, Set<T> excludedItems) {
        lock.lock();
        try {
            set.retainAll(validItems);
            for (T item : validItems) {
                if (!excludedItems.contains(item)) {
                    set.add(item);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
