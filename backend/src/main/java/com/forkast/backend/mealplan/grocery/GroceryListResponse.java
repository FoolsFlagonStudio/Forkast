package com.forkast.backend.mealplan.grocery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.forkast.backend.mealplan.GroceryListItem;
import com.forkast.backend.mealplan.MealPlan;

/**
 * A plan's grocery list in two groups: toBuy, and pantry ("check your pantry":
 * staples counted
 * at what the week uses unless marked needToBuy). unlistedLines counts recipe
 * lines with no
 * matched ingredient yet, which can't be listed or priced.
 */
public record GroceryListResponse(
        UUID planId,
        LocalDate weekStart,
        BigDecimal budget,
        BigDecimal usageCost,
        BigDecimal groceryCost,
        boolean overBudget,
        List<Item> toBuy,
        List<Item> pantry,
        int unlistedLines) {

    /**
     * One line. amount and unit are exact ("g" for weights); displayAmount is
     * rounded for
     * people. cost is what the line adds to groceryCost. packageCount x
     * packageLabel at
     * packagePrice each is what to pick up; by-weight items have no count and
     * packagePrice
     * is per lb. estimate: no package to price it by, so cost is the usage cost (or
     * null).
     */
    public record Item(UUID id, UUID ingredientId, String name, BigDecimal amount, String unit,
            String displayAmount, Integer packageCount, String packageLabel, BigDecimal packagePrice,
            BigDecimal cost, boolean estimate, boolean purchased, boolean needToBuy) {

        static Item from(GroceryListItem item) {
            return new Item(item.getId(), item.getIngredient().getId(), item.getIngredient().getName(),
                    item.getTotalAmount(), item.getUnit(), display(item.getTotalAmount(), item.getUnit()),
                    item.getPackageCount(), item.getPackageLabel(), item.getPackagePrice(), item.getEstimatedCost(),
                    item.getPackagePrice() == null, item.isPurchased(), item.isNeedToBuy());
        }
    }

    public static GroceryListResponse from(MealPlan plan, int unlistedLines) {
        Comparator<GroceryListItem> byName = Comparator.comparing(i -> i.getIngredient().getName());
        List<Item> toBuy = plan.getGroceryItems().stream().filter(i -> !i.isPantry()).sorted(byName)
                .map(Item::from).toList();
        List<Item> pantry = plan.getGroceryItems().stream().filter(GroceryListItem::isPantry).sorted(byName)
                .map(Item::from).toList();
        return new GroceryListResponse(plan.getId(), plan.getWeekStartDate(), plan.getBudgetTarget(),
                plan.getUsageCost(), plan.getGroceryCost(), plan.isOverBudget(), toBuy, pantry, unlistedLines);
    }

    private static final BigDecimal GRAMS_PER_OUNCE = new BigDecimal("28.35");
    private static final BigDecimal GRAMS_PER_POUND = new BigDecimal("453.59");

    private static final BigDecimal HALF_OUNCE = new BigDecimal("14.175");

    /**
     * Weights in lb from a pound up, oz down to half an ounce, and grams below that
     * (a spice's
     * 0.6 g would read "0 oz"); other units as a short number.
     */
    static String display(BigDecimal amount, String unit) {
        if ("g".equals(unit)) {
            if (amount.compareTo(HALF_OUNCE) < 0) {
                return plain(amount.setScale(1, RoundingMode.HALF_UP)) + " g";
            }
            if (amount.compareTo(GRAMS_PER_POUND) >= 0) {
                return plain(amount.divide(GRAMS_PER_POUND, 2, RoundingMode.HALF_UP)) + " lb";
            }
            return plain(amount.divide(GRAMS_PER_OUNCE, 1, RoundingMode.HALF_UP)) + " oz";
        }
        String number = plain(amount.setScale(2, RoundingMode.HALF_UP));
        return "count".equals(unit) ? number : number + " " + unit.replace('_', ' ');
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}