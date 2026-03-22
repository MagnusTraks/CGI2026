package com.project.cgi.dto;

import com.project.cgi.domain.Zone;

import java.util.List;

public record TableLayoutDto(
	long id,
	String code,
	int capacity,
	Zone zone,
	int gridX,
	int gridY,
	int gridW,
	int gridH,
	boolean quietCorner,
	boolean windowSeat,
	boolean nearKids,
	boolean accessible,
	List<Long> adjacentTableIds
) {
}
