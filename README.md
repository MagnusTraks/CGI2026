# Nutikas Restorani Reserveerimissüsteem

CGI praktikaülesanne, lauabroneerimine koos nutika soovitusmootoriga.

---

## Käivitamine

### Eeldused
- Java 21
- Maven (või kasuta kaasasolevat `mvnw` wrapperit)

### Lokaalselt

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / Mac
./mvnw spring-boot:run
```

Rakendus käivitub aadressil: **http://localhost:8080**

H2 andmebaasi konsool: **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:testdb`
- Kasutaja: `sa`
- Parool: *(tühi)*

> Andmebaas on in-memory (`create-drop`) — rakenduse taaskäivitusel lähtestatakse kõik andmed.

### Dockeriga

```bash
docker build -t nutikas-restoran .
docker run --rm -p 8080:8080 nutikas-restoran
```

### Testid

```bash
.\mvnw.cmd test
```

---

## Funktsionaalsus

- **Lauaplaan SVG-na** Restorani põrandaplaan renderdatakse reaalajas SVG-na. Ümmargused lauad (I1, I2, I3) kuvatakse ringidena, ülejäänud ümardatud ristkülikutena. Laudade ümber on väikese mummudena märgitud toolid.
- **Kuupäeva- ja kellaajavalija** Broneering saab alguse valitud kellaajal, kestus on 2 tundi (seda saab muuta application.properties).
- **Isikute arv + eelistused** Kasutaja saab valida seltskonna suuruse ning eelistused: aknakoht, vaikne nurk, laste nurga lähedus, ratastooliligipääs.
- **Nutikas soovitus** Backend skoorib vabu laudu kaalutud valemiga (mahutavuse efektiivsus, täpne mahutavus, eelistuste vastavus, naaberlauapaar). Parimad soovitused kuvatakse järjestatult.
- **Naaberlaudade ühendamine** Kui ükski üksiklaud ei mahu, saab kaks naabrist lauda broneerida ühena (T1 ja T2, I1 ja I2, I4 ja I5, P1 ja P2).
- **Broneering** Valitud lauale saab broneeringu teha; broneeritud lauad kuvatakse plaanil erinevalt.
- **Toidusoovitused** Dekoratiivselt päritakse TheMealDB API-st suvaline roog (ebaõnnestumine varjatakse vaikselt).

---

## Arhitektuur

### Backend (Spring Boot / Java 21)

| Klass | Roll                                                                                          |
|---|-----------------------------------------------------------------------------------------------|
| `RestaurantDataInitializer` | Seab H2 andmebaasi 10 laua ja juhusliku broneeringute komplektiga (seed `42_4242`) käivitusel |
| `TableRecommendationService` | Skoorib laudu kaalutud valemiga; käsitleb ka naaberlauapaare                                  |
| `ReservationService` | Loob üksik- ja ühendatud broneeringud                                                         |
| `RestaurantApiController` | REST API — `/api/tables`, `/api/zones`, `/api/reservations`, `/api/recommendations`           |
| `MealDbService` | Pärib TheMealDB välist API-t                                                                  |

**Skoori valem (üksiklaud):**
```
skoor = 100 − raiskamine × 18.0
      + (55.0 kui mahutavus täpselt)
      + iga eelistuse vastavuse eest ±38.0 / −22.0
```
Naaberlauapaarile lisandub penaltiga `−12.0`, et eelistada ühte lauda võrdse skoori korral.

### Frontend (Vanilla JS / SVG)

- `index.html` struktuur
- `app.js` üks ES-mooduli fail (~450 rida): kalender, kellaajavalija, lauaolekute haldus, SVG renderdamine, API kutsed
- `styles.css` tume teema CSS muutujatega

### Tsoonid

| Tsoon | Asukoht |
|---|---|
| Terrass (`TERRACE`) | Vasakul üleval, y < 28 |
| Privaatne ruum (`PRIVATE_ROOM`) | Paremal üleval, x > 53, y < 28 |
| Siseala (`INDOOR`) | Allosas, y > 28 |

---

## REST API lühiülevaade

| Meetod | Tee | Kirjeldus |
|---|---|---|
| GET | `/api/tables` | Kõik lauad SVG koordinaatide ja eelistustega |
| GET | `/api/zones` | Tsoonide nimekiri |
| GET | `/api/reservations?start=…&end=…` | Broneeringud ajavahemikus |
| POST | `/api/recommendations` | Skooritud soovitused (partySize, start, eelistused) |
| POST | `/api/reservations` | Uue broneeringu loomine |

---

## Arendusprotsess

### Tööks kulunud aeg

Kogu projekti valmimiseks kulus kokku umbes 10 tundi tööd. Alustasin planeerimisest ja andmemudeli loomisest, mis võttis esimese tunni, et paika panna restorani saaliplaani loogika ja tsoonid. Järgmised neli tundi kulusid back-end arendusele, kus keskendusin Spring Booti teenuste kihile, broneerimisloogikale ja soovituste skoorimissüsteemile. Front-end lahenduse ja dünaamilise SVG plaani joonistamine võttis aega kolm tundi, millele järgnes tund aega välise API integreerimiseks ja Dockeriseerimiseks. Viimase tunni pühendasin koodi silumisele, testimisele ja dokumentatsiooni vormistamisele.

### Mis oli keeruline

Kuna olen neid tehnoloogiaid korduvalt kasutanud, läks töö üldpildis päris sujuvalt. Kõige suuremat väljakutset pakkus nutika soovituse algoritmi väljatöötamine. Keeruline oli leida matemaatiliselt õiglane tasakaal laua mahutavuse efektiivsuse ja kliendi erisoovide vahel, et süsteem ei jätaks asjatult tühje kohti, kuid pakuks samas eelistatud asukohti nagu aknakoht või vaikne nurk. Samuti nõudis üksjagu süvenemist dünaamilise saaliplaani loomine puhtas JavaScriptis ilma väliste raamistiketa, kuna pidin tagama, et laudade ja toolide asukohad ning nende visuaalne olek sünkroniseeruksid veatult backend-ist tulevate andmetega.

### Probleemid ja nende lahendamine

Arenduse käigus tekkis küsimusi algoritmi kaalude osas, sest alguses kippus süsteem eelistama liiga suuri laudu, kui need klappisid kliendi lisasoovidega. Lahendasin selle "raiskamise trahvi" kontseptsiooniga, mis langetab skoori vastavalt tühjaks jäävate toolide arvule. Keerulisemate SVG-elementide, näiteks toolide ringikujulise paigutuse arvutamiseks laua ümber, kasutasin abi tehisintellektilt ja matemaatilistelt valemitelt. Muu tehnilise toe osas tuginesin peamiselt Spring Booti ja MDN-i ametlikule dokumentatsioonile.
### Piirangud

Praegune lahendus on funktsionaalne, kuid sisaldab teatud piiranguid. Näiteks võib väga suure koormuse ja samaaegsete päringute puhul tekkida race condition, kus kaks kasutajat proovivad broneerida sama lauda. Selle vältimiseks tuleks tulevikus rakendada andmebaasi tasemel unikaalsuspiiranguid või kasutada optimistlikku lukustamist. Lisaks on hetkel võimalik liita maksimaalselt kaks eelnevalt määratud naaberlauda, mis tähendab, et süsteem ei toeta veel dünaamilist suuremate laudade rühmade moodustamist väga suurte seltskondade jaoks. Lisaks kahetsen, et ei teinud pidevaid committe, vaid laadisin töö Githubi alles siis, kui see oli valmis.

---

## Välised allikad ja AI kasutamine

- **TheMealDB** — avalik toiduandmete API, kasutusel dekoratiivselt: https://www.themealdb.com/api.php
- **Spring Boot** — raamistiku dokumentatsioon: https://docs.spring.io/spring-boot/
- **H2 Database** — in-memory andmebaas testimiseks: https://h2database.com/

Projekti arendamisel kasutasin Cursori koodiredaktori funktsioone, kuid kogu rakenduse arhitektuur, äriloogika ja mahukamad koodiplokid on minu enda kirjutatud. AI roll piirdus peamiselt tööprotsessi kiirendamisega, näiteks kasutasin Cursorit lühemate süntaksivigade leidmiseks või koodierrorite silumiseks, lisaks oli see abiks ka üksiklaua skoori leidmise valemi tasakaalustamisel. Samuti oli AI-st abi projekti lõpufaasis koodi refaktoreerimisel, et tagada parem loetavus ja optimaalsem ülesehitus.
