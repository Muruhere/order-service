package com.fynxt.orderservice.service;

import com.fynxt.orderservice.api.dto.AddPositionRequest;
import com.fynxt.orderservice.api.dto.PortfolioResponse;
import com.fynxt.orderservice.api.dto.SectorOverlapResponse;
import com.fynxt.orderservice.domain.Position;
import com.fynxt.orderservice.overlap.SectorOverlapCalculator;
import com.fynxt.orderservice.overlap.SectorOverlapResult;
import com.fynxt.orderservice.repository.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

	private final TraderAccountService traderAccountService;
	private final PositionRepository positionRepository;
	private final SectorOverlapCalculator sectorOverlapCalculator;

	public PortfolioService(
			TraderAccountService traderAccountService,
			PositionRepository positionRepository,
			SectorOverlapCalculator sectorOverlapCalculator) {
		this.traderAccountService = traderAccountService;
		this.positionRepository = positionRepository;
		this.sectorOverlapCalculator = sectorOverlapCalculator;
	}

	@Transactional(readOnly = true)
	public PortfolioResponse getPortfolio(String traderId) {
		Map<String, Integer> positions = new HashMap<>();
		Map<String, Integer> sectorTotals = new HashMap<>();
		for (Position p : positionRepository.findByTraderId(traderId)) {
			if (p.getQuantity() <= 0) {
				continue;
			}
			positions.merge(p.getStock(), p.getQuantity(), Integer::sum);
			sectorTotals.merge(p.getSector(), p.getQuantity(), Integer::sum);
		}
		return new PortfolioResponse(traderId, positions, sectorTotals);
	}

	@Transactional(readOnly = true)
	public SectorOverlapResponse sectorOverlap(String traderId) {
		Set<String> tickers = positionRepository.findByTraderId(traderId).stream()
				.filter(p -> p.getQuantity() > 0)
				.map(Position::getStock)
				.collect(Collectors.toSet());
		SectorOverlapResult result = sectorOverlapCalculator.compute(tickers);
		return new SectorOverlapResponse(
				result.overlapsByBasket(),
				result.dominantBasket(),
				result.riskFlag().name());
	}

	@Transactional
	public PortfolioResponse addPosition(String traderId, AddPositionRequest request) {
		traderAccountService.lockTraderAccount(traderId);
		positionRepository.findByTraderIdAndStock(traderId, request.getStock()).ifPresentOrElse(p -> {
			p.setQuantity(p.getQuantity() + request.getQuantity());
			p.setSector(request.getSector());
			positionRepository.save(p);
		}, () -> positionRepository.save(
				new Position(traderId, request.getStock(), request.getSector(), request.getQuantity())));
		return getPortfolio(traderId);
	}
}
