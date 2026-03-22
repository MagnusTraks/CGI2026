package com.project.cgi.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MealDbMealJson(
	@JsonProperty("idMeal") String idMeal,
	@JsonProperty("strMeal") String strMeal,
	@JsonProperty("strMealThumb") String strMealThumb
) {
}
