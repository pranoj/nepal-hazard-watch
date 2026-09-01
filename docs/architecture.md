
## G. Local setup — exact commands

Prerequisites: Java 21+, Node 20+, Docker, Git.

```bash
# 1. Repo scaffold
mkdir nepal-hazard-watch && cd nepal-hazard-watch
git init

# 2. Backend (Spring Boot via Spring Initializr CLI, or start.spring.io in a browser)
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.3.0 \
  -d javaVersion=21 \
  -d groupId=watch.nepalhazard \
  -d artifactId=backend \
  -d name=backend \
  -d packageName=watch.nepalhazard \
  -d dependencies=web,data-jpa,validation,actuator,postgresql,flyway \
  -o backend.zip
unzip backend.zip -d backend && rm backend.zip

# 3. Frontend (Vite + React + TypeScript)
npm create vite@latest frontend -- --template react-ts
cd frontend && npm install && cd ..

# 4. Local PostgreSQL via Docker Compose
cat > docker-compose.yml <<'EOF'
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: nepal_hazard_watch
      POSTGRES_USER: nhw
      POSTGRES_PASSWORD: nhw_local_dev
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
volumes:
  pgdata:
EOF
docker compose up -d

# 5. Configure backend/src/main/resources/application.yml
#    spring.datasource.url=jdbc:postgresql://localhost:5432/nepal_hazard_watch
#    spring.datasource.username=nhw
#    spring.datasource.password=nhw_local_dev
#    spring.flyway.enabled=true

# 6. Run Flyway migrations (auto-runs on Spring Boot startup with flyway on the classpath,
#    or explicitly via the Flyway CLI/Maven plugin):
cd backend && ./mvnw flyway:migrate && cd ..

# 7. Start the backend
cd backend && ./mvnw spring-boot:run
#    -> http://localhost:8080/actuator/health

# 8. Start the frontend (separate terminal)
cd frontend && npm run dev
#    -> http://localhost:5173

# 9. Run tests
cd backend && ./mvnw test
cd frontend && npm test
```

## H. Cost table — target $0/month

Research confirmed free-tier terms as of 2026-09-01; several providers changed policy in the last 1-2 years, so these were checked live rather than assumed.

| Component | Service | Free limit | Expected MVP usage | Expected cost |
|---|---|---|---|---|
| Database | **Neon** (Postgres) | 0.5 GB storage, 100 CU-hrs/mo, autosuspend after 5 min idle (no deletion risk) | Small research dataset, sporadic writes | $0 |
| Backend hosting | **Google Cloud Run** | ~240,000 vCPU-sec + 450,000 GiB-sec/mo (resets monthly); scales to zero | Low-traffic REST API for a handful of researchers | $0 (requires card on file, not charged inside free quota) |
| Background collector | **GitHub Actions** scheduled workflow | Unlimited minutes on a public repo (2,000 min/mo if private) | Periodic polling of USGS/NASA/Sentinel APIs, writes to Neon | $0 (auto-disables after 60 days of repo inactivity — mitigate by keeping the repo active) |
| Frontend hosting | **GitHub Pages** (or Cloudflare Pages) | ~100 GB/mo bandwidth (informal), free subdomain | Small React/Vite static build | $0 |
| Source control / CI | **GitHub** | Free public/private repos; free Actions minutes as above | Team of a few devs | $0 |
| Domain | Free subdomains (`*.run.app`, `*.github.io`, `*.pages.dev`) | N/A | No custom domain needed for MVP | $0 |
| Seismic data | USGS / FDSN / EarthScope | Free, no tier | Polling public endpoints | $0 |
| Rainfall data | NASA GPM/IMERG | Free (Earthdata login) | Periodic pulls | $0 |
| Satellite data | ESA Copernicus Sentinel-1/2 | Free (CDSE account) | Periodic pulls for baseline/confirmation | $0 |
| Glacier/GLOF inventory | ICIMOD RDS | Free (CC BY 4.0) | One-time/occasional bulk download | $0 |

**Fallback combo** (avoids any card-on-file requirement): one **Oracle Cloud "Always Free"** ARM VM (2 OCPU/12 GB as of an Aug 2026 tier change) self-hosting Spring Boot + Postgres + a cron/systemd-timer collector, all always-on — trades managed-platform convenience for full ops responsibility, at genuinely $0 with no card requirement beyond identity verification at signup.

**Before introducing any paid service, per the master brief's rule:** none was found necessary for MVP v0.1. The two watch-items are (1) Neon's 0.5 GB/100 CU-hr ceiling if data volume grows past prototype scale, and (2) GitHub's 60-day scheduled-workflow auto-disable if the repo goes quiet — both are free to monitor for, not reasons to pay yet.

## I. Risks

Ranked by how directly they threaten the project's actual research question (can free data give useful, trustworthy warning time), not by generic engineering severity.

1. **No currently-accessible free data source has been shown to support minutes-scale detection for this corridor.** This is the single biggest risk to the project's premise, established directly from Phase 0 research, not assumed: the real Aug 26, 2026 flood reached Betrabati in ~40 minutes, all researched satellite products have latency floors of hours to months, and the one seismic signal that fired was auto-misclassified as a tectonic earthquake before being manually corrected ~2 days later. The MVP's job (per the master brief) is to measure this rigorously via backtesting, not to assume it will work — but the team should go in expecting a real chance the honest answer is "not currently enough warning time with free data," and treat that as a valid, useful finding rather than a failure to fix by adding more code.

2. **No dense, near-source seismic instrumentation exists in this exact corridor.** Every published Himalayan case that delivered useful lead time (Chamoli) used stations 12–20 km from the source; nothing confirmed that close exists for Rasuwa/Langtang. This is an infrastructure gap, not a software problem — it may be the project's most concrete, evidence-backed recommendation for physical intervention (Phase 11 territory), independent of what the MVP software itself can do.

3. **Cross-border data gap.** The trigger mechanism for both real events originates in Tibet/China. ICIMOD explicitly noted the Aug 2026 event fell outside the reach of China's own early-warning system, and a bilateral data-sharing ask from after the July 2025 event went unfulfilled by the time of the second disaster. No amount of software on the Nepal side compensates for zero upstream sensor access.

4. **The Nepali ground-sensor network that does exist is destroyed by the exact hazard it's meant to detect.** Confirmed directly: the four DHM gauges most relevant to this corridor went offline ~8:40am on Aug 26, 2026, at or before the moment detection mattered. Any design that assumes DHM telemetry will be available during a live event is contradicted by the one real test available.

5. **No confirmed public API for Nepal's two most locally-relevant government data sources (BIPAD, DHM).** Both were researched in depth; neither has documented programmatic access. This forces either (a) accepting these as effectively absent inputs (recommended for MVP), or (b) unreliable scraping, which the master brief explicitly discourages ("do not aggressively scrape websites").

6. **False positives vs. false negatives is a genuine tension, not a tuning detail.** Given how sparse and delayed the available signals are, a detector tuned to minimize missed events will likely produce materially more false WATCH/DANGER alerts; the master brief's EXTREME-requires-multiple-independent-signals design is the right mitigation, but with this few independent free signals available for this corridor, achieving "multiple independent signals" for EXTREME may be structurally hard — this should be tested explicitly, not assumed solvable.

7. **Historical data incompleteness undermines statistical confidence.** Only two verified, well-documented positive events exist for this exact watershed (2025, 2026), both Tier A but both with real gaps (no gauge data for either, one unconfirmed seismic claim, one unconfirmed precise impact time). A holdout/train split across only two positive examples is statistically thin — the evaluation should say so honestly rather than implying a robust N.

8. **Satellite latency mismatch with stated project hopes.** The project brief mentions "live data from satellite images" as an initial expectation; Phase 0 research directly contradicts treating any researched satellite product as live in the sub-hour sense. This expectation gap should be corrected explicitly with the user now, not discovered later.

9. **Internet/connectivity failure** in the target corridor itself would sever both data collection (if any local sensors existed) and any future alert delivery — relevant to Phase 49 (long-term multi-channel warning), not the MVP, but worth flagging early since it shapes what "success" can mean even if detection works.

10. **Rate limits and API changes on free tiers.** Every free-tier service researched (Neon, GitHub Actions, Cloud Run, NASA/ESA data platforms) can and does change its terms — this research found several providers had materially changed free-tier policy within the last 1-2 years (Render's Postgres expiry cut from 90 to 30 days, Railway's and Fly.io's free tiers effectively eliminated, Oracle's Always Free spec reduced in Aug 2026). Re-verify before Phase 1 build, and don't hardcode assumptions about specific numeric limits into long-lived documentation without a re-check date.