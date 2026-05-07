package com.fynxt.orderservice.overlap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pure Java: overlap = [2 × |common| / (|portfolio| + |basket|)] × 100 (percent, two decimals).
 * Dominant basket = highest overlap; ties broken by lexicographically smallest basket name.
 * Risk: HIGH if any overlap ≥ 60%, else MEDIUM if any ≥ 40%, else LOW.
 */
public final class SectorOverlapCalculator {

	private static final Map<String, Set<String>> BASKETS = Map.of(
			"TECH_HEAVY", Set.of("AAPL", "MSFT", "GOOGL", "TSLA", "NVDA"),
			"FINANCE_HEAVY", Set.of("JPM", "GS", "BAC", "MS", "WFC"),
			"BALANCED", Set.of("AAPL", "JPM", "XOM", "JNJ", "TSLA")
	);

	public SectorOverlapResult compute(Set<String> portfolioTickers) {
		Objects.requireNonNull(portfolioTickers, "portfolioTickers");
		Set<String> portfolio = Set.copyOf(portfolioTickers);
		int pSize = portfolio.size();

		Map<String, BigDecimal> overlaps = new LinkedHashMap<>();
		for (Map.Entry<String, Set<String>> e : BASKETS.entrySet()) {
			long common = e.getValue().stream().filter(portfolio::contains).count();
			overlaps.put(e.getKey(), overlapPercent(common, pSize, e.getValue().size()));
		}

		BigDecimal max = overlaps.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
		String dominant = overlaps.entrySet().stream()
				.filter(en -> en.getValue().compareTo(max) == 0)
				.map(Map.Entry::getKey)
				.min(String::compareTo)
				.orElse("BALANCED");

		RiskFlag risk = resolveRisk(overlaps);
		return new SectorOverlapResult(Collections.unmodifiableMap(overlaps), dominant, risk);
	}

	static BigDecimal overlapPercent(long commonTickers, int portfolioSize, int basketSize) {
		long denominator = (long) portfolioSize + basketSize;
		if (denominator == 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		return BigDecimal.valueOf(2L * commonTickers)
				.divide(BigDecimal.valueOf(denominator), 10, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100))
				.setScale(2, RoundingMode.HALF_UP);
	}

	private static RiskFlag resolveRisk(Map<String, BigDecimal> overlaps) {
		BigDecimal sixty = BigDecimal.valueOf(60);
		BigDecimal forty = BigDecimal.valueOf(40);
		for (BigDecimal v : overlaps.values()) {
			if (v.compareTo(sixty) >= 0) {
				return RiskFlag.HIGH;
			}
		}
		for (BigDecimal v : overlaps.values()) {
			if (v.compareTo(forty) >= 0) {
				return RiskFlag.MEDIUM;
			}
		}
		return RiskFlag.LOW;
	}
}
