package com.project.cgi.service;

import com.project.cgi.dto.MealSuggestionDto;
import com.project.cgi.integration.MealDbListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * TheMealDB avalik REST API (väline teenus).
 * Dokumentatsioon: https://www.themealdb.com/api.php
 */
@Service
public class MealDbService {

	private static final Logger log = LoggerFactory.getLogger(MealDbService.class);

	private final RestClient client;

	public MealDbService(@Value("${app.mealdb.base-url:https://www.themealdb.com}") String baseUrl) {
		this.client = RestClient.builder().baseUrl(baseUrl).build();
	}

	public Optional<MealSuggestionDto> fetchRandomMeal() {
		try {
			MealDbListResponse body = client.get().uri("/api/json/v1/1/random.php").retrieve().body(MealDbListResponse.class);
			if (body == null || body.meals() == null || body.meals().isEmpty()) {
				return Optional.empty();
			}
			var m = body.meals().getFirst();
			String page = "https://www.themealdb.com/meal.php?i=" + m.idMeal();
			return Optional.of(new MealSuggestionDto(m.idMeal(), m.strMeal(), m.strMealThumb(), page));
		} catch (RestClientException e) {
			log.warn("TheMealDB päring ebaõnnestus: {}", e.getMessage());
			return Optional.empty();
		}
	}
}
