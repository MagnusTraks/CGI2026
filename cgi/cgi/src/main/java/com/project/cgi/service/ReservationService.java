package com.project.cgi.service;

import com.project.cgi.domain.Reservation;
import com.project.cgi.domain.RestaurantTable;
import com.project.cgi.dto.CreateReservationRequest;
import com.project.cgi.dto.ReservationViewDto;
import com.project.cgi.repository.ReservationRepository;
import com.project.cgi.repository.RestaurantTableRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final RestaurantTableRepository tableRepository;
	private final int durationHours;

	public ReservationService(ReservationRepository reservationRepository, RestaurantTableRepository tableRepository, @Value("${app.reservation.duration-hours:2}") int durationHours) {
		this.reservationRepository = reservationRepository;
		this.tableRepository = tableRepository;
		this.durationHours = durationHours;
	}

	@Transactional(readOnly = true)
	public List<ReservationViewDto> reservationsForSlot(LocalDateTime start, LocalDateTime end) {
		return reservationRepository.findInTimeRange(start, end).stream().map(this::toDto).toList();
	}

	@Transactional
	public ReservationViewDto create(CreateReservationRequest req) {
		if (req.mergedTableId() == null) {
			return createSingle(req);
		}
		return createMerged(req);
	}

	private ReservationViewDto createSingle(CreateReservationRequest req) {
		RestaurantTable table = tableRepository.findById(req.tableId()).orElseThrow(() -> new IllegalArgumentException("Laua ID ei leitud"));
		if (table.getCapacity() < req.partySize()) {
			throw new IllegalArgumentException("Seltskond ei mahu valitud laua juurde");
		}
		LocalDateTime end = req.start().plusHours(durationHours);
		if (!reservationRepository.findOverlappingForTable(table.getId(), req.start(), end).isEmpty()) {
			throw new IllegalStateException("See ajavahemik on juba hõivatud");
		}
		Reservation r = new Reservation();
		r.setTable(table);
		r.setMergedTable(null);
		r.setStartTime(req.start());
		r.setEndTime(end);
		r.setGuestCount(req.partySize());
		r.setGuestName(req.guestName().trim());
		reservationRepository.save(r);
		return toDto(r);
	}

	private ReservationViewDto createMerged(CreateReservationRequest req) {
		if (req.tableId().equals(req.mergedTableId())) {
			throw new IllegalArgumentException("Kaks erinevat lauda on vaja kokkuliitmiseks");
		}
		RestaurantTable a = tableRepository.findById(req.tableId()).orElseThrow(() -> new IllegalArgumentException("Esimene laud ei leitud"));
		RestaurantTable b = tableRepository.findById(req.mergedTableId()).orElseThrow(() -> new IllegalArgumentException("Teine laud ei leitud"));
		if (!areAdjacent(a, b)) {
			throw new IllegalArgumentException("Lauad pole naabrid — neid ei saa kokku lükata");
		}
		int combined = a.getCapacity() + b.getCapacity();
		if (combined < req.partySize()) {
			throw new IllegalArgumentException("Seltskond ei mahu kahe laua peale kokku");
		}
		LocalDateTime end = req.start().plusHours(durationHours);
		if (!reservationRepository.findOverlappingForTable(a.getId(), req.start(), end).isEmpty()
				|| !reservationRepository.findOverlappingForTable(b.getId(), req.start(), end).isEmpty()) {
			throw new IllegalStateException("Üks või mõlemad lauad on sel ajal juba hõivatud");
		}
		RestaurantTable primary = a.getId() < b.getId() ? a : b;
		RestaurantTable secondary = a.getId() < b.getId() ? b : a;

		Reservation r = new Reservation();
		r.setTable(primary);
		r.setMergedTable(secondary);
		r.setStartTime(req.start());
		r.setEndTime(end);
		r.setGuestCount(req.partySize());
		r.setGuestName(req.guestName().trim());
		reservationRepository.save(r);
		return toDto(r);
	}

	private static boolean areAdjacent(RestaurantTable a, RestaurantTable b) {
		return a.getAdjacentTableIds().contains(b.getId()) && b.getAdjacentTableIds().contains(a.getId());
	}

	private ReservationViewDto toDto(Reservation r) {
		Long mergedId = r.getMergedTable() != null ? r.getMergedTable().getId() : null;
		String mergedCode = r.getMergedTable() != null ? r.getMergedTable().getCode() : null;
		return new ReservationViewDto(r.getId(), r.getTable().getId(), r.getTable().getCode(), mergedId, mergedCode, r.getStartTime(), r.getEndTime(), r.getGuestCount(), r.getGuestName());
	}
}
