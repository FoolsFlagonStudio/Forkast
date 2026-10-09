package com.forkast.backend.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.forkast.backend.ingest.RecipeLine;
import com.forkast.backend.ingest.Unit;
import com.forkast.backend.ingest.UnitConverter;
import com.forkast.backend.ingredient.Ingredient;

/**
 * Cost per serving, the way NutritionCalculator does macros: the same lines
 * count (required,
 * with an amount, not a pinch or "to taste"), and grams come from the same
 * UnitConverter.
 *
 * Each line costs its grams times the price per 100 g. When the grams can't be
 * worked out but
 * the line is a count ("2 limes") and there's a price per item, that's used
 * instead. Grams go
 * first because the resolver's price per 100 g is already the cheapest way to
 * buy the
 * ingredient (the 3 lb bag), while a price per item usually comes from the
 * loose ones.
 *
 * This is the cost of what the recipe uses, not what a shopper pays: half a
 * bottle of soy
 * sauce costs half a bottle. Whole packages belong to the grocery list.
 *
 * Prices come in through a function so this class needs no database and tests
 * can hand it
 * whatever prices they like.
 */
@Component
public class RecipeCostCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * Ingredients that cost nothing, so a soup isn't "incomplete" for want of a
     * water price
     */
    private static final Set<String> FREE = Set.of("water", "ice");

    private final UnitConverter unitConverter;

    public RecipeCostCalculator(UnitConverter unitConverter) {
        this.unitConverter = unitConverter;
    }

    public RecipeCost calculate(List<RecipeLine> lines, int baseServings,
            Function<Ingredient, Optional<ResolvedPrice>> priceOf) {
        if (baseServings < 1) {
            throw new IllegalArgumentException("baseServings must be at least 1");
        }

        BigDecimal total = BigDecimal.ZERO;
        boolean anyPriced = false;
        boolean complete = true;

        for (RecipeLine line : lines) {
            if (!line.countsTowardNutrition()) {
                continue;
            }
            Ingredient ingredient = line.ingredient();
            if (ingredient != null && FREE.contains(ingredient.getName().toLowerCase(Locale.ROOT))) {
                continue; // free, but not "priced": water alone shouldn't make a cost of $0.00
            }
            Optional<BigDecimal> cost = ingredient == null ? Optional.empty()
                    : lineCost(line, priceOf.apply(ingredient));
            if (cost.isEmpty()) {
                complete = false; // unmatched, no price, or no way to convert the amount
                continue;
            }
            total = total.add(cost.get());
            anyPriced = true;
        }

        BigDecimal perServing = anyPriced
                ? total.divide(BigDecimal.valueOf(baseServings), 2, RoundingMode.HALF_UP)
                : null;
        return new RecipeCost(perServing, complete);
    }

    private Optional<BigDecimal> lineCost(RecipeLine line, Optional<ResolvedPrice> price) {
        if (price.isEmpty()) {
            return Optional.empty();
        }
        ResolvedPrice p = price.get();

        if (p.per100g() != null) {
            Optional<BigDecimal> grams = unitConverter.toGrams(line.amount(), line.unit(), line.prepNote(),
                    line.ingredient());
            if (grams.isPresent()) {
                return Optional.of(grams.get().multiply(p.per100g()).divide(HUNDRED));
            }
        }
        if (p.perItem() != null && line.unit() == Unit.COUNT) {
            return Optional.of(line.amount().multiply(p.perItem()));
        }
        return Optional.empty();
    }
}