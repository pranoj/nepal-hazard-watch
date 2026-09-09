# Nepal Hazard Watch

A dashboard that tracks glacial lake outburst flood (GLOF) risk across Nepal. Live at [nepalhazardwatch.com](https://www.nepalhazardwatch.com).

This is a personal/educational project, not an official warning system. For real emergencies, use Nepal's official disaster authorities.

## What it does

It watches 97 glacial lakes and 123 glacier points across Nepal and scores each one from 0 to 100 based on six things: recent earthquakes nearby, rainfall, local terrain slope, seasonal timing (monsoon), satellite-tracked lake growth, and satellite-tracked glacier ice-cover drops. The score gets recalculated every 30 minutes, and a few of the inputs (USGS earthquakes, weather) refresh even more often than that.

Full breakdown of the formula and where every number comes from is on the [methodology page](https://www.nepalhazardwatch.com/methodology.html).

## Stack

Backend is Java 17 / Spring Boot, PostgreSQL, Flyway for migrations. Frontend is React + TypeScript + Vite, Leaflet for the map.

Data comes from USGS (earthquakes), OpenWeatherMap (weather), ICIMOD's glacial lake database, the Randolph Glacier Inventory, and Copernicus Sentinel-2 imagery via Sentinel Hub for satellite tracking.

## Running it locally

You need Java 17, Maven, Node 20+, and a Postgres database.

Backend:
```
cd backend
mvn spring-boot:run
```
You'll also need a `src/main/resources/application-local.yml` (gitignored) with your local db password, weather API key, and Sentinel Hub credentials. See `application.yml` for which properties it expects.

Frontend:
```
cd frontend
npm install
cp .env.example .env.local
npm run dev
```

The frontend expects the backend at `http://localhost:8080/api` by default, that's already set in `.env.example`.

## Deployment

Backend runs as a Docker container on AWS ECS Fargate, behind an Application Load Balancer for HTTPS. Frontend is on AWS Amplify Hosting, which rebuilds automatically on every push to main. Database is Postgres on RDS.

## Project layout

```
backend/   Spring Boot API, scheduled jobs, risk scoring logic
frontend/  React dashboard
```
