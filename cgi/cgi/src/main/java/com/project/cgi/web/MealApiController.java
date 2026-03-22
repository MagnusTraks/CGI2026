package com.project.cgi.web;

import com.project.cgi.dto.MealSuggestionDto;
import com.project.cgi.service.MealDbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meal")
public class MealApiController {

	private final MealDbService mealDbService;

	public MealApiController(MealDbService mealDbService) {
		this.mealDbService = mealDbService;
	}

	@GetMapping("/random")
	public ResponseEntity<MealSuggestionDto> randomMeal() {
		return mealDbService.fetchRandomMeal().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(503).build());
	}
}
