package com.project.cgi.service;

import com.project.cgi.domain.Reservation;
import com.project.cgi.domain.RestaurantTable;
import com.project.cgi.domain.Zone;
import com.project.cgi.repository.ReservationRepository;
import com.project.cgi.repository.RestaurantTableRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Demoandmed: lauad + juhuslikud broneeringud (fikseeritud seemega korduvuseks).
 */
@Component
public class RestaurantDataInitializer implements ApplicationRunner {

	private final RestaurantTableRepository tableRepository;
	private final ReservationRepository reservationRepository;
	private final int durationHours;

	public RestaurantDataInitializer(
			RestaurantTableRepository tableRepository,
			ReservationRepository reservationRepository,
			@Value("${app.reservation.duration-hours:2}") int durationHours) {
		this.tableRepository = tableRepository;
		this.reservationRepository = reservationRepository;
		this.durationHours = durationHours;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (tableRepository.count() > 0) {
			return;
		}
		List<RestaurantTable> tables = new ArrayList<>();
		tables.add(table("T1", 2, Zone.TERRACE, 4, 7, 14, 12, false, true, false, true));
		tables.add(table("T2", 4, Zone.TERRACE, 24, 6, 22, 15, true, true, false, true));
		tables.add(table("I1", 2, Zone.INDOOR, 7, 37, 10, 10, true, true, false, false));
		tables.add(table("I2", 2, Zone.INDOOR, 25, 37, 10, 10, false, false, false, false));
		tables.add(table("I3", 4, Zone.INDOOR, 43, 35, 12, 12, false, false, false, false));
		tables.add(table("I4", 6, Zone.INDOOR, 6, 58, 24, 16, false, true, false, true));
		tables.add(table("I5", 8, Zone.INDOOR, 40, 56, 28, 18, false, false, true, true));
		tables.add(table("I6", 2, Zone.INDOOR, 68, 35, 12, 12, true, false, true, true));
		tables.add(table("P1", 4, Zone.PRIVATE_ROOM, 82, 5, 16, 17, true, false, false, true));
		tables.add(table("P2", 6, Zone.PRIVATE_ROOM, 54, 5, 20, 17, true, false, false, true));
		tableRepository.saveAll(tables);
		linkAdjacent(tables, "I4", "I5");
		linkAdjacent(tables, "I1", "I2");
		linkAdjacent(tables, "T1", "T2");
		linkAdjacent(tables, "P1", "P2");
		tableRepository.saveAll(tables);

		Random rng = new Random(42_4242);
		LocalDate today = LocalDate.now();
		LocalTime[] slotStarts = {
				LocalTime.of(11, 30), LocalTime.of(12, 30), LocalTime.of(13, 30),
				LocalTime.of(17, 0), LocalTime.of(18, 0), LocalTime.of(19, 0), LocalTime.of(20, 0)
		};
		List<Reservation> seeded = new ArrayList<>();
		for (int d = 0; d < 8; d++) {
			LocalDate day = today.plusDays(d);
			for (LocalTime time : slotStarts) {
				if (rng.nextDouble() < 0.42) {
					RestaurantTable t = tables.get(rng.nextInt(tables.size()));
					LocalDateTime start = LocalDateTime.of(day, time);
					Reservation r = new Reservation();
					r.setTable(t);
					r.setStartTime(start);
					r.setEndTime(start.plusHours(durationHours));
					r.setGuestCount(Math.min(t.getCapacity(), 2 + rng.nextInt(Math.max(1, t.getCapacity()))));
					r.setGuestName("Demo " + rng.nextInt(9000));
					seeded.add(r);
				}
			}
		}
		List<Reservation> nonOverlapping = new ArrayList<>();
		for (Reservation candidate : seeded) {
			boolean clash = nonOverlapping.stream().anyMatch(
					existing -> existing.getTable().getId().equals(candidate.getTable().getId())
							&& overlaps(existing, candidate));
			if (!clash) {
				nonOverlapping.add(candidate);
			}
		}
		reservationRepository.saveAll(nonOverlapping);
	}

	private static void linkAdjacent(List<RestaurantTable> tables, String codeA, String codeB) {
		RestaurantTable a = tables.stream().filter(t -> codeA.equals(t.getCode())).findFirst().orElseThrow();
		RestaurantTable b = tables.stream().filter(t -> codeB.equals(t.getCode())).findFirst().orElseThrow();
		a.getAdjacentTableIds().add(b.getId());
		b.getAdjacentTableIds().add(a.getId());
	}

	private static boolean overlaps(Reservation a, Reservation b) {
		return a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime());
	}

	private static RestaurantTable table(
		String code,
		int cap,
		Zone zone,
		int gx,
		int gy,
		int gw,
		int gh,
		boolean quiet,
		boolean window,
		boolean kids,
		boolean acc) {
		RestaurantTable t = new RestaurantTable();
		t.setCode(code);
		t.setCapacity(cap);
		t.setZone(zone);
		t.setGridX(gx);
		t.setGridY(gy);
		t.setGridW(gw);
		t.setGridH(gh);
		t.setQuietCorner(quiet);
		t.setWindowSeat(window);
		t.setNearKids(kids);
		t.setAccessible(acc);
		return t;
	}
}
