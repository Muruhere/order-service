package com.fynxt.orderservice.controller;

import com.fynxt.orderservice.api.dto.AddPositionRequest;
import com.fynxt.orderservice.api.dto.PortfolioResponse;
import com.fynxt.orderservice.api.dto.SectorOverlapResponse;
import com.fynxt.orderservice.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portfolios")
public class PortfolioController {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	@GetMapping("/{traderId}")
	public PortfolioResponse getPortfolio(@PathVariable String traderId) {
		return portfolioService.getPortfolio(traderId);
	}

	@GetMapping("/{traderId}/sector-overlap")
	public SectorOverlapResponse sectorOverlap(@PathVariable String traderId) {
		return portfolioService.sectorOverlap(traderId);
	}

	@PostMapping("/{traderId}/positions")
	public PortfolioResponse addPosition(
			@PathVariable String traderId,
			@Valid @RequestBody AddPositionRequest request) {
		return portfolioService.addPosition(traderId, request);
	}
}
