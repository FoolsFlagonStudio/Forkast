package com.forkast.backend.pricing.kroger;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.forkast.backend.ingredient.SoldBy;

import tools.jackson.databind.json.JsonMapper;

/**
 * Reading Kroger's JSON and choosing the price. The JSON is cut down from the
 * step 1 test run
 * (Kroger On the Rhine, October 2026), keeping the fields we read plus a few we
 * ignore.
 */
class KrogerQuoteTest {

  private final JsonMapper json = JsonMapper.builder().build();

  private static final String LOOSE_ONION = """
      {
        "data": {
          "productId": "0000000004093",
          "upc": "0000000004093",
          "aisleLocations": [{"description": "PRODUCE", "number": "0"}],
          "categories": ["Produce"],
          "description": "Onions - Yellow",
          "items": [{
            "itemId": "0000000004093",
            "favorite": false,
            "price": {"regular": 1.29, "regularPerUnitEstimate": 1.29},
            "size": "1 lb",
            "soldBy": "WEIGHT"
          }],
          "itemInformation": {"depth": "3.0", "averageWeightPerUnit": "0.5 [lb_av]"}
        },
        "meta": {}
      }
      """;

  private static final String CHICKEN = """
      {
        "data": {
          "productId": "0024058150000",
          "brand": "Simple Truth",
          "categories": ["Meat & Seafood", "Natural & Organic"],
          "description": "Simple Truth\\u00c2\\u00ae Natural Boneless & Skinless Fresh Chicken Breast Tenders",
          "items": [{
            "itemId": "0024058150000",
            "inventory": {"stockLevel": "HIGH"},
            "price": {"regular": 6.99, "regularPerUnitEstimate": 6.99},
            "size": "1 lb",
            "soldBy": "WEIGHT"
          }],
          "itemInformation": {"averageWeightPerUnit": "0.94 [lb_av]"}
        }
      }
      """;

  private static final String ONION_BAG = """
      {
        "data": {
          "productId": "0001111091682",
          "categories": ["Produce"],
          "description": "Kroger\\u00c2\\u00ae Yellow Onion 3 lb Bag",
          "items": [{"price": {"regular": 3.79, "promo": 2.99}, "size": "3 lb", "soldBy": "UNIT"}]
        }
      }
      """;

  private KrogerProduct read(String body) {
    return json.readValue(body, KrogerProduct.Response.class).data();
  }

  @Test
  void readsKrogersJsonAndIgnoresTheRest() {
    KrogerProduct onion = read(LOOSE_ONION);

    assertThat(onion.productId()).isEqualTo("0000000004093");
    assertThat(onion.categories()).containsExactly("Produce");
    assertThat(onion.items()).hasSize(1);
    assertThat(onion.items().get(0).price().regular()).isEqualByComparingTo("1.29");
    assertThat(onion.itemInformation().averageWeightPerUnit()).isEqualTo("0.5 [lb_av]");
  }

  @Test
  void looseProduceGetsItsAverageWeight() {
    KrogerQuote quote = KrogerQuote.from(read(LOOSE_ONION)).orElseThrow();

    assertThat(quote.soldBy()).isEqualTo(SoldBy.WEIGHT);
    assertThat(quote.regular()).isEqualByComparingTo("1.29");
    assertThat(quote.averageGramsPerItem()).isEqualByComparingTo("226.80"); // 0.5 lb
    assertThat(quote.promo()).isNull();
  }

  @Test
  void meatByWeightDoesNotGetAnAverageWeight() {
    KrogerQuote quote = KrogerQuote.from(read(CHICKEN)).orElseThrow();

    assertThat(quote.averageGramsPerItem()).isNull(); // a tray, not one breast
    assertThat(quote.label()).isEqualTo("Simple Truth Natural Boneless & Skinless Fresh Chicken Breast Tenders");
  }

  @Test
  void packagedItemKeepsItsSizeAndPromo() {
    KrogerQuote quote = KrogerQuote.from(read(ONION_BAG)).orElseThrow();

    assertThat(quote.soldBy()).isEqualTo(SoldBy.UNIT);
    assertThat(quote.size()).isEqualTo("3 lb");
    assertThat(quote.promo()).isEqualByComparingTo("2.99");
    assertThat(quote.averageGramsPerItem()).isNull();
    assertThat(quote.label()).isEqualTo("Kroger Yellow Onion 3 lb Bag");
  }

  @Test
  void noPriceAtTheStoreIsEmpty() {
    KrogerProduct unpriced = new KrogerProduct("1", "Something", List.of("Pantry"),
        List.of(new KrogerProduct.Item("1", null, "16 oz", "UNIT"),
            new KrogerProduct.Item("1", new KrogerProduct.Price(BigDecimal.ZERO, null), "16 oz", "UNIT")),
        null);

    assertThat(KrogerQuote.from(unpriced)).isEmpty();
    assertThat(KrogerQuote.from(new KrogerProduct("2", "Nothing", null, null, null))).isEmpty();
  }

  @Test
  void countedPackageGetsItsWeight() {
    KrogerProduct celery = new KrogerProduct("4070", "Celery", List.of("Produce"),
        List.of(new KrogerProduct.Item("4070", new KrogerProduct.Price(new BigDecimal("2.59"), null), "1 ct",
            "UNIT")),
        new KrogerProduct.ItemInformation("1.5 [lb_av]", null));

    KrogerQuote quote = KrogerQuote.from(celery).orElseThrow();

    assertThat(quote.packageGrams()).isEqualByComparingTo("680.39");
    assertThat(quote.averageGramsPerItem()).isNull(); // not sold by weight
  }

  @Test
  void readsPoundsAndOunces() {
    assertThat(KrogerQuote.grams("0.5 [lb_av]")).isEqualByComparingTo("226.80");
    assertThat(KrogerQuote.grams("12 [oz_av]")).isEqualByComparingTo("340.20");
    assertThat(KrogerQuote.grams("1.4")).isEqualByComparingTo("635.03");
    assertThat(KrogerQuote.grams(null)).isNull();
    assertThat(KrogerQuote.grams("n/a")).isNull();
  }

  @Test
  void zeroPromoMeansNoSale() {
    KrogerProduct product = new KrogerProduct("1", "Rice", List.of("Pantry"),
        List.of(new KrogerProduct.Item("1", new KrogerProduct.Price(new BigDecimal("2.49"), BigDecimal.ZERO),
            "2 lb", "UNIT")),
        null);

    assertThat(KrogerQuote.from(product).orElseThrow().promo()).isNull();
  }
}