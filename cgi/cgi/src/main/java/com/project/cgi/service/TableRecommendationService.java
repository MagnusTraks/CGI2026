package com.project.cgi.service;

import com.project.cgi.domain.RestaurantTable;
import com.project.cgi.domain.Zone;
import com.project.cgi.dto.RecommendationRequest;
import com.project.cgi.dto.RecommendationResponse;
import com.project.cgi.dto.ScoredTableDto;
import com.project.cgi.repository.ReservationRepository;
import com.project.cgi.repository.RestaurantTableRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Skoorib vabu laudu ja naaberpaare: efektiivsus + eelistused.
 * Paarile lisandub väike miinus, et eelistada ühte lauda, kui mahutavus lubab.
 */
@Service
public class TableRecommendationService {

	private static final double EFFICIENCY_WEIGHT = 18.0;
	private static final double EXACT_CAPACITY_BONUS = 55.0;
	private static final double PREFERENCE_MATCH = 38.0;
	private static final double PREFERENCE_MISS = 22.0;
	/** Eelistab ühte lauda, kui skoor on sarnane */
	private static final double MERGE_PAIR_PENALTY = 12.0;

	private final RestaurantTableRepository tableRepository;
	private final ReservationRepository reservationRepository;
	private final int durationHours;

	public TableRecommendationService(
			RestaurantTableRepository tableRepository,
			ReservationRepository reservationRepository,
			@Value("${app.reservation.duration-hours:2}") int durationHours) {
		this.tableRepository = tableRepository;
		this.reservationRepository = reservationRepository;
		this.durationHours = durationHours;
	}

	@Transactional(readOnly = true)
	public RecommendationResponse recommend(RecommendationRequest req) {
		LocalDateTime end = req.start().plusHours(durationHours);
		Zone zoneFilter = req.zone();
		List<RestaurantTable> all = tableRepository.findAll();
		Map<Long, RestaurantTable> byId = new HashMap<>();
		for (RestaurantTable t : all) {
			byId.put(t.getId(), t);
		}

		List<ScoredTableDto> ranked = new ArrayList<>();

		for (RestaurantTable t : all) {
			if (t.getCapacity() < req.partySize()) {
				continue;
			}
			if (!zoneMatches(zoneFilter, t)) {
				continue;
			}
			if (!reservationRepository.findOverlappingForTable(t.getId(), req.start(), end).isEmpty()) {
				continue;
			}
			double score = scoreSingle(t, req);
			ranked.add(new ScoredTableDto(t.getId(), t.getCode(), score, explainSingle(t, req, score), null, null, false));
		}

		for (RestaurantTable a : all) {
			for (Long otherId : a.getAdjacentTableIds()) {
				if (a.getId() >= otherId) {
					continue;
				}
				RestaurantTable b = byId.get(otherId);
				if (b == null) {
					continue;
				}
				int combined = a.getCapacity() + b.getCapacity();
				if (combined < req.partySize()) {
					continue;
				}
				if (!zoneMatches(zoneFilter, a) || !zoneMatches(zoneFilter, b)) {
					continue;
				}
				if (!reservationRepository.findOverlappingForTable(a.getId(), req.start(), end).isEmpty()) {
					continue;
				}
				if (!reservationRepository.findOverlappingForTable(b.getId(), req.start(), end).isEmpty()) {
					continue;
				}
				double score = scorePair(a, b, req);
				long idLo = Math.min(a.getId(), b.getId());
				long idHi = Math.max(a.getId(), b.getId());
				RestaurantTable lo = a.getId() == idLo ? a : b;
				RestaurantTable hi = a.getId() == idHi ? a : b;
				ranked.add(new ScoredTableDto(idLo, lo.getCode(), score, explainPair(lo, hi, req, score), idHi, hi.getCode(), true));
			}
		}

		ranked.sort(Comparator.comparingDouble(ScoredTableDto::score).reversed());
		Long bestTableId = ranked.isEmpty() ? null : ranked.getFirst().tableId();
		Long bestMergedTableId = ranked.isEmpty() ? null : ranked.getFirst().merged() ? ranked.getFirst().mergedTableId() : null;

		return new RecommendationResponse(bestTableId, bestMergedTableId, ranked);
	}

	private static boolean zoneMatches(Zone filter, RestaurantTable t) {
		return filter == null || t.getZone() == filter;
	}

	private double scoreSingle(RestaurantTable t, RecommendationRequest req) {
		int waste = t.getCapacity() - req.partySize();
		double s = 100.0 - waste * EFFICIENCY_WEIGHT;
		if (waste == 0) {
			s += EXACT_CAPACITY_BONUS;
		}
		s += preferenceDelta(t, req);
		return s;
	}

	private double scorePair(RestaurantTable a, RestaurantTable b, RecommendationRequest req) {
		int combined = a.getCapacity() + b.getCapacity();
		int waste = combined - req.partySize();
		double s = 100.0 - waste * EFFICIENCY_WEIGHT - MERGE_PAIR_PENALTY;
		if (waste == 0) {
			s += EXACT_CAPACITY_BONUS;
		}
		s += (preferenceDelta(a, req) + preferenceDelta(b, req)) / 2.0;
		return s;
	}

	private double preferenceDelta(RestaurantTable t, RecommendationRequest req) {
		double d = 0;
		if (req.wantsPrivacy()) {
			d += t.isQuietCorner() ? PREFERENCE_MATCH : -PREFERENCE_MISS;
		}
		if (req.wantsWindow()) {
			d += t.isWindowSeat() ? PREFERENCE_MATCH : -PREFERENCE_MISS;
		}
		if (req.wantsNearKids()) {
			d += t.isNearKids() ? PREFERENCE_MATCH : -PREFERENCE_MISS;
		}
		if (req.wantsAccessible()) {
			d += t.isAccessible() ? PREFERENCE_MATCH : -PREFERENCE_MISS;
		}
		return d;
	}

	private String explainSingle(RestaurantTable t, RecommendationRequest req, double score) {
		StringBuilder b = new StringBuilder();
		b.append(String.format("Skoor %.1f — üks laud; ", score));
		int w = t.getCapacity() - req.partySize();
		if (w == 0) {
			b.append("mahutavus täpselt; ");
		} else if (w <= 1) {
			b.append("väike vaba koht; ");
		} else {
			b.append("laual on ").append(w).append(" vaba kohta; ");
		}
		appendPrefs(b, t, req);
		b.append("tsoon: ").append(t.getZone().name());
		return b.toString();
	}

	private String explainPair(RestaurantTable lo, RestaurantTable hi, RecommendationRequest req, double score) {
		StringBuilder b = new StringBuilder();
		int combined = lo.getCapacity() + hi.getCapacity();
		b.append(String.format("Skoor %.1f — kaks naaberlauda kokku (%d+%d=%d kohta); ",
				score, lo.getCapacity(), hi.getCapacity(), combined));
		int waste = combined - req.partySize();
		if (waste == 0) {
			b.append("mahutavus täpselt; ");
		} else {
			b.append(waste).append(" vaba kohta kokku; ");
		}
		appendPrefs(b, lo, req);
		appendPrefs(b, hi, req);
		b.append("tsoonid: ").append(lo.getZone().name()).append(" / ").append(hi.getZone().name());
		return b.toString();
	}

	private void appendPrefs(StringBuilder b, RestaurantTable t, RecommendationRequest req) {
		if (req.wantsPrivacy()) {
			b.append(t.isQuietCorner() ? "privaatsus OK; " : "privaatsus nõrk; ");
		}
		if (req.wantsWindow()) {
			b.append(t.isWindowSeat() ? "aken OK; " : "aken puudub; ");
		}
		if (req.wantsNearKids()) {
			b.append(t.isNearKids() ? "laste nurk lähedal; " : "laste mängunurk kaugemal; ");
		}
		if (req.wantsAccessible()) {
			b.append(t.isAccessible() ? "ligipääsetav; " : "vähem ligipääsetav; ");
		}
	}
}
