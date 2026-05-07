package com.fynxt.orderservice.config;

import com.fynxt.orderservice.overlap.SectorOverlapCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OverlapConfiguration {

	@Bean
	public SectorOverlapCalculator sectorOverlapCalculator() {
		return new SectorOverlapCalculator();
	}
}
