package com.fynxt.orderservice.repository;

import com.fynxt.orderservice.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

	Optional<Position> findByTraderIdAndStock(String traderId, String stock);

	List<Position> findByTraderId(String traderId);
}
