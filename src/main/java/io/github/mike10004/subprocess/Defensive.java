package io.github.mike10004.subprocess;

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

import static java.util.stream.Collectors.toList;

class Defensive {

    private Defensive() {

    }

    public static <T> List<T> immutable(List<T> items) {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public static <T> List<T> listOf(Iterable<T> items) {
        return StreamSupport.stream(items.spliterator(), false).collect(toList());
    }

    public static <T> Set<T> immutable(Set<T> items) {
        return Collections.unmodifiableSet(new HashSet<>(items));
    }

    public static <K, V> Map<K, V> immutable(Map<K, V> items) {
        return Collections.unmodifiableMap(new HashMap<>(items));
    }

    public static <T> void requireAllNotNull(Iterable<T> items, String message) {
        requireAll(items, Objects::nonNull, message);
    }

    public static <T> void requireAll(Iterable<T> items, Predicate<? super T> condition, String message) {
        Preconditions.checkArgument(StreamSupport.stream(items.spliterator(), false).allMatch(condition), message);
    }

}
