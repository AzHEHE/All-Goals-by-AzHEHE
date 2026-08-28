package dev.allgoals.notification;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum NotificationPreference {
    DEFAULT,
    ON,
    OFF;

    public static final Codec<NotificationPreference> CODEC = Codec.STRING.xmap(
            value -> {
                try {
                    return valueOf(value.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    return DEFAULT;
                }
            },
            value -> value.name().toLowerCase(Locale.ROOT)
    );

}
