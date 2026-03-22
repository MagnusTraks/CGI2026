package com.project.cgi.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MealDbListResponse(List<MealDbMealJson> meals) {
}
