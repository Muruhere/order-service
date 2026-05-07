package com.fynxt.orderservice.service;

import com.fynxt.orderservice.domain.Trader;
import com.fynxt.orderservice.repository.TraderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TraderAccountService {

	private final TraderRepository traderRepository;

	public TraderAccountService(TraderRepository traderRepository) {
		this.traderRepository = traderRepository;
	}

	/**
	 * Ensures a trader row exists, then locks it ({@code SELECT ... FOR UPDATE}) so callers can
	 * safely enforce per-trader rules (pending cap, etc.) within the same transaction.
	 */
	@Transactional
	public void lockTraderAccount(String traderId) {
		traderRepository.findById(traderId).orElseGet(() -> {
			try {
				return traderRepository.save(new Trader(traderId, Instant.now()));
			} catch (DataIntegrityViolationException ex) {
				return traderRepository.findById(traderId).orElseThrow(() -> ex);
			}
		});
		traderRepository.findByIdForUpdate(traderId).orElseThrow();
	}
}
