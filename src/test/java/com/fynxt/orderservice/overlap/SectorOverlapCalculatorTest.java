package com.fynxt.orderservice.overlap;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SectorOverlapCalculatorTest {

	private final SectorOverlapCalculator calculator = new SectorOverlapCalculator();

	@Test
	void portfolioAaplMsftGoogl_matchesDocStylePercentages() {
		SectorOverlapResult result = calculator.compute(Set.of("AAPL", "MSFT", "GOOGL"));
		Map<String, BigDecimal> o = result.overlapsByBasket();
		assertThat(o.get("TECH_HEAVY")).isEqualByComparingTo("75.00");
		assertThat(o.get("FINANCE_HEAVY")).isEqualByComparingTo("0.00");
		assertThat(o.get("BALANCED")).isEqualByComparingTo("25.00");
		assertThat(result.dominantBasket()).isEqualTo("TECH_HEAVY");
		assertThat(result.riskFlag()).isEqualTo(RiskFlag.HIGH);
	}

	@Test
	void dominantBasket_tieBreaksLexicographically() {
		SectorOverlapResult result = calculator.compute(Set.of());
		assertThat(result.overlapsByBasket().values()).allMatch(v -> v.compareTo(BigDecimal.ZERO) == 0);
		assertThat(result.dominantBasket()).isEqualTo("BALANCED");
	}

	@Test
	void riskFlag_mediumWhenMaxBetween40And60() {
		SectorOverlapResult result = calculator.compute(Set.of("AAPL", "MSFT", "JPM", "GS"));
		assertThat(result.riskFlag()).isEqualTo(RiskFlag.MEDIUM);
	}

	@Test
	void overlapPercent_formula() {
		assertThat(SectorOverlapCalculator.overlapPercent(3, 3, 5)).isEqualByComparingTo("75.00");
		assertThat(SectorOverlapCalculator.overlapPercent(0, 3, 5)).isEqualByComparingTo("0.00");
	}
}
