package dev.allgoals.goal;

import java.util.Locale;
import java.util.Set;

public final class VariantGoalIds {
    public static final String COLORED_SHEEP = "KILL_COLORED_SHEEP";
    public static final String COLORED_CONCRETE = "OBTAIN_64_COLORED_CONCRETE";
    public static final String COLORED_WOOL = "OBTAIN_64_COLORED_WOOL";
    public static final String GLAZED_TERRACOTTA = "OBTAIN_COLORED_GLAZED_TERRACOTTA";
    public static final String DYED_LEATHER = "WEAR_COLORED_LEATHER_ARMOR_PIECE";

    public static final Set<String> FAMILIES = Set.of(
            COLORED_SHEEP, COLORED_CONCRETE, COLORED_WOOL, GLAZED_TERRACOTTA, DYED_LEATHER
    );

    private VariantGoalIds() {
    }

    public static String goalId(String familyId, String variant) {
        return familyId + "__" + variant.toUpperCase(Locale.ROOT)
                .replace('&', '_')
                .replaceAll("[^A-Z0-9_]", "_");
    }

    public static boolean belongsToFamily(String goalId, String familyId) {
        return goalId.startsWith(familyId + "__");
    }

    public static boolean isVariantGoal(String goalId) {
        return FAMILIES.stream().anyMatch(family -> belongsToFamily(goalId, family));
    }
}
