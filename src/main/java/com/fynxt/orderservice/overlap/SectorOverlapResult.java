package com.fynxt.orderservice.overlap;

import java.math.BigDecimal;
import java.util.Map;

public record SectorOverlapResult(
		Map<String, BigDecimal> overlapsByBasket,
		String dominantBasket,
		RiskFlag riskFlag
) {
}
