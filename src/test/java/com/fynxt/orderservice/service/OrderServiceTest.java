package com.fynxt.orderservice.service;

import com.fynxt.orderservice.api.dto.PlaceOrderRequest;
import com.fynxt.orderservice.api.error.TradingException;
import com.fynxt.orderservice.domain.OrderStatus;
import com.fynxt.orderservice.repository.OrderRepository;
import com.fynxt.orderservice.repository.PositionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderServiceTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private PositionRepository positionRepository;

	@Test
	void placeOrder_rejectsFourthPending() {
		String trader = "t-pending-cap";
		for (int i = 0; i < 3; i++) {
			orderService.placeOrder(buy(trader, "AAPL", "TECH", 1));
		}
		assertThatThrownBy(() -> orderService.placeOrder(buy(trader, "MSFT", "TECH", 1)))
				.isInstanceOf(TradingException.class)
				.satisfies(ex -> {
					TradingException te = (TradingException) ex;
					assertThat(te.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
					assertThat(te.getMessage()).contains("PENDING");
				});
	}

	@Test
	void placeSell_rejectsWhenInsufficientShares() {
		String trader = "t-sell";
		orderService.placeOrder(buy(trader, "AAPL", "TECH", 5));
		orderRepository.findAll().stream()
				.filter(o -> o.getTraderId().equals(trader))
				.findFirst()
				.ifPresent(o -> orderService.fillOrder(o.getId()));

		assertThatThrownBy(() -> orderService.placeOrder(sell(trader, "AAPL", "TECH", 10)))
				.isInstanceOf(TradingException.class)
				.satisfies(ex -> assertThat(((TradingException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@Test
	void fillBuy_increasesPosition() {
		String trader = "t-fill-buy";
		long id = orderService.placeOrder(buy(trader, "NVDA", "TECH", 7)).id();
		orderService.fillOrder(id);
		assertThat(positionRepository.findByTraderIdAndStock(trader, "NVDA"))
				.isPresent()
				.get()
				.satisfies(p -> assertThat(p.getQuantity()).isEqualTo(7));
	}

	@Test
	void cancel_onlyWhenPending() {
		String trader = "t-cancel";
		long id = orderService.placeOrder(buy(trader, "CSCO", "TECH", 1)).id();
		orderService.fillOrder(id);
		assertThatThrownBy(() -> orderService.cancelOrder(id))
				.isInstanceOf(TradingException.class)
				.satisfies(ex -> assertThat(((TradingException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@Test
	void cancel_setsCancelled() {
		String trader = "t-cancel-ok";
		long id = orderService.placeOrder(buy(trader, "IBM", "TECH", 2)).id();
		orderService.cancelOrder(id);
		assertThat(orderRepository.findById(id)).isPresent().get().extracting(o -> o.getStatus())
				.isEqualTo(OrderStatus.CANCELLED);
	}

	private static PlaceOrderRequest buy(String traderId, String stock, String sector, int qty) {
		PlaceOrderRequest r = new PlaceOrderRequest();
		r.setTraderId(traderId);
		r.setStock(stock);
		r.setSector(sector);
		r.setQuantity(qty);
		r.setSide("BUY");
		return r;
	}

	private static PlaceOrderRequest sell(String traderId, String stock, String sector, int qty) {
		PlaceOrderRequest r = buy(traderId, stock, sector, qty);
		r.setSide("SELL");
		return r;
	}
}
