package com.fynxt.orderservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "positions",
		uniqueConstraints = @UniqueConstraint(name = "uk_positions_trader_stock", columnNames = { "trader_id", "stock" })
)
public class Position {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "trader_id", nullable = false, length = 64)
	private String traderId;

	@Column(nullable = false, length = 32)
	private String stock;

	@Column(nullable = false, length = 64)
	private String sector;

	@Column(nullable = false)
	private int quantity;

	protected Position() {
	}

	public Position(String traderId, String stock, String sector, int quantity) {
		this.traderId = traderId;
		this.stock = stock;
		this.sector = sector;
		this.quantity = quantity;
	}

	public Long getId() {
		return id;
	}

	public String getTraderId() {
		return traderId;
	}

	public void setTraderId(String traderId) {
		this.traderId = traderId;
	}

	public String getStock() {
		return stock;
	}

	public void setStock(String stock) {
		this.stock = stock;
	}

	public String getSector() {
		return sector;
	}

	public void setSector(String sector) {
		this.sector = sector;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}
