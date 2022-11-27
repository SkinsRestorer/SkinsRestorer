package net.skinsrestorer.shared.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FluentList {
    public static List<String> listOf(String... elements) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, elements);
        return Collections.unmodifiableList(list);
    }
}
