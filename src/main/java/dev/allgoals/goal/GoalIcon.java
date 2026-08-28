package dev.allgoals.goal;

import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.Optional;

/**
 * Describes one texture that can represent a goal.
 *
 * <p>Most goals have one icon. Goals such as colored wool have several variants;
 * the UI can rotate through those variants without the tracking code knowing
 * anything about textures.</p>
 */
public record GoalIcon(Optional<String> variant, String label, Identifier texture, int width, int height) {
	public GoalIcon {
		variant = Objects.requireNonNull(variant, "variant");
		label = Objects.requireNonNull(label, "label");
		texture = Objects.requireNonNull(texture, "texture");

		if (label.isBlank()) {
			throw new IllegalArgumentException("A goal icon label cannot be blank");
		}
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("A goal icon must have positive dimensions");
		}
	}
}
