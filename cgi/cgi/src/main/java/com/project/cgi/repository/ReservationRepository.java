package com.project.cgi.repository;

import com.project.cgi.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	@Query("""
			SELECT r FROM Reservation r
			WHERE r.startTime < :end AND r.endTime > :start
			AND (r.table.id = :tableId
				OR (r.mergedTable IS NOT NULL AND r.mergedTable.id = :tableId))
			""")
	List<Reservation> findOverlappingForTable(
			@Param("tableId") Long tableId,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);

	@Query("""
			SELECT DISTINCT r FROM Reservation r
			JOIN FETCH r.table t
			LEFT JOIN FETCH r.mergedTable m
			WHERE r.startTime < :rangeEnd AND r.endTime > :rangeStart
			""")
	List<Reservation> findInTimeRange(
			@Param("rangeStart") LocalDateTime rangeStart,
			@Param("rangeEnd") LocalDateTime rangeEnd);
}
