package dev.allgoals.network;

import java.util.Objects;

public final class ModVersionPolicy {
    private ModVersionPolicy() {
    }

    public static boolean matches(String localVersion, String remoteVersion) {
        return localVersion != null
                && remoteVersion != null
                && Objects.equals(localVersion, remoteVersion);
    }
}
