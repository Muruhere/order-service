package com.fynxt.orderservice.api.error;

import org.springframework.http.HttpStatus;

public class TradingException extends RuntimeException {

	private final HttpStatus status;

	public TradingException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
