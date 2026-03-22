package com.project.cgi.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "restaurant_tables")
public class RestaurantTable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 16)
	private String code;

	@Column(nullable = false)
	private int capacity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private Zone zone;

	/** Ruudustiku koordinaadid (0–100), et kuvada SVG-l */
	@Column(nullable = false)
	private int gridX;

	@Column(nullable = false)
	private int gridY;

	@Column(nullable = false)
	private int gridW;

	@Column(nullable = false)
	private int gridH;

	@Column(nullable = false)
	private boolean quietCorner;

	@Column(nullable = false)
	private boolean windowSeat;

	@Column(nullable = false)
	private boolean nearKids;

	@Column(nullable = false)
	private boolean accessible;

	/** Laua ID-d, millega saab kõrvuti kokku lükata (sama broneeringu jaoks). */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "table_adjacent_ids", joinColumns = @JoinColumn(name = "table_id"))
	@Column(name = "adjacent_id")
	private List<Long> adjacentTableIds = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public Zone getZone() {
		return zone;
	}

	public void setZone(Zone zone) {
		this.zone = zone;
	}

	public int getGridX() {
		return gridX;
	}

	public void setGridX(int gridX) {
		this.gridX = gridX;
	}

	public int getGridY() {
		return gridY;
	}

	public void setGridY(int gridY) {
		this.gridY = gridY;
	}

	public int getGridW() {
		return gridW;
	}

	public void setGridW(int gridW) {
		this.gridW = gridW;
	}

	public int getGridH() {
		return gridH;
	}

	public void setGridH(int gridH) {
		this.gridH = gridH;
	}

	public boolean isQuietCorner() {
		return quietCorner;
	}

	public void setQuietCorner(boolean quietCorner) {
		this.quietCorner = quietCorner;
	}

	public boolean isWindowSeat() {
		return windowSeat;
	}

	public void setWindowSeat(boolean windowSeat) {
		this.windowSeat = windowSeat;
	}

	public boolean isNearKids() {
		return nearKids;
	}

	public void setNearKids(boolean nearKids) {
		this.nearKids = nearKids;
	}

	public boolean isAccessible() {
		return accessible;
	}

	public void setAccessible(boolean accessible) {
		this.accessible = accessible;
	}

	public List<Long> getAdjacentTableIds() {
		return adjacentTableIds;
	}

	public void setAdjacentTableIds(List<Long> adjacentTableIds) {
		this.adjacentTableIds = adjacentTableIds != null ? adjacentTableIds : new ArrayList<>();
	}
}
