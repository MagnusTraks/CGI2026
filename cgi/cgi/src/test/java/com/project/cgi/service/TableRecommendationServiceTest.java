package com.project.cgi.service;

import com.project.cgi.domain.Reservation;
import com.project.cgi.domain.RestaurantTable;
import com.project.cgi.domain.Zone;
import com.project.cgi.dto.RecommendationRequest;
import com.project.cgi.dto.RecommendationResponse;
import com.project.cgi.dto.ScoredTableDto;
import com.project.cgi.repository.ReservationRepository;
import com.project.cgi.repository.RestaurantTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableRecommendationServiceTest {

	@Mock
	private RestaurantTableRepository tableRepository;
	@Mock
	private ReservationRepository reservationRepository;

	private TableRecommendationService recommendationService;

	private RestaurantTable small;
	private RestaurantTable large;

	@BeforeEach
	void setUp() {
		recommendationService = new TableRecommendationService(tableRepository, reservationRepository, 2);
		small = new RestaurantTable();
		small.setId(1L);
		small.setCode("S");
		small.setCapacity(2);
		small.setZone(Zone.INDOOR);
		small.setQuietCorner(true);
		small.setWindowSeat(false);
		small.setNearKids(false);
		small.setAccessible(false);

		large = new RestaurantTable();
		large.setId(2L);
		large.setCode("L");
		large.setCapacity(8);
		large.setZone(Zone.INDOOR);
		large.setQuietCorner(true);
		large.setWindowSeat(false);
		large.setNearKids(false);
		large.setAccessible(false);
	}

	@Test
	void prefersExactCapacityForPartyOfTwo() {
		when(tableRepository.findAll()).thenReturn(List.of(small, large));
		when(reservationRepository.findOverlappingForTable(eq(1L), any(), any())).thenReturn(List.of());
		when(reservationRepository.findOverlappingForTable(eq(2L), any(), any())).thenReturn(List.of());

		LocalDateTime start = LocalDateTime.of(2026, 6, 1, 18, 0);
		RecommendationRequest req = new RecommendationRequest(start, 2, null, true, false, false, false);
		RecommendationResponse res = recommendationService.recommend(req);

		assertThat(res.bestTableId()).isEqualTo(1L);
		assertThat(res.bestMergedTableId()).isNull();
		assertThat(res.rankedTables().getFirst().tableId()).isEqualTo(1L);
	}

	@Test
	void excludesBusyTables() {
		when(tableRepository.findAll()).thenReturn(List.of(small, large));
		when(reservationRepository.findOverlappingForTable(eq(1L), any(), any()))
				.thenReturn(List.of(new Reservation()));
		when(reservationRepository.findOverlappingForTable(eq(2L), any(), any())).thenReturn(List.of());

		LocalDateTime start = LocalDateTime.of(2026, 6, 1, 18, 0);
		RecommendationRequest req = new RecommendationRequest(start, 2, null, false, false, false, false);
		RecommendationResponse res = recommendationService.recommend(req);

		assertThat(res.bestTableId()).isEqualTo(2L);
		assertThat(res.bestMergedTableId()).isNull();
	}

	@Test
	void filtersByZone() {
		when(tableRepository.findAll()).thenReturn(List.of(small, large));
		small.setZone(Zone.TERRACE);
		large.setZone(Zone.INDOOR);
		// Ainult INDOOR laua ülekatted kontrollitakse; terrassi lauda ei vaadata
		when(reservationRepository.findOverlappingForTable(eq(2L), any(), any())).thenReturn(List.of());

		LocalDateTime start = LocalDateTime.of(2026, 6, 1, 18, 0);
		RecommendationRequest req = new RecommendationRequest(start, 2, Zone.INDOOR, false, false, false, false);
		RecommendationResponse res = recommendationService.recommend(req);

		assertThat(res.bestTableId()).isEqualTo(2L);
		assertThat(res.bestMergedTableId()).isNull();
		assertThat(res.rankedTables()).hasSize(1);
	}

	@Test
	void suggestsMergedPairWhenSingleTablesTooSmall() {
		RestaurantTable six = new RestaurantTable();
		six.setId(1L);
		six.setCode("M6");
		six.setCapacity(6);
		six.setZone(Zone.INDOOR);
		six.setQuietCorner(false);
		six.setWindowSeat(false);
		six.setNearKids(false);
		six.setAccessible(false);
		six.getAdjacentTableIds().add(2L);

		RestaurantTable eight = new RestaurantTable();
		eight.setId(2L);
		eight.setCode("M8");
		eight.setCapacity(8);
		eight.setZone(Zone.INDOOR);
		eight.setQuietCorner(false);
		eight.setWindowSeat(false);
		eight.setNearKids(false);
		eight.setAccessible(false);
		eight.getAdjacentTableIds().add(1L);

		when(tableRepository.findAll()).thenReturn(List.of(six, eight));
		when(reservationRepository.findOverlappingForTable(eq(1L), any(), any())).thenReturn(List.of());
		when(reservationRepository.findOverlappingForTable(eq(2L), any(), any())).thenReturn(List.of());

		LocalDateTime start = LocalDateTime.of(2026, 6, 1, 18, 0);
		RecommendationRequest req = new RecommendationRequest(start, 9, null, false, false, false, false);
		RecommendationResponse res = recommendationService.recommend(req);

		assertThat(res.rankedTables().stream().filter(ScoredTableDto::merged)).isNotEmpty();
		assertThat(res.bestMergedTableId()).isEqualTo(2L);
		assertThat(res.bestTableId()).isEqualTo(1L);
	}
}
