package dev.allgoals.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, per-player progress. Replacing the attachment whenever it changes
 * makes Fabric persist and synchronize it reliably.
 */
public record PlayerGoalProgress(
        Set<String> completed,
        Map<String, Integer> counters,
        Map<String, List<String>> observations
) {
    private static final String COMMAND_REVOKED_GOALS = "command_revoked_goals";

    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(
            LinkedHashSet::new,
            ArrayList::new
    );

    public static final Codec<PlayerGoalProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STRING_SET_CODEC.optionalFieldOf("completed", Set.of()).forGetter(PlayerGoalProgress::completed),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("counters", Map.of()).forGetter(PlayerGoalProgress::counters),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf())
                    .optionalFieldOf("observations", Map.of()).forGetter(PlayerGoalProgress::observations)
    ).apply(instance, PlayerGoalProgress::new));

    public PlayerGoalProgress {
        completed = Collections.unmodifiableSet(new LinkedHashSet<>(completed));
        counters = Collections.unmodifiableMap(new LinkedHashMap<>(counters));

        Map<String, List<String>> copiedObservations = new LinkedHashMap<>();
        observations.forEach((key, values) -> copiedObservations.put(
                key,
                List.copyOf(new LinkedHashSet<>(values))
        ));
        observations = Collections.unmodifiableMap(copiedObservations);
    }

    public static PlayerGoalProgress empty() {
        return new PlayerGoalProgress(Set.of(), Map.of(), Map.of());
    }

    /**
     * Combines two independently recorded histories without double-counting
     * counters that may describe the same vanilla statistic.
     */
    public static PlayerGoalProgress merge(PlayerGoalProgress first, PlayerGoalProgress second) {
        Set<String> mergedCompleted = new LinkedHashSet<>(first.completed);
        mergedCompleted.addAll(second.completed);

        Map<String, Integer> mergedCounters = new LinkedHashMap<>(first.counters);
        second.counters.forEach((key, value) -> mergedCounters.merge(key, value, Math::max));

        Map<String, LinkedHashSet<String>> mergedObservationSets = new LinkedHashMap<>();
        first.observations.forEach((key, values) ->
                mergedObservationSets.put(key, new LinkedHashSet<>(values)));
        second.observations.forEach((key, values) ->
                mergedObservationSets.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).addAll(values));

        // A completion recorded by either player wins over an old manual revoke.
        LinkedHashSet<String> revoked = mergedObservationSets.get(COMMAND_REVOKED_GOALS);
        if (revoked != null) {
            revoked.removeAll(mergedCompleted);
            if (revoked.isEmpty()) mergedObservationSets.remove(COMMAND_REVOKED_GOALS);
        }

        Map<String, List<String>> mergedObservations = new LinkedHashMap<>();
        mergedObservationSets.forEach((key, values) ->
                mergedObservations.put(key, List.copyOf(values)));
        return new PlayerGoalProgress(mergedCompleted, mergedCounters, mergedObservations);
    }

    public boolean isComplete(String goalId) {
        return completed.contains(goalId);
    }

    /**
     * Whether normal gameplay may award this goal. Command-revoked goals stay
     * disabled until an operator grants them again.
     */
    public boolean canCompleteAutomatically(String goalId) {
        return !isComplete(goalId) && !observations(COMMAND_REVOKED_GOALS).contains(goalId);
    }

    public int counter(String key) {
        return counters.getOrDefault(key, 0);
    }

    public List<String> observations(String key) {
        return observations.getOrDefault(key, List.of());
    }

    public Editor edit() {
        return new Editor(this);
    }

    public static final class Editor {
        private final PlayerGoalProgress original;
        private Set<String> completed;
        private Map<String, Integer> counters;
        private final Map<String, LinkedHashSet<String>> changedObservations = new LinkedHashMap<>();
        private final Map<String, Set<String>> observationCache = new HashMap<>();
        private final Set<String> newlyCompleted = new LinkedHashSet<>();
        private boolean completedChanged;
        private boolean countersChanged;

        private Editor(PlayerGoalProgress original) {
            this.original = original;
            this.completed = original.completed;
            this.counters = original.counters;
        }

        public void complete(String goalId) {
            if (observationSet(COMMAND_REVOKED_GOALS).contains(goalId) || completed.contains(goalId)) return;
            mutableCompleted().add(goalId);
            newlyCompleted.add(goalId);
        }

        public void grantCompletion(String goalId) {
            if (observationSet(COMMAND_REVOKED_GOALS).contains(goalId)) {
                LinkedHashSet<String> revoked = mutableObservations(COMMAND_REVOKED_GOALS);
                revoked.remove(goalId);
            }
            complete(goalId);
        }

        public boolean isComplete(String goalId) {
            return completed.contains(goalId);
        }

        public void uncomplete(String goalId) {
            if (!completed.contains(goalId)) return;
            mutableCompleted().remove(goalId);
            newlyCompleted.remove(goalId);
        }

        public void revokeCompletion(String goalId) {
            uncomplete(goalId);
            mutableObservations(COMMAND_REVOKED_GOALS).add(goalId);
        }

        public void setCounter(String key, int value) {
            Integer current = counters.get(key);
            if (current != null && current == value) return;
            mutableCounters().put(key, value);
        }

        public void setCounterAtLeast(String key, int value) {
            if (value <= counter(key)) return;
            mutableCounters().put(key, value);
        }

        public int addToCounter(String key, int amount) {
            long next = (long) counter(key) + amount;
            int clamped = (int) Math.clamp(next, Integer.MIN_VALUE, Integer.MAX_VALUE);
            mutableCounters().put(key, clamped);
            return clamped;
        }

        public int counter(String key) {
            return counters.getOrDefault(key, 0);
        }

        public int observe(String key, String value) {
            Set<String> current = observationSet(key);
            if (current.contains(value)) return current.size();
            LinkedHashSet<String> values = mutableObservations(key);
            values.add(value);
            return values.size();
        }

        public int observationCount(String key) {
            return observationSet(key).size();
        }

        public void clearObservations(String key) {
            if (observationSet(key).isEmpty()) return;
            changedObservations.put(key, new LinkedHashSet<>());
            observationCache.remove(key);
        }

        public Set<String> observations(String key) {
            Set<String> values = observationSet(key);
            return values.isEmpty() ? Set.of() : Collections.unmodifiableSet(values);
        }

        public Set<String> newlyCompleted() {
            return Set.copyOf(newlyCompleted);
        }

        public int completedCount() {
            return completed.size();
        }

        public PlayerGoalProgress build() {
            if (!completedChanged && !countersChanged && changedObservations.isEmpty()) return original;

            Map<String, List<String>> savedObservations = new LinkedHashMap<>();
            original.observations.forEach((key, values) -> savedObservations.put(key, values));
            changedObservations.forEach((key, values) -> {
                if (values.isEmpty()) savedObservations.remove(key);
                else savedObservations.put(key, List.copyOf(values));
            });
            PlayerGoalProgress result = new PlayerGoalProgress(completed, counters, savedObservations);
            return result.equals(original) ? original : result;
        }

        private Set<String> mutableCompleted() {
            if (!completedChanged) {
                completed = new LinkedHashSet<>(completed);
                completedChanged = true;
            }
            return completed;
        }

        private Map<String, Integer> mutableCounters() {
            if (!countersChanged) {
                counters = new LinkedHashMap<>(counters);
                countersChanged = true;
            }
            return counters;
        }

        private Set<String> observationSet(String key) {
            LinkedHashSet<String> changed = changedObservations.get(key);
            if (changed != null) return changed;
            return observationCache.computeIfAbsent(key, ignored -> {
                List<String> saved = original.observations.get(key);
                return saved == null || saved.isEmpty()
                        ? Set.of()
                        : Collections.unmodifiableSet(new LinkedHashSet<>(saved));
            });
        }

        private LinkedHashSet<String> mutableObservations(String key) {
            LinkedHashSet<String> changed = changedObservations.get(key);
            if (changed != null) return changed;
            LinkedHashSet<String> copy = new LinkedHashSet<>(observationSet(key));
            changedObservations.put(key, copy);
            observationCache.remove(key);
            return copy;
        }
    }
}
