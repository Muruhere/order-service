package com.fynxt.orderservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity(name = "TradingOrder")
@Table(name = "orders")
public class Order {

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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 4)
	private OrderSide side;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private OrderStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Version
	@Column(name = "version")
	private Integer version;

	protected Order() {
	}

	public static Order newPending(
			String traderId,
			String stock,
			String sector,
			int quantity,
			OrderSide side) {
		Order order = new Order();
		order.setTraderId(traderId);
		order.setStock(stock);
		order.setSector(sector);
		order.setQuantity(quantity);
		order.setSide(side);
		order.setStatus(OrderStatus.PENDING);
		return order;
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

	public OrderSide getSide() {
		return side;
	}

	public void setSide(OrderSide side) {
		this.side = side;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Integer getVersion() {
		return version;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
