const DURATION_H = 2;

/** Ümmarguste laudade koodid – renderdatakse ringina SVG-is */
const ROUND_TABLES = new Set(["I1", "I2", "I3"]);

/** Naaberlaupaarid, mida saab liita — kood → naaberkood */
const ADJACENT_CODES = new Map([
	["T1", "T2"], ["T2", "T1"],
	["I1", "I2"], ["I2", "I1"],
	["I4", "I5"], ["I5", "I4"],
	["P1", "P2"], ["P2", "P1"],
]);

const MONTHS_ET = [
	"jaanuar", "veebruar", "märts", "aprill", "mai", "juuni",
	"juuli", "august", "september", "oktoober", "november", "detsember",
];

const floor = document.getElementById("floor");
const form = document.getElementById("search-form");
const bookForm = document.getElementById("book-form");
const msg = document.getElementById("msg");
const partyInput = document.getElementById("party");
const zoneSelect = document.getElementById("zone");
const pickedTable = document.getElementById("picked-table");
const bookBtn = document.getElementById("book-btn");
const guestInput = document.getElementById("guest");
const calGrid = document.getElementById("cal-grid");
const calMonthLabel = document.getElementById("cal-month-label");
const calPrev = document.getElementById("cal-prev");
const calNext = document.getElementById("cal-next");
const mealBox = document.getElementById("meal-suggestion");
const mealImg = document.getElementById("meal-img");
const mealName = document.getElementById("meal-name");
const mealRefresh = document.getElementById("meal-refresh");

const hourDisplay = document.getElementById("hour-display");
const minDisplay = document.getElementById("min-display");
const hourUp = document.getElementById("hour-up");
const hourDown = document.getElementById("hour-down");
const minUp = document.getElementById("min-up");
const minDown = document.getElementById("min-down");

/** Valitud päev kohalikus kuupäevas (YYYY-MM-DD) */
let selectedDateStr = "";
/** Vaadatav kuu */
let viewYear = 0;
let viewMonth = 0;

/** Kellaaja olek */
let timeHour = 18;
let timeMin = 0;

let tables = [];
let reservations = [];
let recommendation = null;
let selectedId = null;
let selectedMergedId = null;
let bestId = null;
let bestMergedId = null;
const busyIds = new Set();

function setMsg(text, isError = false) {
	msg.textContent = text;
	msg.classList.toggle("err", isError);
}

function pad2(n) {
	return String(n).padStart(2, "0");
}

function formatDateStr(d) {
	return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

function parseDateStr(s) {
	const [y, m, d] = s.split("-").map(Number);
	return new Date(y, m - 1, d);
}

// --- Kellaaeg ---

function updateTimeDisplay() {
	hourDisplay.textContent = pad2(timeHour);
	minDisplay.textContent = pad2(timeMin);
}

hourUp.addEventListener("click", () => {
	timeHour = (timeHour + 1) % 24;
	updateTimeDisplay();
	void refreshSlotPreview();
});

hourDown.addEventListener("click", () => {
	timeHour = (timeHour + 23) % 24;
	updateTimeDisplay();
	void refreshSlotPreview();
});

minUp.addEventListener("click", () => {
	timeMin = (timeMin + 15) % 60;
	updateTimeDisplay();
	void refreshSlotPreview();
});

minDown.addEventListener("click", () => {
	timeMin = (timeMin - 15 + 60) % 60;
	updateTimeDisplay();
	void refreshSlotPreview();
});

// --- Kalender ---

function defaultStart() {
	const d = new Date();
	d.setMinutes(0, 0, 0);
	d.setHours(d.getHours() + 3);
	selectedDateStr = formatDateStr(d);
	timeHour = d.getHours() % 24;
	timeMin = 0;
	updateTimeDisplay();
	viewYear = d.getFullYear();
	viewMonth = d.getMonth();
	renderCalendar();
}

function parseStart() {
	const base = parseDateStr(selectedDateStr);
	base.setHours(timeHour, timeMin, 0, 0);
	return base;
}

function slotEnd(start) {
	return new Date(start.getTime() + DURATION_H * 3600 * 1000);
}

function renderCalendar() {
	calMonthLabel.textContent = `${MONTHS_ET[viewMonth]} ${viewYear}`;

	const first = new Date(viewYear, viewMonth, 1);
	const startPad = (first.getDay() + 6) % 7;

	const today = new Date();
	const todayStr = formatDateStr(today);

	calGrid.replaceChildren();

	for (let i = 0; i < 42; i++) {
		const cellDate = new Date(viewYear, viewMonth, i - startPad + 1);
		const ds = formatDateStr(cellDate);
		const displayDay = cellDate.getDate();
		const isOtherMonth =
			cellDate.getFullYear() !== viewYear || cellDate.getMonth() !== viewMonth;

		const btn = document.createElement("button");
		btn.type = "button";
		btn.className = "cal-day";
		btn.setAttribute("role", "gridcell");
		if (isOtherMonth) {
			btn.classList.add("other-month");
		}
		btn.textContent = String(displayDay);

		if (ds === todayStr) {
			btn.classList.add("today");
		}
		if (ds === selectedDateStr) {
			btn.classList.add("selected");
		}

		btn.addEventListener("click", () => {
			selectedDateStr = ds;
			viewYear = cellDate.getFullYear();
			viewMonth = cellDate.getMonth();
			renderCalendar();
			void refreshSlotPreview();
		});

		calGrid.appendChild(btn);
	}
}

calPrev.addEventListener("click", () => {
	if (viewMonth === 0) {
		viewMonth = 11;
		viewYear -= 1;
	} else {
		viewMonth -= 1;
	}
	renderCalendar();
});

calNext.addEventListener("click", () => {
	if (viewMonth === 11) {
		viewMonth = 0;
		viewYear += 1;
	} else {
		viewMonth += 1;
	}
	renderCalendar();
});

async function refreshSlotPreview() {
	try {
		await loadReservationsForSlot();
		renderFloor();
	} catch (e) {
		setMsg(String(e.message || e), true);
	}
}

async function loadTables() {
	const res = await fetch("/api/tables");
	if (!res.ok) throw new Error("Lauade laadimine ebaõnnestus");
	tables = await res.json();
}

async function loadReservationsForSlot() {
	const start = parseStart();
	const end = slotEnd(start);
	const qs = new URLSearchParams({
		start: start.toISOString(),
		end: end.toISOString(),
	});
	const res = await fetch(`/api/reservations?${qs}`);
	if (!res.ok) throw new Error("Broneeringute laadimine ebaõnnestus");
	reservations = await res.json();
	busyIds.clear();
	for (const r of reservations) {
		busyIds.add(r.tableId);
		if (r.mergedTableId != null) {
			busyIds.add(r.mergedTableId);
		}
	}
}

function zonePayload() {
	const z = zoneSelect.value;
	return z === "" ? null : z;
}

async function loadMealSuggestion() {
	try {
		const res = await fetch("/api/meal/random");
		if (!res.ok) {
			mealBox.hidden = true;
			return;
		}
		const m = await res.json();
		mealImg.src = m.thumbnailUrl || "";
		mealImg.alt = m.name || "";
		mealName.textContent = m.name || "";
		mealBox.hidden = false;
	} catch {
		mealBox.hidden = true;
	}
}

function formatPickedLabel(id, mergedId) {
	const t = tables.find((x) => x.id === id);
	if (!t) return String(id);
	if (mergedId == null) {
		return `${t.code} (${t.capacity} kohta)`;
	}
	const u = tables.find((x) => x.id === mergedId);
	if (!u) {
		return `${t.code} + ?`;
	}
	return `${t.code} + ${u.code} (${t.capacity + u.capacity} kohta kokku)`;
}

async function onSearch(e) {
	e.preventDefault();
	setMsg("");
	selectedId = null;
	selectedMergedId = null;
	pickedTable.value = "";
	bookBtn.disabled = true;
	bestId = null;
	bestMergedId = null;
	recommendation = null;

	const start = parseStart();
	const body = {
		start: start.toISOString(),
		partySize: Number(partyInput.value),
		zone: zonePayload(),
		wantsPrivacy: document.getElementById("wPrivacy").checked,
		wantsWindow: document.getElementById("wWindow").checked,
		wantsNearKids: document.getElementById("wKids").checked,
		wantsAccessible: document.getElementById("wAcc").checked,
	};

	try {
		const [recRes] = await Promise.all([
			fetch("/api/recommendations", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(body),
			}),
			loadReservationsForSlot(),
		]);

		if (!recRes.ok) {
			const err = await recRes.json().catch(() => ({}));
			throw new Error(err.detail || "Soovitus ebaõnnestus");
		}
		recommendation = await recRes.json();
		bestId = recommendation.bestTableId ?? null;
		bestMergedId = recommendation.bestMergedTableId ?? null;

		if (bestId != null) {
			selectedId = bestId;
			selectedMergedId = bestMergedId;
			pickedTable.value = formatPickedLabel(bestId, bestMergedId);
			bookBtn.disabled = false;
			const hint = bestMergedId != null
				? "Soovitus: kaks naaberlauda kokku — mõlemad on esile tõstitud."
				: "Parim soovitus on esile tõstitud — vajuta «Kinnita broneering» või vali teine laud.";
			setMsg(hint);
		} else {
			setMsg("Sellele ajale ei leitud sobivat vaba lauda filtritega.", true);
		}
		renderFloor();
		void loadMealSuggestion();
	} catch (err) {
		setMsg(String(err.message || err), true);
	}
}

function tableState(t) {
	if (selectedId != null && t.id === selectedId) return "pick";
	if (selectedMergedId != null && t.id === selectedMergedId) return "pick";
	if (bestId != null && t.id === bestId) return "rec";
	if (bestMergedId != null && t.id === bestMergedId) return "rec";
	if (busyIds.has(t.id)) return "busy";
	return "free";
}

// --- SVG abiprotseduurid ---

function svgEl(tag) {
	return document.createElementNS("http://www.w3.org/2000/svg", tag);
}

function addRect(parent, x, y, w, h, cls, rx = 0) {
	const r = svgEl("rect");
	r.setAttribute("x", String(x));
	r.setAttribute("y", String(y));
	r.setAttribute("width", String(w));
	r.setAttribute("height", String(h));
	if (rx) r.setAttribute("rx", String(rx));
	r.setAttribute("class", cls);
	parent.appendChild(r);
	return r;
}

function addLine(parent, x1, y1, x2, y2, cls) {
	const l = svgEl("line");
	l.setAttribute("x1", String(x1));
	l.setAttribute("y1", String(y1));
	l.setAttribute("x2", String(x2));
	l.setAttribute("y2", String(y2));
	l.setAttribute("class", cls);
	parent.appendChild(l);
}

function addText(parent, x, y, content, cls, anchor = "start") {
	const t = svgEl("text");
	t.setAttribute("x", String(x));
	t.setAttribute("y", String(y));
	t.setAttribute("class", cls);
	t.setAttribute("text-anchor", anchor);
	t.textContent = content;
	parent.appendChild(t);
}

// --- Toolide joonistamine ---

function drawChairDot(parent, cx, cy) {
	const c = svgEl("circle");
	c.setAttribute("cx", cx.toFixed(2));
	c.setAttribute("cy", cy.toFixed(2));
	c.setAttribute("r", "0.95");
	c.setAttribute("class", "chair");
	parent.appendChild(c);
}

function drawChairsRound(parent, cx, cy, tableR, capacity) {
	const dist = tableR + 1.8;
	for (let i = 0; i < capacity; i++) {
		const angle = (i / capacity) * 2 * Math.PI - Math.PI / 2;
		drawChairDot(parent, cx + dist * Math.cos(angle), cy + dist * Math.sin(angle));
	}
}

function drawChairsRect(parent, x, y, w, h, capacity) {
	const perim = 2 * (w + h);
	const topN = Math.max(0, Math.round(capacity * w / perim));
	const botN = Math.max(0, Math.round(capacity * w / perim));
	const rem = capacity - topN - botN;
	const leftN = Math.floor(rem / 2);
	const rightN = rem - leftN;
	const gap = 1.8;

	for (let i = 0; i < topN; i++)
		drawChairDot(parent, x + w * (i + 1) / (topN + 1), y - gap);
	for (let i = 0; i < botN; i++)
		drawChairDot(parent, x + w * (i + 1) / (botN + 1), y + h + gap);
	for (let i = 0; i < leftN; i++)
		drawChairDot(parent, x - gap, y + h * (i + 1) / (leftN + 1));
	for (let i = 0; i < rightN; i++)
		drawChairDot(parent, x + w + gap, y + h * (i + 1) / (rightN + 1));
}

// --- Peasaal ---

function renderFloor() {
	floor.replaceChildren();

	// Tsoonide taustvärvid
	addRect(floor, 0, 0, 51, 28, "zone-bg zone-bg-terrace");
	addRect(floor, 53, 0, 47, 28, "zone-bg zone-bg-private");
	addRect(floor, 0, 28, 100, 72, "zone-bg zone-bg-indoor");

	// Terrassi näärkraad (katusealuse serv, täpsed piirjooned)
	const terrBorder = svgEl("rect");
	terrBorder.setAttribute("x", "0.4");
	terrBorder.setAttribute("y", "0.4");
	terrBorder.setAttribute("width", "50.2");
	terrBorder.setAttribute("height", "27.2");
	terrBorder.setAttribute("fill", "none");
	terrBorder.setAttribute("class", "terrace-border");
	floor.appendChild(terrBorder);

	// Seinad
	addLine(floor, 0, 28, 100, 28, "zone-wall");
	addLine(floor, 51, 0, 51, 28, "zone-divider");

	// Aknad — terrass ülemine sein (T1 ja T2: windowSeat=true)
	for (const wx of [8, 18, 29, 39]) {
		const w = svgEl("rect");
		w.setAttribute("x", String(wx));
		w.setAttribute("y", "0");
		w.setAttribute("width", "5.5");
		w.setAttribute("height", "1.6");
		w.setAttribute("class", "window");
		floor.appendChild(w);
	}
	// Aknad — sisesaali vasak sein (I1 y≈37-47: windowSeat=true; I4 y≈58-74: windowSeat=true)
	for (const wy of [40, 62]) {
		const w = svgEl("rect");
		w.setAttribute("x", "0");
		w.setAttribute("y", String(wy));
		w.setAttribute("width", "1.5");
		w.setAttribute("height", "5");
		w.setAttribute("class", "window");
		floor.appendChild(w);
	}

	// Tsoonide sildid
	addText(floor, 4, 25.5, "TERRASS", "zone-label");
	addText(floor, 76, 25.5, "PRIVAAT", "zone-label", "middle");
	addText(floor, 5, 34, "SISESAAL", "zone-label");

	// Laste mängunurk (x=74–96, y=56–78)
	addRect(floor, 74, 56, 22, 22, "kids-area", 2);
	const kidsBorder = svgEl("rect");
	kidsBorder.setAttribute("x", "74");
	kidsBorder.setAttribute("y", "56");
	kidsBorder.setAttribute("width", "22");
	kidsBorder.setAttribute("height", "22");
	kidsBorder.setAttribute("fill", "none");
	kidsBorder.setAttribute("class", "kids-border");
	floor.appendChild(kidsBorder);
	addText(floor, 85, 63.5, "🎨", "kids-icon", "middle");
	addText(floor, 85, 69, "Laste", "kids-label", "middle");
	addText(floor, 85, 74, "mängunurk", "kids-label", "middle");

	// --- Lauad ---
	for (const t of tables) {
		const g = svgEl("g");
		g.setAttribute("class", "table-group");
		g.addEventListener("click", () => selectTable(t.id));

		const st = tableState(t);
		const isRound = ROUND_TABLES.has(t.code);
		const cx = t.gridX + t.gridW / 2;
		const cy = t.gridY + t.gridH / 2;
		const r = t.gridW / 2;

		// Toolid (taustal, enne lauareksi)
		if (isRound) {
			drawChairsRound(g, cx, cy, r, t.capacity);
		} else {
			drawChairsRect(g, t.gridX, t.gridY, t.gridW, t.gridH, t.capacity);
		}

		// Lauda kuju
		if (isRound) {
			const circle = svgEl("circle");
			circle.setAttribute("cx", String(cx));
			circle.setAttribute("cy", String(cy));
			circle.setAttribute("r", String(r));
			circle.setAttribute("class", `table-shape state-${st}`);
			g.appendChild(circle);
		} else {
			const rect = svgEl("rect");
			rect.setAttribute("x", String(t.gridX));
			rect.setAttribute("y", String(t.gridY));
			rect.setAttribute("width", String(t.gridW));
			rect.setAttribute("height", String(t.gridH));
			rect.setAttribute("rx", "1.5");
			rect.setAttribute("class", `table-shape state-${st}`);
			g.appendChild(rect);
		}

		// Kood ja kohtade arv
		const code = svgEl("text");
		code.setAttribute("class", "table-code");
		code.setAttribute("x", String(cx));
		code.setAttribute("y", String(cy - 1));
		code.setAttribute("text-anchor", "middle");
		code.textContent = t.code;

		const cap = svgEl("text");
		cap.setAttribute("class", "table-cap");
		cap.setAttribute("x", String(cx));
		cap.setAttribute("y", String(cy + 3.2));
		cap.setAttribute("text-anchor", "middle");
		cap.textContent = String(t.capacity);

		g.append(code, cap);
		floor.appendChild(g);
	}
}

function selectTable(id) {
	const t = tables.find((x) => x.id === id);
	if (!t) return;
	if (busyIds.has(id)) {
		setMsg("See laud on valitud ajal juba hõivatud.", true);
		return;
	}

	// Kui klikiti juba valitud lauale — tühista valik
	if (selectedId === id) {
		selectedId = null;
		selectedMergedId = null;
		pickedTable.value = "";
		bookBtn.disabled = true;
		setMsg("Valik tühistatud.");
		renderFloor();
		return;
	}

	// Kui üks laud on juba valitud, kontrolli kas klikkis naaberlaud
	if (selectedId != null && selectedMergedId == null) {
		const existing = tables.find((x) => x.id === selectedId);
		if (existing && ADJACENT_CODES.get(existing.code) === t.code) {
			// Liida kaks naberlauda
			selectedMergedId = id;
			pickedTable.value = formatPickedLabel(selectedId, selectedMergedId);
			bookBtn.disabled = false;
			setMsg(`Valitud kaks lauda: ${existing.code} + ${t.code} (${existing.capacity + t.capacity} kohta kokku).`);
			renderFloor();
			return;
		}
	}

	// Tavaline ühe laua valik
	selectedId = id;
	selectedMergedId = null;
	pickedTable.value = formatPickedLabel(id, null);
	bookBtn.disabled = false;
	setMsg(`Valitud üks laud: ${t.code} (${t.capacity} kohta). Naaberlaua lisamiseks kliki sellele.`);
	renderFloor();
}

async function onBook(e) {
	e.preventDefault();
	if (selectedId == null) return;
	setMsg("");
	const start = parseStart();
	const body = {
		tableId: selectedId,
		start: start.toISOString(),
		partySize: Number(partyInput.value),
		guestName: guestInput.value.trim(),
	};
	if (selectedMergedId != null) {
		body.mergedTableId = selectedMergedId;
	}
	try {
		const res = await fetch("/api/reservations", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(body),
		});
		if (!res.ok) {
			const err = await res.json().catch(() => ({}));
			throw new Error(err.detail || "Broneering ebaõnnestus");
		}
		setMsg("Broneering salvestatud.");
		guestInput.value = "";
		await loadReservationsForSlot();
		renderFloor();
	} catch (err) {
		setMsg(String(err.message || err), true);
	}
}

form.addEventListener("submit", onSearch);
bookForm.addEventListener("submit", onBook);
mealRefresh.addEventListener("click", () => {
	void loadMealSuggestion();
});

defaultStart();
(async () => {
	try {
		await loadTables();
		await loadReservationsForSlot();
		renderFloor();
		void loadMealSuggestion();
	} catch (e) {
		setMsg(String(e.message || e), true);
	}
})();