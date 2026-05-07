package com.fynxt.orderservice.api.dto;

import java.time.Instant;

public record OrderResponse(
		Long id,
		String traderId,
		String stock,
		String sector,
		int quantity,
		String side,
		String status,
		Instant createdAt
) {
}
