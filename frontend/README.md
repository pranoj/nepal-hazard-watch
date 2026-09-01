# Nepal Hazard Watch Frontend

React + TypeScript + Vite frontend for the Nepal Hazard Watch Phase 1 dashboard.

## Prerequisites

- Node.js 20+
- npm 10+

## Local setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Copy the example environment file and adjust the backend URL if needed:
   ```bash
   cp .env.example .env
   ```

3. Start the development server:
   ```bash
   npm run dev
   ```

   The Vite app runs on: http://localhost:5173

4. Build for production:
   ```bash
   npm run build
   ```

5. Preview the production build:
   ```bash
   npm run preview
   ```

## Environment variables

- `VITE_API_BASE_URL` — base URL for the backend API service.

## Project structure

- `src/components` — reusable UI components
- `src/pages` — page-level screens
- `src/api` — API client helpers and service calls
- `src/maps` — map and geospatial utilities
- `src/events` — event sourcing and event handling logic
- `src/replay` — replay and historical playback logic
- `src/evaluation` — evaluation and scoring logic
- `src/types` — shared TypeScript types
