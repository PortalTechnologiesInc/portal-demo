# Portal Demo

A full-stack reference implementation showing how to integrate Portal into a web application — authentication, payments, subscriptions, Cashu tokens, and profile management.

**Live demo:** [demo.getportal.cc](https://demo.getportal.cc/) · **Stack:** Kotlin + Javalin backend · Svelte frontend · [Portal Java SDK](https://github.com/PortalTechnologiesInc/java-sdk)

---

## What it demonstrates

- **Authentication** — Nostr key handshake flow: generate a QR code, user scans with Portal app, session established
- **Single payments** — request a one-time Lightning payment in sats or fiat
- **Subscriptions** — request and manage recurring payments
- **Cashu** — mint, send, and burn ecash tokens
- **Profiles** — fetch and display Nostr profiles

---

## Prerequisites

- Docker and Docker Compose
- [Portal SDK Daemon](https://github.com/PortalTechnologiesInc/lib) (`getportal/sdk-daemon`) running and reachable
- [Portal app](https://getportal.cc) on your phone

---

## Running with Docker

**Option A — sdk-daemon on the same host (recommended):**

```bash
docker compose --profile host up
```

Portal demo at `http://localhost:7070`. Connects to sdk-daemon at `ws://localhost:7000/ws`.

**Option B — sdk-daemon reachable via network:**

```bash
docker compose up
```

sdk-daemon must listen on `0.0.0.0:7000` (not `127.0.0.1`) so the container can reach it via `host.docker.internal`.

---

## Environment variables

| Variable | Description | Default |
|----------|-------------|---------|
| `REST_WS_ENDPOINT` | WebSocket URL of the running sdk-daemon | `ws://host.docker.internal:7000/ws` |
| `REST_TOKEN` | Auth token (must match `PORTAL__AUTH__AUTH_TOKEN` on sdk-daemon) | `my-static-token` |
| `DB_PATH` | Directory for the local SQLite database | `/home/user/portal/demo` |

Edit the values in `docker-compose.yml` or pass them via environment before running.

---

## Running sdk-daemon

If you don't have sdk-daemon running yet:

```bash
docker run -d -p 7000:3000 \
  -e PORTAL__AUTH__AUTH_TOKEN=my-static-token \
  -e PORTAL__NOSTR__PRIVATE_KEY=your-nostr-private-key-hex \
  getportal/sdk-daemon:0.3.0
```

> Generate a Nostr private key with `nak generate` or any Nostr tool.

---

## Running from source

```bash
# Backend (requires JDK 17+, Maven)
mvn package -DskipTests
java -jar target/portal-demo-1.0-SNAPSHOT.jar

# Frontend (in a separate terminal)
cd frontend
npm install
npm run dev
```

Set `REST_WS_ENDPOINT`, `REST_TOKEN`, and `DB_PATH` as environment variables before starting the backend.

---

## Related

- [Portal lib & SDK Daemon](https://github.com/PortalTechnologiesInc/lib) — source for `getportal/sdk-daemon` and the TypeScript SDK
- [Java SDK](https://github.com/PortalTechnologiesInc/java-sdk) — used by this demo
- [Documentation](https://portaltechnologiesinc.github.io/lib/)
