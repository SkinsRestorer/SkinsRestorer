package net.skinsrestorer.shared.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Pair<L, R> {
    private final L left;
    private final R right;
}
