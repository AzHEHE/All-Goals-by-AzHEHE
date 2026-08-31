package dev.allgoals.world;

import java.util.UUID;

public final class WorldRunIdentitySelfTest {
    private WorldRunIdentitySelfTest() {
    }

    public static void main(String[] args) {
        String existing = UUID.randomUUID().toString();
        expect(existing.equals(RunModeSavedData.normalizeRunId(existing)),
                "saved world identities must remain stable");

        String first = RunModeSavedData.normalizeRunId("");
        String second = RunModeSavedData.normalizeRunId(null);
        UUID.fromString(first);
        UUID.fromString(second);
        expect(!first.equals(second), "new worlds must receive unique identities");
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
