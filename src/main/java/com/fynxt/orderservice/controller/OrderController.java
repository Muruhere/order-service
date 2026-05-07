package com.fynxt.orderservice.controller;

import com.fynxt.orderservice.api.dto.OrderResponse;
import com.fynxt.orderservice.api.dto.PlaceOrderRequest;
import com.fynxt.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	public OrderResponse placeOrder(@Valid @RequestBody PlaceOrderRequest orderRequest) {
		return orderService.placeOrder(orderRequest);
	}

	@PostMapping("/{id}/fill")
	public OrderResponse fillOrder(@PathVariable("id") long id) {
		return orderService.fillOrder(id);
	}

	@PostMapping("/{id}/cancel")
	public OrderResponse cancelOrder(@PathVariable("id") long id) {
		return orderService.cancelOrder(id);
	}
}
