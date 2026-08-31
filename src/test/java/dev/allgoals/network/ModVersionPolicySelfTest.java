package dev.allgoals.network;

public final class ModVersionPolicySelfTest {
    private ModVersionPolicySelfTest() {
    }

    public static void main(String[] args) {
        expect(ModVersionPolicy.matches("1.0.2", "1.0.2"), "identical versions must match");
        expect(!ModVersionPolicy.matches("1.0.2", "1.0.1"), "patch mismatches must be rejected");
        expect(!ModVersionPolicy.matches("1.0.2", "1.1.0"), "minor mismatches must be rejected");
        expect(!ModVersionPolicy.matches("1.0.2", null), "missing remote versions must be rejected");
        expect(!ModVersionPolicy.matches(null, "1.0.2"), "missing local versions must be rejected");
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
