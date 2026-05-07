package com.fynxt.orderservice.api.dto;

import java.math.BigDecimal;
import java.util.Map;

public record SectorOverlapResponse(
		Map<String, BigDecimal> overlaps,
		String dominantBasket,
		String riskFlag
) {
}
