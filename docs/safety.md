# Nepal Hazard Watch — Safety Rules (Phase 0)

This document is the standing safety contract for the project. It does not expire when Phase 0 ends — every later phase must be checked against it.

## 1. What this system is, and is not, right now

Nepal Hazard Watch is, during development and for the foreseeable phases ahead, an **experimental research system**. It is:
- NOT an official Nepal emergency-warning service.
- NOT affiliated with the Government of Nepal, NDRRMA, DHM, or any Nepali authority, unless and until such a partnership is explicitly formed.
- NOT something that tells real people they must evacuate.
- NOT something whose outputs should be described as saving lives, until real-world validation exists to support that claim.

## 2. Hard rules for every phase, including the MVP

- Do not send public evacuation warnings.
- Do not send public SMS emergency alerts.
- Do not claim affiliation with Nepal's government.
- Do not tell real people they must evacuate.
- Do not claim the system saves lives.
- Do not let an LLM independently declare an emergency or make the evacuate/don't-evacuate call.
- Every generated alert, at every phase until real-world deployment is explicitly approved, must be clearly labeled: **SIMULATED / EXPERIMENTAL ALERT**.
- Alerts are visible only to developers/researchers until extensive validation (historical replay + a meaningful period of live shadow mode, per Phase 10) has occurred — and even then, real-world warning deployment should happen only in partnership with Nepali authorities, scientists, and disaster-management organizations, not unilaterally.

## 3. Why false-alarm reduction is a top priority, not a nice-to-have

If people repeatedly receive warnings that turn out to be nothing, they learn to ignore warnings — including the one that matters. This is why the risk-level design (NORMAL / WATCH / DANGER / EXTREME) deliberately allows WATCH to be sensitive while requiring EXTREME to demand much stronger, ideally multi-signal, corroborated evidence. Phase 0 research is directly relevant here: given how sparse and delayed the currently-available free data sources are for this specific corridor (see `data-sources.md`), achieving genuinely independent multi-signal corroboration for EXTREME may be structurally difficult — this must be tested honestly (see `architecture.md` §I, risk 6), not assumed solved by picking thresholds.

## 4. Missing data means UNKNOWN, never SAFE

This is a direct, evidence-backed finding from Phase 0, not a hypothetical: the four DHM gauging stations most relevant to the target corridor were physically destroyed by the actual August 26, 2026 flood, with automated telemetry going offline at or before the moment detection mattered. A system that treats "no data from this source" as "conditions are normal" would have been silently blind at exactly the moment it needed to work. Every data-source status must default to `UNKNOWN`, and the system must visibly report **DATA CONFIDENCE REDUCED** when a source that normally reports goes silent — never silence.

## 5. No look-ahead bias, ever

Every observation carries `observedAt` (when the phenomenon occurred) and `receivedAt` (when the system could realistically have known about it). Historical replay may only expose observations where `receivedAt <= simulatedCurrentTime`. This applies uniformly to satellite imagery, seismic data, rainfall products, government alerts, river gauges, and news/official reports. Violating this produces a system that looks accurate in backtesting and is not — the single easiest way to fool ourselves, and the master brief is explicit that this is "absolutely critical."

## 6. Honest reporting, including when the answer is discouraging

Phase 0's own findings already surface an uncomfortable possibility worth stating plainly now: no free, currently-accessible data source researched in this phase has been shown to support reliable, minutes-scale detection for this specific corridor (see `data-sources.md` §4 and `architecture.md` §I, risk 1). If historical replay and holdout testing confirm that finding, the correct response is to report it — not to keep adjusting thresholds until the numbers look better. The master brief is explicit: "if results are poor, report them. Never manipulate thresholds afterward just to make performance look better." The same standard applies to AI/ML introduced in later phases — kept only if it measurably improves detection, warning time, or false-positive rate; removed if it doesn't.

## 7. Never fabricate

Never fabricate API availability, latency, satellite/river/seismic data, historical times or coordinates, fatality figures, scientific conclusions, or detection performance. Where Phase 0 could not verify something, it is marked UNKNOWN in `data-sources.md` and `historical-incidents.md` rather than guessed — including, notably, the exact casualty and magnitude figures for both the July 2025 and August 2026 events, which remain disputed/evolving across sources and must be stored as as-of-date snapshots, never a single hardcoded "true" number.

## 8. What Phase 0 changes about the stated goal

The original project description mentions "live data from satellite images" as an expectation. Phase 0 research directly contradicts treating any currently free satellite product as live in the sub-hour, emergency-relevant sense — the fastest option found (NASA IMERG Early Run) has a ~4-hour latency floor, and the corridor's own real disaster shows the flood reaching the first downstream community in ~10-40 minutes. This isn't a reason to abandon the project; it's the kind of finding the master brief's Phase 0 exists to surface honestly before more code gets built on top of an unverified assumption. See `data-sources.md` §4 for the full reasoning and a recommended reframing of the MVP's immediate goal around rigorous backtesting rather than an assumed detection capability.