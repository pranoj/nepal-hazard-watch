# Nepal Hazard Watch — Data Source Plan (Phase 0)

Status: research complete 2026-09-01. This document is the verified, honest answer to "what free/public data can we actually use" — not an assumption of what should be available. Where something could not be verified, it is marked UNKNOWN rather than guessed.

**Headline finding:** The corridor this project targets (Lhende Khola / Rasuwa / Bhote Koshi) suffered two real GLOF/glacier-collapse floods during this research — July 8, 2025 and August 26, 2026 (the latter six days before this document was written, 1,000+ dead as of Sept 1, 2026). Both are documented in `historical-incidents.md`. The Aug 2026 event functioned as an unplanned live test of nearly every data source below, and that real-world evidence is used throughout this document instead of speculation.

---

## 1. Seismic data

### 1.1 USGS Earthquake Catalog (GeoJSON feeds + FDSN Event Web Service)
- **Org / docs:** USGS Earthquake Hazards Program (ANSS). [Feeds](https://earthquake.usgs.gov/earthquakes/feed/) · [FDSN Event API](https://earthquake.usgs.gov/fdsnws/event/1/) · [ComCat docs](https://earthquake.usgs.gov/data/comcat/)
- **Data:** magnitude, place, time, depth, lat/lon, review status, event-type field (can be `landslide`, not just `earthquake`)
- **Live/delayed:** Feeds update ~every 60s; USGS itself warns international events "often" aren't reported for 15+ minutes. No published numeric SLA for auto-detection latency near Nepal/Tibet specifically — UNKNOWN.
- **Historical archive:** Decades deep via FDSN queries; exact depth for the Nepal/Tibet border not independently verified.
- **Access:** Plain HTTPS GET, GeoJSON/CSV/QuakeML. No SDK.
- **Auth:** None.
- **Rate limits:** Rate-limited (HTTP 429), no published numeric threshold found; recommend gzip + `updated`-timestamp checks, no polling faster than 60s.
- **Cost / license:** Free, public domain (US federal data); some ANSS-contributed data may carry separate terms — not fully verified.
- **Real-world test (Aug 26, 2026 event):** USGS's automated pipeline **first classified the collapse as a M4.4 tectonic earthquake**, and only revised it to **M5.2 "landslide"** after manual analyst review of waveform character. The corrected classification landed roughly 2 days later.
- **Bottom line:** Free, real, no auth — but the one live test we have shows the fast/automated layer got the event *type* wrong, and the correct classification required human review. Treat as "fast flag that something seismic happened," not "fast, correctly-typed glacier-collapse alert."

### 1.2 FDSN web services / EarthScope (successor to IRIS) — waveform data
- **Org / docs:** EarthScope Consortium. [FDSNWS Dataselect](https://service.iris.edu/fdsnws/dataselect/docs/1/help/) · [event page for the Aug 2026 event](https://www.earthscope.org/geophysical-event/nepal-glacial-collapse-and-debris-flow-us7000tbwb/)
- **Data:** Raw waveforms (miniSEED), station metadata.
- **Live/delayed:** Dataselect REST service is explicitly **archive-only** ("polling for real-time continuous data is not allowed"); real-time needs SeedLink. Numeric SeedLink latency not verified — UNKNOWN.
- **Station coverage near our corridor — the binding constraint:**
  - GEOFON (GFZ): no stations in Nepal/Tibet.
  - Nepal's own network (FDSN code `NK`, National Seismological Centre): registered since 1978, but NSC's own registry text states it does **not yet run its own public real-time web service** — external access is via FDSN federation/bulletins. One station, `NK.KKN`, did record the Aug 2026 event, but its distance from Rasuwa/Langtang was not confirmed (inferred ~60–80+ km, near Kathmandu).
  - Nepal School Seismology Network (Raspberry Shake-based, open FDSN/SeedLink): real but covers **western Nepal (27.5–29.1°N, 82.5–85°E)**, which excludes the Rasuwa/Langtang corridor (~28.1–28.3°N, 85.5–85.9°E).
- **Auth:** None for open networks.
- **License:** Not explicitly verified; broadly treated as open for research use.
- **Cost:** Free.
- **Bottom line:** Physically capable of the job, but there is **no dense, near-source station coverage in this exact corridor today**. Every successful Himalayan case in the literature (below) used stations 12–20 km from the source; nothing that close is confirmed to exist here.

### 1.3 Published science on seismic detection of Himalayan glacier collapse / GLOFs
This is the strongest evidence the underlying physics works, and it directly informs what thresholds/architecture are realistic:

- **Cook et al. 2020, *Science Advances*** — studied the **July 5, 2016 GLOF on the Bhote Koshi itself** (Sindhupalchok corridor, a *different* river than our target — see historical-incidents.md), using 6 seismometers that happened to be in place post-Gorkha-earthquake. Detected a clear two-phase signal. Explicitly **not** an operational early-warning demonstration — post-hoc analysis only. A cited 1994 Bhutan case found 5 hours of lead time from stations ~100 km away, but flagged as a single case, uncertain to generalize.
- **Chamoli, India, Feb 2021** (closest well-studied analog, not Nepal): stations 13–20 km away gave **~10–14 min lead time** (Kumar et al. 2023); a closer 12 km station captured **~2.5 hours of precursory tremor** before collapse (Shukla et al. 2022) — the single most encouraging number found, but distance-dependent and not guaranteed to generalize to a fast/sudden collapse with no slow nucleation phase.
- **Every source** (Cook et al., Columbia Climate School coverage) explicitly states operational, automated, real-time seismic early warning **has not been deployed** anywhere in the Himalaya — the barrier named repeatedly is maintaining real-time infrastructure in this terrain, which is an institutional/engineering problem, not a physics problem.

### 1.4 Bottom line on seismic
Real signal, proven physics, free access, but **not currently a standalone fast-detection source for this corridor**: no dense near-source stations exist, and the one live test (Aug 2026) shows the fast/automated layer misclassified the event type. Treat seismic as one free, valuable, corroborating signal — good for "something happened" flags and for backtesting — not the sole trigger. Advocate for near-source low-cost seismometers (Raspberry-Shake class, modeled on the existing Nepal School Seismology Network) as a concrete, cheap future improvement.

---

## 2. Nepal government sources

### 2.1 BIPAD Portal (bipadportal.gov.np)
- **Org:** NDRRMA (Nepal), built with UNDP support.
- **Data:** Disaster incident records, hazard layers; per NDRRMA's sensor network, feeds from ~200 hydro stations and ~360 automatic weather stations; a pilot impact-based-forecasting module ingesting Copernicus GLoFAS forecasts (not for our corridor currently).
- **API:** **No public REST API or developer documentation found.** The portal is a JS SPA with no discoverable machine-readable endpoint, no Swagger/OpenAPI, no GitHub client. Historical incident export appears to be manual/UI-driven.
- **Auth / license / cost:** Undocumented — no API surface was found to characterize.
- **Real-world latency (Aug 2026 event):** Alerting reached the public via SMS ~16 minutes after the flood had already crossed into Nepal, triggered by a **phone call**, not automated telemetry.
- **Bottom line:** Not usable today as a programmatic input. Possibly useful later as a place to push our own findings/alerts to, or for manually-exported historical incident data for backtesting.

### 2.2 Nepal DHM (Department of Hydrology and Meteorology)
- **Org:** Government of Nepal, hydromet authority.
- **Data:** "River Watch" and "Flood Monitoring" pages (server-rendered, human-readable) list real stations directly relevant to our corridor: **Bhote Koshi at Bahrabise, Bhote Koshi at Shyaprubesi, Bhotekoshi at Rasuwagadi, Trishuli Khola at Dhunche, Trishuli River at Betrawati/Bhorle/Galchi** — with water level, warning level, danger level, and rising/falling trend.
- **`realtime-stream` endpoint:** the page most likely to hold a machine feed **returned HTTP 500 on every attempt** during this research — unverified/possibly non-functional, not confirmed working.
- **API:** **No documented public REST/JSON API found.** No third-party project found consuming one programmatically; one GitHub repo found processes files **manually obtained** from DHM, confirming even researchers get data as files/requests, not live API calls.
- **Historical/bulk data:** Via a manual **Data Request Service** (name, purpose, ToS acceptance) — not self-service.
- **Real-world performance (Aug 2026 event):** The four gauges most relevant to this corridor (**Rasuwa Bhote Koshi, Rasuwa Syabrubesi, Nuwakot Betrawati, Dhading Malekhu**) were **physically destroyed by the flood**, with automated telemetry going offline ~8:40am — at or before the moment it mattered. DHM's Flood Forecasting Division was alerted by a phone call at ~9:00am; SMS alerts went out at 9:16am (~16 min after the flood had already crossed the border, and driven by human observation, not sensor thresholds).
- **Bottom line:** No API; the one candidate live-data page is broken; and the real disaster shows the physical gauge network on this exact corridor does not survive the hazard it exists to detect. Useful as a manually-curated reference for station names/thresholds, not as a dependable low-latency feed.

### 2.3 ICIMOD — real-time monitoring specifically
No evidence ICIMOD runs real-time monitoring/alerting for this corridor. Its Aug 2026 press release describes most Trishuli-basin stations as damaged/washed away, with only one (Glacchi) still operating, and frames community-to-community informal relay as aspirational, not an existing system. ICIMOD's real value is its glacier/glacial-lake/GLOF inventories (see §3.4) and, notably, its scientists were quoted supplying the technical failure elevation (~5,200 m, ~1,200 m fall into the Lhende) for the Aug 2026 event to journalists — i.e., active bespoke analysis capacity worth reaching out to directly, even without a public API.

### 2.4 Post-disaster response (both events)
No new sensors, gauges, or automated warning technology had been installed on this corridor as of the latest reporting found (through ~Aug 31, 2026), despite this being the *second* flood on the same tributary in 14 months. What changed was political/oversight process (a parliamentary cross-party mechanism, calls for study of Nepal's remaining ~47 high-risk glacial lakes), not hardware. A prior bilateral Nepal-China data-sharing ask (raised after the July 2025 event) remained unfulfilled at the time of the August 2026 event — China provided no advance warning despite the earlier agreement.

### 2.5 Bottom line — Nepal government sources
**Not currently usable as free, low-latency, programmatic inputs.** This is a real finding, not a research gap: no public API for BIPAD or DHM was found; the one DHM page that might be a live feed is broken; and the actual disaster shows the ground station network on this corridor is destroyed by the hazard at the moment it matters, with the real warning chain running on a phone call. Plan the system around minimal, best-effort, likely-manual integration with Nepali government data, not a dependable backbone.

---

## 3. Satellite, rainfall, and geographic data

### 3.1 NASA GPM / IMERG (rainfall)
- **Resolution:** 0.1° × 0.1° (~10 km), 30-min cadence.
- **Latency (verified from NASA's own FAQ):** **Early Run ~4 hours**, **Late Run ~12–14 hours**, **Final Run ~3.5 months** (gauge-corrected).
- **Archive:** Back to June 2000 (TRMM-era record to 1998).
- **Access:** GES DISC (OPeNDAP), Giovanni, `earthaccess` Python client. **Auth:** free NASA Earthdata Login. **Cost:** free, public domain.
- **Usefulness:** Only the **Early Run (4 hr)** is fast enough to be a near-real-time contextual input ("heavy upstream rain in the last N hours" as one factor in a composite score). It cannot detect the GLOF/collapse mechanism itself — rainfall is a risk factor, not the event. 10 km grid is coarse for a narrow Himalayan valley.
- **Category: contextual input only, not an emergency trigger.**

### 3.2 NASA Landsat (8/9 + archive to 1972)
- **Resolution:** 30 m (15 m pan-sharpened). **Revisit:** 16-day per satellite, ~8-day combined L8+L9 (mission spec).
- **Latency:** Not NRT-designed; general USGS guidance suggests ~24 hours to a few days for Collection 2 products — not independently pinned down to an exact number here.
- **Cloud limitation (confirmed via the July 2025 Purepu case):** the Stimson Center's own analysis explicitly cites "extended cloud cover during monsoon season restricts optical satellite utility" — usable clear-sky images can be **weeks apart** during exactly the June–September window when GLOF risk peaks.
- **Access:** EarthExplorer, M2M API, AWS/STAC. **Auth:** free USGS/EROS account. **Cost:** free since 2008.
- **Category: baseline/long-term change-detection only** (multi-year glacial lake growth trends) — not for emergency detection.

### 3.3 ESA Copernicus Sentinel-1 (SAR) / Sentinel-2 (optical)
- **Sentinel-1:** ~5×20 m (IW mode). Revisit degraded after S1B's 2021 failure; with S1C now flying (since May 2025), likely ~3–12 days over our region (not pinned to an exact number for Nepal specifically). Standard GRD products: "within 24 hours in practice, often a few hours"; true <3hr NRT tiers exist but are generally reserved for designated monitoring programs, not guaranteed for an arbitrary AOI. **Cloud-penetrating** — the best free option for the monsoon-affected Himalaya — but steep terrain causes layover/shadow that can blind SAR on certain valley walls (confirmed limitation noted in the Purepu case study).
- **Sentinel-2:** 10 m visible/NIR. Revisit ~5 days (2-satellite constellation). Standard L2A latency ~24 hours.
- **Access:** Copernicus Data Space Ecosystem (OData, S3, STAC, openEO). **Auth:** free registration. **Cost:** free (General User Account tier).
- **Real-world validation (July 2025 Purepu event):** The Stimson Center/HiRISK team, doing dedicated post-event analysis, found public Sentinel-1 required "several additional days" in practice and had to fall back on **tasked, paid commercial SAR (ICEYE)** for time-critical confirmation — delivered within 24 hours of a specific request. Even a well-resourced forensic team took **36+ hours** to formally confirm the source and mechanism, well after the disaster.
- **Category: best free cloud-penetrating option, but hours-to-days — not a minutes-scale trigger; use for after-the-fact confirmation and periodic lake-area monitoring.**

### 3.4 ICIMOD datasets (static/periodic — not live)
- **GLOF database of High Mountain Asia:** 766 documented events, 1533–2025, updated Dec 2025, CC BY 4.0, free download via [rds.icimod.org](https://rds.icimod.org/). No confirmed public API — download/catalog interface only.
- National glacier/glacial-lake/GLOF inventories for Nepal specifically.
- No confirmed listing of Purepu/Lende Khola lakes specifically in the structured DB as of this research, though ICIMOD scientists are actively doing bespoke analysis on this exact corridor (see §2.3) — worth direct outreach (`rds@icimod.org`) for corridor-specific data.
- **Category: baseline/historical inventory only.**

### 3.5 Other free geographic reference data
- **OpenStreetMap** (rivers, settlements, roads, bridges): free via HOTOSM/HDX exports or Overpass API, ODbL license. Coverage in remote Himalayan valleys like Lende Khola may be sparse — spot-check before relying on it for bridge/settlement inventories there.
- **Copernicus DEM (GLO-30, 30 m):** free, modern, more accurate than SRTM for slope/flow-routing — prefer this over the older Nepal-specific SRTM 90 m DEM (static, last updated 2019).

### 3.6 Bottom line — satellite/rainfall
**No, satellite data cannot serve as a minutes-scale detection trigger, confirmed directly by this corridor's own disaster, not by inference.** The Aug 2026 flood reached Betrabati in **~40 minutes** from the source event (vs. a historical baseline of ~14 hours for comparable floods). Every satellite source researched has a latency floor of hours (IMERG Early Run ~4h, Sentinel-1 GRD in practice a few hours to 24+h) to days/weeks/months for everything else. The July 2025 event shows even dedicated forensic analysis by a well-resourced team took 36+ hours. **Emergency-scale detection, if it is possible at all with free data, has to come from seismic and/or (currently nonexistent-for-this-corridor) ground sensors — not satellites.** Satellite/rainfall data's real value here is: (1) contextual risk-scoring input (IMERG Early Run, Sentinel-1 change detection), (2) baseline hazard mapping and known-lake inventories (ICIMOD, Landsat/Sentinel-2 trend series), and (3) historical backtesting reconstruction and after-the-fact confirmation.

---

## 4. Consolidated verdict for the detection architecture

| Source | Speed | Free? | Auth | Verdict |
|---|---|---|---|---|
| USGS earthquake catalog | Minutes (typing may lag ~2 days) | Yes | None | Fast flag, unreliable auto-classification for non-tectonic events |
| FDSN/EarthScope waveforms | Real-time capable via SeedLink | Yes | None (open) | Physically sound, but no confirmed near-source station in this corridor |
| Nepal BIPAD | Unknown, no API | Unclear | No API found | Not usable programmatically today |
| Nepal DHM river gauges | Unknown; live page broken; gauges destroyed by the actual hazard | Unclear | No API found | Not usable programmatically today; physically vulnerable to the exact hazard |
| NASA GPM/IMERG Early Run | ~4 hours | Yes | Earthdata login | Contextual input only |
| NASA Landsat | Days–weeks (cloud-limited) | Yes | USGS/EROS login | Baseline only |
| ESA Sentinel-1 SAR | Hours–days in practice | Yes | Free CDSE account | Best free cloud-penetrating option, still not minutes-scale |
| ESA Sentinel-2 optical | ~24h+ (cloud-limited) | Yes | Free CDSE account | Baseline / confirmation |
| ICIMOD GLOF database/inventories | Static | Yes (CC BY 4.0) | None | Historical backtesting + hazard-zone reference |
| OpenStreetMap | Static | Yes | None | Terrain/exposure reference (verify remote-valley coverage) |
| Copernicus DEM | Static | Yes | Free CDSE account | Terrain/slope reference |

**Implication the user should hear plainly, per the project's own "be critical with me" instruction:** given everything above, this project's most defensible immediate goal is not "detect the flood in time to warn people 30 minutes downstream" — no currently-accessible free data source has been shown to support that for this specific corridor. The more defensible goal for an MVP is: (1) build the backtesting/replay engine to rigorously establish how much warning time, if any, is achievable with each candidate data source and combination, using the two real 2025/2026 events as ground truth, and (2) be honest in the evaluation screen if the answer turns out to be "not much, with today's free data." That finding — even if disappointing — is itself the valuable output Phase 0–7 are designed to produce, and it would also be a legitimate, fact-based case for advocating specific cheap infrastructure (near-source Raspberry-Shake-class seismometers, in particular) as the real unlock.

---

## Sources

All source URLs are preserved in the research notes; key ones reused across sections:
- USGS: https://earthquake.usgs.gov/earthquakes/feed/ , https://earthquake.usgs.gov/fdsnws/event/1/ , https://earthquake.usgs.gov/programs/landslide-hazards/science/2026-nepal-debris-avalanche-and-flash-flood
- EarthScope/FDSN: https://service.iris.edu/fdsnws/dataselect/docs/1/help/ , https://www.earthscope.org/geophysical-event/nepal-glacial-collapse-and-debris-flow-us7000tbwb/
- Cook et al. 2020, Science Advances: https://www.science.org/doi/10.1126/sciadv.aba3645
- Kumar et al. 2023: https://link.springer.com/article/10.1007/s41748-023-00364-y
- Shukla et al. 2022, Scientific Reports: https://www.nature.com/articles/s41598-022-07491-y
- BIPAD Portal: https://bipadportal.gov.np/
- DHM: https://dhm.gov.np/hydrology/river-watch , https://dhm.gov.np/hydrology/realtime-stream (HTTP 500 at time of research)
- Nepalnews / Kathmandu Post / Spotlight Nepal reporting on Aug 2026 warning-chain failure (full citations in historical-incidents.md)
- NASA GPM FAQ: https://gpm.nasa.gov/data/faq
- Copernicus Data Space Ecosystem: https://dataspace.copernicus.eu/
- Stimson Center, Purepu Glacier report: https://www.stimson.org/2025/investigating-an-emerging-climate-hazard-transboundary-glacial-floods-on-the-china-nepal-border/
- ICIMOD GLOF database: https://rds.icimod.org/metadata/8881454b-6f7c-461b-95c2-eaf7618230d9
- ICIMOD Kyirong-Rasuwa Flood press release: https://www.icimod.org/kyirong-rasuwa-flood-2026-nepal-china-border/

Full source lists with every URL are preserved in `historical-incidents.md` (events) and can be re-supplied on request for any individual source above.