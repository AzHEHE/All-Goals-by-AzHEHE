package dev.allgoals.goal;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;

/**
 * Stable information shared by the server-side tracker and the client UI.
 *
 * <p>This is deliberately not a complete/incomplete value. Progress belongs to
 * a particular player and world, so it will live in a separate server-owned
 * progress store.</p>
 */
public record GoalDefinition(
		Identifier id,
		String sourceId,
		String displayName,
		boolean userAdded,
		boolean activeOnWiki,
		int rotationPeriodMillis,
		List<GoalIcon> icons
) {
	public GoalDefinition {
		id = Objects.requireNonNull(id, "id");
		sourceId = Objects.requireNonNull(sourceId, "sourceId");
		displayName = Objects.requireNonNull(displayName, "displayName");
		icons = List.copyOf(icons);

		if (sourceId.isBlank()) {
			throw new IllegalArgumentException("A source goal ID cannot be blank");
		}
		if (displayName.isBlank()) {
			throw new IllegalArgumentException("A goal display name cannot be blank");
		}
		if (icons.isEmpty()) {
			throw new IllegalArgumentException("A goal must have at least one icon");
		}
		if (icons.size() > 1 && rotationPeriodMillis <= 0) {
			throw new IllegalArgumentException("A rotating goal must have a positive rotation period");
		}
	}

	public boolean hasRotatingIcon() {
		return icons.size() > 1;
	}
}
