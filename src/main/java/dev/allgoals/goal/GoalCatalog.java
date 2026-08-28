package dev.allgoals.goal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.allgoals.AllGoals;
import dev.allgoals.progress.PlayerGoalProgress;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Loads the bundled goal metadata and provides immutable lookup collections.
 */
public final class GoalCatalog {
	private static final String MANIFEST_RESOURCE = "/assets/all_goals/goals/icon_manifest.json";
	private static final Pattern SOURCE_ID_PATTERN = Pattern.compile("[A-Z0-9_]+");

	private final List<GoalDefinition> goals;
	private final Map<Identifier, GoalDefinition> goalsById;
	private final Map<String, GoalDefinition> goalsBySourceId;
	private final int iconCount;
	private final long rotatingGoalCount;

	private GoalCatalog(List<GoalDefinition> goals, int iconCount) {
		this.goals = List.copyOf(goals);
		this.iconCount = iconCount;

		Map<Identifier, GoalDefinition> byId = new LinkedHashMap<>();
		Map<String, GoalDefinition> bySourceId = new LinkedHashMap<>();
		for (GoalDefinition goal : goals) {
			GoalDefinition previous = byId.put(goal.id(), goal);
			if (previous != null) {
				throw new IllegalStateException("Duplicate goal ID in manifest: " + goal.id());
			}
			GoalDefinition previousSource = bySourceId.put(goal.sourceId(), goal);
			if (previousSource != null) {
				throw new IllegalStateException("Duplicate source goal ID in manifest: " + goal.sourceId());
			}
		}
		this.goalsById = Map.copyOf(byId);
		this.goalsBySourceId = Map.copyOf(bySourceId);
		this.rotatingGoalCount = goals.stream().filter(GoalDefinition::hasRotatingIcon).count();
	}

	public static GoalCatalog loadBundled() {
		try (InputStream stream = GoalCatalog.class.getResourceAsStream(MANIFEST_RESOURCE)) {
			if (stream == null) {
				throw new IllegalStateException("Missing bundled goal manifest: " + MANIFEST_RESOURCE);
			}

			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				return read(reader);
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Could not read bundled goal manifest", exception);
		}
	}

	static GoalCatalog read(Reader reader) {
		JsonObject root = requireObject(JsonParser.parseReader(reader), "manifest root");
		int schemaVersion = requireInt(root, "schemaVersion");
		if (schemaVersion != 1) {
			throw new IllegalStateException("Unsupported goal manifest schema version: " + schemaVersion);
		}

		JsonArray goalEntries = requireArray(root, "goals");
		List<GoalDefinition> goals = new ArrayList<>(goalEntries.size());
		int parsedIconCount = 0;

		for (int goalIndex = 0; goalIndex < goalEntries.size(); goalIndex++) {
			JsonObject goalEntry = requireObject(goalEntries.get(goalIndex), "goals[" + goalIndex + "]");
			String sourceId = requireString(goalEntry, "goalId");
			if (!SOURCE_ID_PATTERN.matcher(sourceId).matches()) {
				throw new IllegalStateException("Invalid source goal ID: " + sourceId);
			}

			Identifier goalId = Identifier.fromNamespaceAndPath(
					AllGoals.MOD_ID,
					sourceId.toLowerCase(Locale.ROOT)
			);
			JsonArray iconEntries = requireArray(goalEntry, "icons");
			List<GoalIcon> icons = new ArrayList<>(iconEntries.size());

			for (int iconIndex = 0; iconIndex < iconEntries.size(); iconIndex++) {
				JsonObject iconEntry = requireObject(
						iconEntries.get(iconIndex),
						"icons[" + iconIndex + "] for " + sourceId
				);
				Identifier texture = Identifier.parse(requireString(iconEntry, "texture"));
				validateTexture(texture, sourceId);
				icons.add(new GoalIcon(
						optionalString(iconEntry, "variant"),
						requireString(iconEntry, "label"),
						texture,
						requireInt(iconEntry, "width"),
						requireInt(iconEntry, "height")
				));
			}

			parsedIconCount += icons.size();
			String displayName = requireString(goalEntry, "name");
			boolean userAdded = requireBoolean(goalEntry, "userAdded");
			boolean activeOnWiki = requireBoolean(goalEntry, "activeOnWiki");
			if (VariantGoalIds.FAMILIES.contains(sourceId)) {
				for (GoalIcon icon : icons) {
					String variant = icon.variant().orElseThrow(() ->
							new IllegalStateException("Split goal family " + sourceId + " has an icon without a variant"));
					String variantSourceId = VariantGoalIds.goalId(sourceId, variant);
					goals.add(new GoalDefinition(
							Identifier.fromNamespaceAndPath(AllGoals.MOD_ID, variantSourceId.toLowerCase(Locale.ROOT)),
							variantSourceId,
							variantDisplayName(icon.label()),
							userAdded,
							activeOnWiki,
							0,
							List.of(icon)
					));
				}
			} else {
				goals.add(new GoalDefinition(
						goalId,
						sourceId,
						displayName,
						userAdded,
						activeOnWiki,
						optionalInt(goalEntry, "rotationPeriodMs").orElse(0),
						icons
				));
			}
		}

		int declaredGoalCount = requireInt(root, "goalCount");
		int declaredIconCount = requireInt(root, "iconCount");
		if (declaredGoalCount != goalEntries.size()) {
			throw new IllegalStateException(
					"Goal count mismatch: manifest declares " + declaredGoalCount
							+ " but contains " + goalEntries.size() + " entries"
			);
		}
		if (declaredIconCount != parsedIconCount) {
			throw new IllegalStateException(
					"Icon count mismatch: manifest declares " + declaredIconCount + " but contains " + parsedIconCount
			);
		}

		return new GoalCatalog(goals, parsedIconCount);
	}

	private static String variantDisplayName(String label) {
		return label
				.replace("Leather Helmet", "Leather Cap")
				.replace("Leather Chestplate", "Leather Tunic")
				.replace("Leather Leggings", "Leather Pants");
	}

	public List<GoalDefinition> goals() {
		return goals;
	}

	public Optional<GoalDefinition> find(Identifier id) {
		return Optional.ofNullable(goalsById.get(Objects.requireNonNull(id, "id")));
	}

	public Optional<GoalDefinition> findSource(String sourceId) {
		return Optional.ofNullable(goalsBySourceId.get(Objects.requireNonNull(sourceId, "sourceId")));
	}

	public int goalCount() {
		return goals.size();
	}

	public int iconCount() {
		return iconCount;
	}

	public long rotatingGoalCount() {
		return rotatingGoalCount;
	}

	public int completedCount(PlayerGoalProgress progress) {
		int completed = 0;
		for (String sourceId : progress.completed()) {
			if (goalsBySourceId.containsKey(sourceId)) completed++;
		}
		return completed;
	}

	public boolean allComplete(PlayerGoalProgress progress) {
		Set<String> completed = progress.completed();
		return completed.size() >= goalsBySourceId.size()
				&& completed.containsAll(goalsBySourceId.keySet());
	}

	private static void validateTexture(Identifier texture, String sourceId) {
		if (!texture.getNamespace().equals(AllGoals.MOD_ID)) {
			throw new IllegalStateException("Goal " + sourceId + " uses another namespace: " + texture);
		}
		if (!texture.getPath().startsWith("textures/goals/") || !texture.getPath().endsWith(".png")) {
			throw new IllegalStateException("Goal " + sourceId + " uses an invalid texture path: " + texture);
		}

		String classpathResource = "/assets/" + texture.getNamespace() + "/" + texture.getPath();
		if (GoalCatalog.class.getResource(classpathResource) == null) {
			throw new IllegalStateException("Goal " + sourceId + " references a missing texture: " + texture);
		}
	}

	private static JsonObject requireObject(JsonElement element, String location) {
		if (element == null || !element.isJsonObject()) {
			throw new IllegalStateException("Expected a JSON object at " + location);
		}
		return element.getAsJsonObject();
	}

	private static JsonArray requireArray(JsonObject object, String member) {
		JsonElement value = object.get(member);
		if (value == null || !value.isJsonArray()) {
			throw new IllegalStateException("Expected JSON array member '" + member + "'");
		}
		return value.getAsJsonArray();
	}

	private static String requireString(JsonObject object, String member) {
		JsonElement value = object.get(member);
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
			throw new IllegalStateException("Expected string member '" + member + "'");
		}
		return value.getAsString();
	}

	private static int requireInt(JsonObject object, String member) {
		JsonElement value = object.get(member);
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
			throw new IllegalStateException("Expected integer member '" + member + "'");
		}
		return value.getAsInt();
	}

	private static boolean requireBoolean(JsonObject object, String member) {
		JsonElement value = object.get(member);
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
			throw new IllegalStateException("Expected boolean member '" + member + "'");
		}
		return value.getAsBoolean();
	}

	private static Optional<String> optionalString(JsonObject object, String member) {
		JsonElement value = object.get(member);
		if (value == null || value.isJsonNull()) {
			return Optional.empty();
		}
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
			throw new IllegalStateException("Expected optional string member '" + member + "'");
		}
		return Optional.of(value.getAsString());
	}

	private static Optional<Integer> optionalInt(JsonObject object, String member) {
		JsonElement value = object.get(member);
		if (value == null || value.isJsonNull()) {
			return Optional.empty();
		}
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
			throw new IllegalStateException("Expected optional integer member '" + member + "'");
		}
		return Optional.of(value.getAsInt());
	}
}
