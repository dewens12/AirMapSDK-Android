package com.airmap.airmapsdk.util;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CopyCollections {

    @Nullable
    public static <T> ArrayList<T> copy(@Nullable List<T> list) {
        return list != null ? new ArrayList<>(list) : null;
    }

    @Nullable
    public static <T> HashSet<T> copy(@Nullable Set<T> set) {
        return set != null ? new HashSet<>(set) : null;
    }
}
