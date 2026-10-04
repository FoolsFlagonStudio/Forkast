package com.forkast.backend.ingest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

import com.forkast.backend.ingredient.IngredientTag;

/**
 * Assigns dietary label names (matching the dietary_labels rows) from
 * ingredient tags.
 *
 * Tag-based labels need every required line matched: a wrong "gluten-free" is
 * worse than a
 * missing one, so an unmatched line blocks them until someone reviews it.
 * Optional lines are
 * ignored. "high-protein" comes from nutrition instead of tags.
 */
@Component
public class DietaryLabelClassifier {

    static final BigDecimal HIGH_PROTEIN_GRAMS = new BigDecimal("25");

    /** Each label and the tags that rule it out. */
    private static final Map<String, Set<IngredientTag>> EXCLUDED_TAGS = new LinkedHashMap<>();

    static {
        Set<IngredientTag> meatAndFish = EnumSet.of(IngredientTag.MEAT, IngredientTag.POULTRY,
                IngredientTag.FISH, IngredientTag.SHELLFISH, IngredientTag.GELATIN);
        Set<IngredientTag> vegan = EnumSet.copyOf(meatAndFish);
        vegan.addAll(EnumSet.of(IngredientTag.DAIRY, IngredientTag.EGG, IngredientTag.HONEY));

        EXCLUDED_TAGS.put("vegetarian", meatAndFish);
        EXCLUDED_TAGS.put("vegan", vegan);
        EXCLUDED_TAGS.put("pescatarian", EnumSet.of(IngredientTag.MEAT, IngredientTag.POULTRY, IngredientTag.GELATIN));
        EXCLUDED_TAGS.put("dairy-free", EnumSet.of(IngredientTag.DAIRY));
        EXCLUDED_TAGS.put("egg-free", EnumSet.of(IngredientTag.EGG));
        EXCLUDED_TAGS.put("gluten-free", EnumSet.of(IngredientTag.GLUTEN));
        EXCLUDED_TAGS.put("nut-free", EnumSet.of(IngredientTag.TREE_NUT, IngredientTag.PEANUT));
        EXCLUDED_TAGS.put("shellfish-free", EnumSet.of(IngredientTag.SHELLFISH));
        EXCLUDED_TAGS.put("soy-free", EnumSet.of(IngredientTag.SOY));
    }

    public Set<String> classify(List<RecipeLine> lines, NutritionResult nutrition) {
        Set<String> labels = new TreeSet<>(); // sorted, so results are predictable in tests and logs

        List<RecipeLine> required = lines.stream().filter(line -> !line.optional()).toList();
        boolean allMatched = required.stream().allMatch(line -> line.ingredient() != null);

        if (allMatched) {
            Set<IngredientTag> present = EnumSet.noneOf(IngredientTag.class);
            required.forEach(line -> present.addAll(line.ingredient().getTags()));

            EXCLUDED_TAGS.forEach((label, excluded) -> {
                if (Collections.disjoint(present, excluded)) {
                    labels.add(label);
                }
            });
        }

        if (nutrition != null
                && nutrition.complete()
                && nutrition.proteinGPerServing() != null
                && nutrition.proteinGPerServing().compareTo(HIGH_PROTEIN_GRAMS) >= 0) {
            labels.add("high-protein");
        }
        return labels;
    }
}