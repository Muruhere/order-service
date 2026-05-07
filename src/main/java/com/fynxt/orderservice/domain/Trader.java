package com.fynxt.orderservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "traders")
public class Trader {

	@Id
	@Column(name = "trader_id", length = 64, nullable = false)
	private String traderId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Trader() {
	}

	public Trader(String traderId, Instant createdAt) {
		this.traderId = traderId;
		this.createdAt = createdAt;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public String getTraderId() {
		return traderId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
