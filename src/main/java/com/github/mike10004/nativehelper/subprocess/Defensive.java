package com.github.mike10004.nativehelper.subprocess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class Defensive {

    private Defensive() {

    }

    public static <T> List<T> copyOf(List<T> items) {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public static <T> List<T> listOf(Iterable<T> items) {
        return StreamSupport.stream(items.spliterator(), false).collect(toList());
    }

    public static <T> Set<T> copyOf(Set<T> items) {
        return Collections.unmodifiableSet(new HashSet<>(items));
    }

    public static <K, V> Map<K, V> copyOf(Map<K, V> items) {
        return Collections.unmodifiableMap(new HashMap<>(items));
    }

    public static <T> void requireAllNotNull(Iterable<T> items, String message) {
        requireAll(items, Objects::nonNull, message);
    }

    public static <T> void requireAll(Iterable<T> items, Predicate<? super T> condition, String message) {
        Preconditions.checkArgument(StreamSupport.stream(items.spliterator(), false).allMatch(condition), message);
    }

    public static <T> Collector<T, ?, List<T>> toList() {
        return Collectors.toList();
    }
}
