package dev.allgoals.world;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum RunMode {
    ALL_GOALS,
    NONE;

    public static final Codec<RunMode> CODEC = Codec.STRING.xmap(value -> {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ALL_GOALS;
        }
    }, value -> value.name().toLowerCase(Locale.ROOT));
}
