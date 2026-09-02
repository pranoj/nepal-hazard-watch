# Nepal Hazard Watch — Historical Incident Catalog & Backtesting Tiers (Phase 0)

Research date: 2026-09-01. This is safety-critical, factual research. Every event below is sourced; anything not independently cross-confirmed is flagged. **Casualty and volume figures for recent events are moving targets — never hardcode a "final" number.**

## Critical disambiguation

Nepal has **two unrelated rivers both called "Bhote Koshi,"** and conflating them corrupts the catalog:

1. **Bhote Koshi (Sindhupalchok / Tatopani corridor)** — from the Zhangzangbo/Poiqu glacier system near Nyalam/Zhangmu, Tibet; enters Nepal at Kodari/Tatopani; becomes the Sun Koshi at Barabise. This is the basin of the well-documented 1935/1964/1981/1983/2000/2002/2016 GLOF series. **NOT our target corridor.**
2. **Bhote Koshi / Lhende Khola (Rasuwa / Kyirong corridor)** — formed by the Lhende Khola (draining SE Tibet/Gyirong County, including Langtang Lirung) joining the Kyirong Tsangpo at Rasuwagadhi, then becoming the Trishuli River. **This is our target corridor** — Events A and B below, ~60–90 km west of the Sindhupalchok corridor.

`regions/lhende-bhote-koshi.json` (Phase 1) must encode corridor #2 only. Do not reuse the Sindhupalchok events as positive test cases for this region's detector — they are valuable context but the wrong watershed.

---

## EVENT A — July 8, 2025: Purepu Glacier supraglacial lake drainage (Rasuwagadhi flood)

**Corridor:** Rasuwa / Kyirong / Lhende Khola–Trishuli (target corridor). **Confidence: MEDIUM.**

- **Date/time:** July 8, 2025. A ~3:15am NPT arrival time is cited by one source (Stimson Center) — LOW-MEDIUM confidence, uncorroborated by an official timestamp.
- **Location:** Trigger — supraglacial lake on Purepu ("Pyurepu") Glacier, Gyirong County, TAR China, ~5,100–5,150 m. Rasuwagadhi border: 28.27875°N, 85.37808°E, ~31–35 km downstream of the lake.
- **River(s):** Lhende River (China) → Rasuwagadhi border → Bhote Koshi (Rasuwa) → Upper Trishuli; effects traced 100+ km downstream.
- **Mechanism:** Rapid drainage of a supraglacial lake on debris-covered Purepu Glacier, independently confirmed by HiRISK, IMHE, University of Alaska Fairbanks, ICIMOD, and a 2026 EGUsphere preprint. Rainfall was in a lull — ruling out a precipitation trigger. This was the **second** drainage of this lake system; an earlier, smaller drainage occurred July 12–15, 2023 (worth treating as its own, smaller backtest case if satellite archive depth allows).
- **Magnitude:** Lake area peaked ~638,000 m² (July 7), fell to ~435,000 m² (July 8). Best volume estimate (DEM-based, EGUsphere preprint): ~3.55 million m³ lost, ~71–78% complete — an estimated 15–20 million m³ remained stored, flagged as an ongoing hazard (relevant to Event B, 13 months later, same broader area).
- **Downstream impact:** Rasuwagadhi border crossing/customs destroyed; Miteri Pul ("Friendship Bridge") swept away; Nepal-China trade halted; hydropower stations damaged (Rasuwagadhi, Trishuli III, Trishuli, Benighat) — MW-loss figures disputed across sources (~250 MW vs ~211 MW, unreconciled).
- **Fatalities:** Disputed and not confirmed closed. Most-repeated figure: 9 confirmed dead + 19 missing in Nepal, plus 11 missing in Tibet (~28-29 total) — but Stimson Center separately reports "at least 11 dead, 17 missing." Treat as an unreconciled range, not a fixed number.
- **Data available for reconstruction:** PlanetScope, Sentinel-1 SAR, Sentinel-2, Landsat 8/9, 8m HMA DEM (all used in the EGUsphere study, delayed by monsoon cloud cover, motivating use of tasked commercial SAR for faster confirmation). A seismic station near Kathmandu reportedly showed a signal ~1 hour before border arrival (Eos/Landslide Blog) — station ID and exact timing not pinned down, worth chasing in Phase 1/3. No hydrological gauge data or pre-event alert found; the Purepu lakes were **not being actively monitored** before this event.
- **Sources:** Eos Landslide Blog; Kathmandu Post (Aug 28, 2026); Stimson Center (2025); EGUsphere preprint 2026; Khabarhub; Reuters/Yahoo mirror; Wikipedia "2025 Nepal floods."

---

## EVENT B — August 26, 2026: Langtang ice-rock avalanche and Lhende Khola outburst flood

**Corridor:** Rasuwa / Kyirong / Lhende Khola–Trishuli (target corridor), ~13 months after Event A, **different mechanism** — the detector must not assume GLOF-only. **Confidence: MEDIUM for mechanism/location; LOW for exact magnitude and casualty totals (still moving 6 days in).**

- **Date/time:** Wednesday, August 26, 2026. USGS origin time **02:52:10 UTC = 08:37:10 NPT** — HIGH confidence (USGS event `us7000tbwb`). A possible secondary M4.2 event ~3 hours later is single-source/unverified.
- **Location:** North face of Langtang Lirung, Nepali side of the border, ~5,200–5,400 m, within Langtang National Park. USGS: 28.271°N, 85.515°E. Mass fell ~1,200 vertical meters into the Lhende Khola.
- **River(s):** Lhende Khola (temporarily dammed, then breached) → Bhote Koshi (Rasuwa) at Rasuwagadhi → Trishuli → Narayani → Gandak (India); bodies recovered as far as Kushinagar/Maharajganj districts, Uttar Pradesh (~240 km downstream).
- **Mechanism:** USGS revised its own classification within ~2 days: **M4.4 "earthquake" → M5.2 "landslide."** Multiple independent analyses (ICIMOD, Eos/Petley, AntarcticGlaciers.org, Nature) converge on a **bedrock failure that swept an overlying glacier with it** — primarily a rock/ice avalanche, explicitly **not** a classical GLOF (no major pre-existing lake identified at the failure site). The debris temporarily dammed the Lhende Khola before the dam breached. Climate attribution is explicitly **not yet established** by researchers quoted.
- **Magnitude:** Unreconciled across sources (no single authoritative m³ figure). More consistent data points: Planet Labs identified a 600 m-wide collapse scar; Trishuli rose ~9 m in ~30 minutes at an unspecified downstream point; flood/debris traveled 60–100 km.
- **Downstream impact:** Rasuwagadhi border post destroyed; Timure trading village scoured; Syabrubesi submerged in mud; Miteri/Friendship Bridge destroyed again (same structure as Event A); affected districts: Rasuwa, Nuwakot, Makawanpur, Dhading, Gorkha, Tanahu, Nawalparasi West/East, Chitwan; Gyirong crossing on the China side also hit. ICIMOD notes the disaster started on the Nepali side and **fell outside the reach of China's early-warning system.**
- **Known impact times:** One source claims ~3 minutes from failure to border arrival (single-source, unverified); the "~9m rise in ~30 min downstream" figure is more widely repeated. **This corridor's actual observed warning window is on the order of 10–40 minutes** — use this as the realistic target for "potential warning time," not a larger number.
- **Fatalities (as-of-date snapshot — DO NOT treat as final):**

| Date | Dead | Missing | Notes |
|---|---|---|---|
| Aug 27 (~11:00 UTC) | ≥300 | >900 | — |
| Aug 28 | — | — | ~470 combined |
| Aug 30 | 734 | 2,498 | ~$4–5B damage estimate |
| Aug 31 | 903 | 2,498 (incl. 589 foreign nationals) | — |
| Sept 1 | ≥1,003 | 3,916 (Nepal-wide) | ~747 in Rasuwa alone, incl. ~583 tourists |

  Store every figure with an explicit as-of timestamp in the database; never overwrite with a single "true" value.
- **Data available for reconstruction:** USGS global seismic network (detected/located; auto-classified as tectonic before manual reclassification within ~2 days — directly relevant precedent for the detector's evidence-engine design). Planet Labs imagery; "Chinese satellite imagery reportedly shared directly with Nepal's DHM" (rare cross-border sharing, worth chasing for Phase 1). River-stage rise implies some hydrological observation existed but no gauge ID confirmed. No quantitative rainfall dataset cited. **No pre-event alert** — the slope "had not been heavily studied or closely watched" (Alton Byers, CU Boulder, quoted in coverage).
- **Sources:** USGS Landslide Hazards program page; USGS event `us7000tbwb`; Kathmandu Post (Aug 26, 28, 30); ICIMOD; AntarcticGlaciers.org; Nature news; EarthSky; Al Jazeera (multiple, Aug 27–31); The Print; Mappr.co; ABC News (Sept 1); severe-weather.eu; English NepalNews timeline; CARE.

---

## Additional historical events researched

**Finding:** every specifically-named "classic" event for 1935, 1964 (Cirenmaco), 1981, 1983, 2000, 2002, and 2016 belongs to the **Sindhupalchok** corridor — the wrong watershed for this project's detector. Only two much sparser events were found that are genuinely in the Rasuwa/Trishuli corridor, and both have essentially no reconstructable quantitative detail.

### Sindhupalchok corridor (wrong watershed — context only, do not use as positive test cases)
- **1935 — Taraco Lake outburst**, Tajilingpu valley. Destroyed livestock and ~66,000 m² of fields; no fatalities documented. LOW confidence, single secondary source.
- **1964 / July 11, 1981 / 1983 — Cirenmaco Lake GLOFs**, Zhangzangbo valley. 1981 is well-documented: ice avalanche into a moraine-dammed lake, destroyed 3 bridges (including one at the border), damaged the Araniko Highway, **>200 fatalities**. MEDIUM-HIGH confidence for 1981; LOW for 1964/1983 standalone detail. Pre-satellite/seismic era — not usable for modern backtesting.
- **August 2000 — Cirenmaco/Zhangzangbo recurrence.** Destroyed 98 bridges + ~10,000 houses in Tibet, ~$75M loss. MEDIUM confidence, single detailed source.
- **May–June 2002 — Jialongco Lake, two bursts.** Damaged the China-Nepal highway and a hydropower station. MEDIUM confidence.
- **July 5, 2016 — Gongbatongsha GLOF.** Small initial GLOF amplified into a documented "giant transborder flood and debris flow" (peer-reviewed, *Scientific Reports*), crossing the border at ~4,610 m. >$70M economic loss; no fatality figure found. MEDIUM-HIGH confidence — and notably **within the modern Sentinel/Landsat/seismic era**, so this event (despite being the wrong watershed for our region config) is a genuinely strong **Tier B candidate for a second region** if the project later expands to the Sindhupalchok corridor.

### Rasuwa/Trishuli corridor (correct watershed, but data-poor)
- **August 25, 1964 — "Longda" GLOF, Trishuli basin.** Lake origin in TAR China near Kyirong (28°37'01"N, 85°20'58"E) — plausibly the same broader sub-basin as Purepu/Lhende, not confirmed identical. Mechanism, magnitude, impact, and fatalities are all recorded as **"Not known"** in the source inventory (World Bank/GFDRR, UNISDR/ICIMOD). LOW-MEDIUM confidence the event occurred at all; effectively no reconstructable quantitative detail.
- **June 6, 1995 — "Zanaco" GLOF, Trishuli basin.** Coordinates 28°39'44"N, 85°22'19"E, ~4 km from the 1964 Longda site. Same "Not known" caveats. LOW-MEDIUM confidence.

### Could not verify / explicitly out of scope
- No standalone 1935 or 1964 event specific to Rasuwa/Kyirong beyond the bare "Longda" table row.
- No 2002 or 2016 event in the Rasuwa/Trishuli corridor — both documented years are Sindhupalchok events.
- Regional context events confirmed **not** in this corridor (useful for negative-testing / other-basin comparison, not this region's positive catalog): 2024 Thame GLOF (Dudh Koshi, Aug 18 2024); April 2025 Limi Valley thermokarst outburst (Humla); 2023 South Lhonak GLOF (Sikkim, India); 2021 Melamchi disaster (Sindhupalchok, June 15 2021); 2014 Jure landslide/Sunkoshi dam (Sindhupalchok, Aug 2 2014, 156 dead).
- Wikipedia's "2026 Nepal floods" page could not be fetched during this research pass (tool error) — likely the most complete running timeline; re-fetch before finalizing the catalog in Phase 1.
- Neither Event A nor Event B has a confirmed *final* casualty total as of this writing.

---

## Backtesting feasibility tiers

Per the master brief's Tier A/B/C system: Tier A = strongest modern replay candidate (reconstruct what was digitally available minute-by-minute); Tier B = partial replay possible; Tier C = valuable for mechanism/magnitude/geography context only, not a valid minute-by-minute test.

| Event | Seismic | River/hydro | Rainfall | Satellite | Official alerts | Precise impact time | Tier | Why |
|---|---|---|---|---|---|---|---|---|
| **Event B — Aug 26, 2026** | YES (USGS `us7000tbwb`, global + `NK.KKN`) | PARTIAL (no confirmed gauge ID; gauges on this corridor destroyed at ~8:40am) | PARTIAL (no dataset directly cited; IMERG Early Run reconstructable) | YES (Planet Labs; possible Chinese imagery shared with DHM) | YES (SMS chain reconstructable: 9:00am call → 9:16am SMS) | PARTIAL (border-arrival time ~3 min claim unverified; ~9m rise in ~30 min is the sturdier anchor) | **A** | Freshest event, richest documentation, modern data stack fully available; the one gap is a confirmed precise impact timeline, which is workable with available anchors |
| **Event A — Jul 8, 2025** | PARTIAL (1 unconfirmed station/timing claim) | NO (no gauge data found; lakes were not being monitored) | PARTIAL (IMERG reconstructable; rainfall was NOT the trigger per sources) | YES (PlanetScope, Sentinel-1/2, Landsat, HMA DEM — but monsoon-cloud-delayed) | NO (no pre-event alert; response was reactive) | PARTIAL (~3:15am claim low-confidence) | **A** (weaker than B) | Modern data stack available but with real gaps (no gauge data, cloud-delayed satellite, one unconfirmed seismic claim) — still within the "we can attempt full receivedAt-honest reconstruction" era |
| **2016 Gongbatongsha (Sindhupalchok — wrong watershed)** | Likely partial (post-Gorkha-earthquake seismic deployment may cover this era/region) | Unconfirmed | Unconfirmed | YES (peer-reviewed study used satellite) | Unconfirmed | Unconfirmed | **B** (if used at all — only valid for a *different* region config, not `lhende-bhote-koshi.json`) | Modern-era, peer-reviewed, but wrong corridor for this region — do not mix into this region's evaluation set |
| **1981 Cirenmaco (Sindhupalchok)** | NO | NO | NO | NO | NO | NO (qualitative only) | **C** | Pre-satellite, pre-digital-seismic era; valuable for magnitude/mechanism/recurrence context only |
| **1935 Taraco, 1964/1983 Cirenmaco, 2000 Zhangzangbo, 2002 Jialongco (Sindhupalchok)** | NO | NO | NO | NO (2000/2002 predate the modern Sentinel/Landsat-open-archive era for practical reconstruction) | NO | NO | **C** | Same reasoning — historical/context value only |
| **1964 Longda, 1995 Zanaco (Rasuwa/Trishuli — right watershed)** | NO | NO | NO | NO | NO | NO | **C** | Right watershed, but "Not known" mechanism/magnitude/impact in the only source found — cannot be reconstructed at all; value is solely "this corridor has GLOF history going back decades" |

**Practical implication for Phase 5 (Historical Replay):** Build and validate the replay engine against **Event B first** (richest data, freshest, best-anchored impact-time signal), then **Event A** as the second positive test — and be honest that even these two "Tier A" events have real gaps (no confirmed river gauge data for either, an unconfirmed seismic claim for Event A, an unconfirmed precise border-arrival time for Event B). The 2016 Gongbatongsha event is a legitimate Tier B candidate but only for a *separate* Sindhupalchok region config — do not fold it into `lhende-bhote-koshi.json` testing. All Tier C events belong in `historical_incidents` for context/recurrence statistics only, explicitly flagged as not valid for minute-by-minute replay, per the master brief's rule against pretending modern data existed historically.

---

## Full source list

**Event A:** Eos Landslide Blog (eos.org/thelandslideblog/rasuwagadhi-1) · Kathmandu Post (Aug 28, 2026) · Stimson Center (2025) · EGUsphere preprint 2026-4065 · Khabarhub (Oct 2025) · Reuters/Yahoo mirror · Wikipedia "2025 Nepal floods"

**Event B:** USGS Landslide Hazards program page · USGS event `us7000tbwb` · Kathmandu Post (Aug 26/28/30, 2026) · ICIMOD (Kyirong-Rasuwa Flood 2026) · AntarcticGlaciers.org · Nature news (d41586-026-02716-w) · EarthSky · Al Jazeera (Aug 27, 28, 30) · The Print · Mappr.co · ABC News (Sept 1) · severe-weather.eu · English NepalNews (multiple long-reads) · CARE · nepaldisasterupdatelive tracker

**Historical events:** World Bank/GFDRR GLOF Nepal report · UNISDR/ICIMOD GLOF report · *Scientific Reports* transborder GLOF paper (s41598-022-16337-6) · Wikipedia "Bhotekoshi River" · BioOne Poiqu/Bhote Koshi GLOF risk paper (abstract only)

*(Full URLs for every citation above are available in the research session and can be re-supplied on request; omitted here for readability — request the unabridged source list if needed for the published `historical_incidents` table's `sourceNotes` field.)*