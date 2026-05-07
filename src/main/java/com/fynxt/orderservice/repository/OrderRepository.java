package com.fynxt.orderservice.repository;

import com.fynxt.orderservice.domain.Order;
import com.fynxt.orderservice.domain.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

	long countByTraderIdAndStatus(String traderId, OrderStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from TradingOrder o where o.id = :id")
	Optional<Order> findByIdForUpdate(@Param("id") Long id);
}
