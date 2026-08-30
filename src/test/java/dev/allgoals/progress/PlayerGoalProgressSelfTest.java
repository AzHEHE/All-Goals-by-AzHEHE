package dev.allgoals.progress;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dependency-free regression checks for the copy-on-write progress editor.
 */
public final class PlayerGoalProgressSelfTest {
    private PlayerGoalProgressSelfTest() {
    }

    public static void main(String[] args) {
        PlayerGoalProgress empty = PlayerGoalProgress.empty();
        require(empty.edit().build() == empty, "An unchanged edit must reuse its original instance");
        require(empty.canCompleteAutomatically("GOAL_A"), "A new goal should be automatically completable");

        PlayerGoalProgress.Editor firstEdit = empty.edit();
        firstEdit.complete("GOAL_A");
        firstEdit.observe("foods", "apple");
        firstEdit.setCounterAtLeast("kills", 3);
        PlayerGoalProgress first = firstEdit.build();
        require(first.isComplete("GOAL_A"), "New completion was not saved");
        require(!first.canCompleteAutomatically("GOAL_A"), "A completed goal should not be completable again");
        require(first.observations("foods").equals(List.of("apple")), "New observation was not saved");
        require(first.counter("kills") == 3, "Counter was not saved");

        PlayerGoalProgress.Editor repeatedEdit = first.edit();
        repeatedEdit.complete("GOAL_A");
        repeatedEdit.observe("foods", "apple");
        repeatedEdit.setCounterAtLeast("kills", 2);
        require(repeatedEdit.build() == first, "Repeated progress must not create a replacement object");

        PlayerGoalProgress.Editor revokeEdit = first.edit();
        revokeEdit.revokeCompletion("GOAL_A");
        PlayerGoalProgress revoked = revokeEdit.build();
        require(!revoked.isComplete("GOAL_A"), "Revoked completion remained complete");
        require(revoked.canCompleteAutomatically("GOAL_A"), "A revoked goal could not be earned again");

        PlayerGoalProgress.Editor automaticEdit = revoked.edit();
        automaticEdit.complete("GOAL_A");
        PlayerGoalProgress reacquired = automaticEdit.build();
        require(reacquired.isComplete("GOAL_A"), "Gameplay did not reacquire a revoked goal");

        PlayerGoalProgress.Editor grantEdit = reacquired.edit();
        grantEdit.revokeCompletion("GOAL_A");
        grantEdit.grantCompletion("GOAL_A");
        PlayerGoalProgress granted = grantEdit.build();
        require(granted.isComplete("GOAL_A"), "Explicit grant did not clear a command revoke");

        PlayerGoalProgress.Editor clearEdit = granted.edit();
        clearEdit.clearObservations("foods");
        PlayerGoalProgress cleared = clearEdit.build();
        require(cleared.observations("foods").isEmpty(), "Observation history was not cleared");

        PlayerGoalProgress nearMaximum = new PlayerGoalProgress(
                Set.of(), Map.of("counter", Integer.MAX_VALUE - 1), Map.of());
        PlayerGoalProgress.Editor saturatingEdit = nearMaximum.edit();
        require(saturatingEdit.addToCounter("counter", 10) == Integer.MAX_VALUE,
                "Counter overflow was not safely saturated");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
