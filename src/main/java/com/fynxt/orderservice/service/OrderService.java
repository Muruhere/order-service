package com.fynxt.orderservice.service;

import com.fynxt.orderservice.api.dto.OrderResponse;
import com.fynxt.orderservice.api.dto.PlaceOrderRequest;
import com.fynxt.orderservice.api.error.TradingException;
import com.fynxt.orderservice.domain.Order;
import com.fynxt.orderservice.domain.OrderSide;
import com.fynxt.orderservice.domain.OrderStatus;
import com.fynxt.orderservice.domain.Position;
import com.fynxt.orderservice.repository.OrderRepository;
import com.fynxt.orderservice.repository.PositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

	private static final int MAX_PENDING_ORDERS = 3;

	private final TraderAccountService traderAccountService;
	private final OrderRepository orderRepository;
	private final PositionRepository positionRepository;

	public OrderService(
			TraderAccountService traderAccountService,
			OrderRepository orderRepository,
			PositionRepository positionRepository) {
		this.traderAccountService = traderAccountService;
		this.orderRepository = orderRepository;
		this.positionRepository = positionRepository;
	}

	@Transactional
	public OrderResponse placeOrder(PlaceOrderRequest request) {
		traderAccountService.lockTraderAccount(request.getTraderId());

		long pending = orderRepository.countByTraderIdAndStatus(request.getTraderId(), OrderStatus.PENDING);
		if (pending >= MAX_PENDING_ORDERS) {
			throw new TradingException(HttpStatus.BAD_REQUEST, "Max 3 PENDING orders per trader");
		}

		OrderSide side = OrderSide.valueOf(request.getSide());
		if (side == OrderSide.SELL) {
			int available = positionRepository
					.findByTraderIdAndStock(request.getTraderId(), request.getStock())
					.map(Position::getQuantity)
					.orElse(0);
			if (available < request.getQuantity()) {
				throw new TradingException(HttpStatus.BAD_REQUEST, "Insufficient shares for SELL");
			}
		}

		Order order = Order.newPending(
				request.getTraderId(),
				request.getStock(),
				request.getSector(),
				request.getQuantity(),
				side);
		orderRepository.save(order);
		return toResponse(order);
	}

	@Transactional
	public OrderResponse fillOrder(long orderId) {
		Order order = orderRepository.findByIdForUpdate(orderId)
				.orElseThrow(() -> new TradingException(HttpStatus.NOT_FOUND, "Order not found"));
		if (order.getStatus() != OrderStatus.PENDING) {
			throw new TradingException(HttpStatus.BAD_REQUEST, "Only PENDING orders can be filled");
		}

		if (order.getSide() == OrderSide.BUY) {
			mergePosition(order.getTraderId(), order.getStock(), order.getSector(), order.getQuantity());
		} else {
			Position position = positionRepository
					.findByTraderIdAndStock(order.getTraderId(), order.getStock())
					.orElseThrow(() -> new TradingException(HttpStatus.BAD_REQUEST, "Insufficient shares"));
			if (position.getQuantity() < order.getQuantity()) {
				throw new TradingException(HttpStatus.BAD_REQUEST, "Insufficient shares");
			}
			position.setQuantity(position.getQuantity() - order.getQuantity());
			positionRepository.save(position);
		}

		order.setStatus(OrderStatus.FILLED);
		return toResponse(order);
	}

	@Transactional
	public OrderResponse cancelOrder(long orderId) {
		Order order = orderRepository.findByIdForUpdate(orderId)
				.orElseThrow(() -> new TradingException(HttpStatus.NOT_FOUND, "Order not found"));
		if (order.getStatus() != OrderStatus.PENDING) {
			throw new TradingException(HttpStatus.BAD_REQUEST, "Only PENDING orders can be cancelled");
		}
		order.setStatus(OrderStatus.CANCELLED);
		return toResponse(order);
	}

	private void mergePosition(String traderId, String stock, String sector, int deltaQty) {
		positionRepository.findByTraderIdAndStock(traderId, stock).ifPresentOrElse(p -> {
			p.setQuantity(p.getQuantity() + deltaQty);
			p.setSector(sector);
			positionRepository.save(p);
		}, () -> positionRepository.save(new Position(traderId, stock, sector, deltaQty)));
	}

	private static OrderResponse toResponse(Order order) {
		return new OrderResponse(
				order.getId(),
				order.getTraderId(),
				order.getStock(),
				order.getSector(),
				order.getQuantity(),
				order.getSide().name(),
				order.getStatus().name(),
				order.getCreatedAt());
	}
}
