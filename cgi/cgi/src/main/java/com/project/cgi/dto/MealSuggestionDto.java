package com.project.cgi.dto;

/**
 * TheMealDB — juhuslik roa soovitus (väline API).
 * @see <a href="https://www.themealdb.com/api.php">TheMealDB API</a>
 */
public record MealSuggestionDto(
	String idMeal,
	String name,
	String thumbnailUrl,
	String mealPageUrl
) {
}
