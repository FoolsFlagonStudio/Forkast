package com.forkast.backend.ingredient;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs against the real database with the seeded ingredient catalog (step 4),
 * because
 * trigram similarity only exists in Postgres. Read-only, and @Transactional
 * rolls back
 * anyway, so it leaves no data behind.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class IngredientMatcherIntegrationTest {

    @Autowired
    private IngredientMatcher matcher;

    @Test
    void exactName() {
        MatchResult result = matcher.match("garlic");

        assertThat(result.ingredient().getName()).isEqualTo("garlic");
        assertThat(result.score()).isEqualByComparingTo("1");
    }

    @Test
    void exactAlias() {
        assertThat(matcher.match("garlic cloves").ingredient().getName()).isEqualTo("garlic");
    }

    @Test
    void pluralFallsBackToSingularName() {
        assertThat(matcher.match("cauliflowers").ingredient().getName()).isEqualTo("cauliflower");
    }

    @Test
    void descriptorsAreStripped() {
        MatchResult result = matcher.match("finely chopped fresh cilantro");

        assertThat(result.ingredient().getName()).isEqualTo("cilantro");
        assertThat(result.score()).isEqualByComparingTo("1");
    }

    @Test
    void typoMatchesBySimilarity() {
        MatchResult result = matcher.match("parmesean cheese");

        assertThat(result.matched()).isTrue();
        assertThat(result.ingredient().getName()).isEqualTo("parmesan cheese");
        assertThat(result.score()).isBetween(IngredientMatcher.THRESHOLD, java.math.BigDecimal.ONE);
    }

    @Test
    void unknownIngredientIsNotMatchedButKeepsItsScore() {
        MatchResult result = matcher.match("xanthan gum");

        assertThat(result.matched()).isFalse();
        assertThat(result.score()).isLessThan(IngredientMatcher.THRESHOLD);
    }
}