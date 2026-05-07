package com.fynxt.orderservice.repository;

import com.fynxt.orderservice.domain.Trader;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TraderRepository extends JpaRepository<Trader, String> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Trader t where t.traderId = :traderId")
	Optional<Trader> findByIdForUpdate(@Param("traderId") String traderId);
}
