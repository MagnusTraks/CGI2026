package com.project.cgi.web;

import com.project.cgi.domain.RestaurantTable;
import com.project.cgi.domain.Zone;
import com.project.cgi.dto.CreateReservationRequest;
import com.project.cgi.dto.RecommendationRequest;
import com.project.cgi.dto.RecommendationResponse;
import com.project.cgi.dto.ReservationViewDto;
import com.project.cgi.dto.TableLayoutDto;
import com.project.cgi.repository.RestaurantTableRepository;
import com.project.cgi.service.ReservationService;
import com.project.cgi.service.TableRecommendationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RestaurantApiController {

	private final RestaurantTableRepository tableRepository;
	private final TableRecommendationService recommendationService;
	private final ReservationService reservationService;

	public RestaurantApiController(
			RestaurantTableRepository tableRepository,
			TableRecommendationService recommendationService,
			ReservationService reservationService) {
		this.tableRepository = tableRepository;
		this.recommendationService = recommendationService;
		this.reservationService = reservationService;
	}

	@GetMapping("/tables")
	public List<TableLayoutDto> tables() {
		return tableRepository.findAll().stream().map(this::toLayout).toList();
	}

	@GetMapping("/zones")
	public List<String> zones() {
		return Arrays.stream(Zone.values()).map(Enum::name).toList();
	}

	@GetMapping("/reservations")
	public List<ReservationViewDto> reservationsForRange(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
		return reservationService.reservationsForSlot(start, end);
	}

	@PostMapping("/recommendations")
	public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequest body) {
		return recommendationService.recommend(body);
	}

	@PostMapping("/reservations")
	@ResponseStatus(HttpStatus.CREATED)
	public ReservationViewDto book(@Valid @RequestBody CreateReservationRequest body) {
		return reservationService.create(body);
	}

	private TableLayoutDto toLayout(RestaurantTable t) {
		return new TableLayoutDto(
				t.getId(),
				t.getCode(),
				t.getCapacity(),
				t.getZone(),
				t.getGridX(),
				t.getGridY(),
				t.getGridW(),
				t.getGridH(),
				t.isQuietCorner(),
				t.isWindowSeat(),
				t.isNearKids(),
				t.isAccessible(),
				List.copyOf(t.getAdjacentTableIds()));
	}
}
