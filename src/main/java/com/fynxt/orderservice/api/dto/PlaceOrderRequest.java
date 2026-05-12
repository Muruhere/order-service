package com.fynxt.orderservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public class PlaceOrderRequest {

	@NotBlank
	private String traderId;

	@NotNull(message = "Stock cannot be blank")
	private Stock stock;

	@NotBlank
	private String sector;

	@Positive
	private int quantity;

	@NotBlank
	@Pattern(regexp = "BUY|SELL")
	private String side;

	public String getTraderId() {
		return traderId;
	}

	public void setTraderId(String traderId) {
		this.traderId = traderId;
	}

	public String getStock() {
		return stock.name();
	}

	public void setStock(Stock stock) {
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

	public String getSide() {
		return side;
	}

	public void setSide(String side) {
		this.side = side;
	}
}
