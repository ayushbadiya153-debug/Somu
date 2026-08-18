# NexusTrade — Product Requirements Document

## Original problem statement
Build a clean, professional responsive real trading dashboard for automated trading strategies. Requirements include: login/signup, dashboard with virtual account balance, virtual cash, open real positions, unrealized/realized P&L, total number of trades, real Buy/Sell using user API/secret keys, trade history, strategy section with enable/disable, engine Start/Stop, activity log panel, settings for virtual capital/symbol/timeframe/strategy params. Modular, env-based config, portable to any hosting provider. User will supply strategy logic later.

## Confirmed decisions (2026-02)
- Auth: Emergent-managed Google login
- Broker: Delta Exchange India (v2 REST) — perpetual futures + options (ETHUSD, XAUUSD, BTCUSD, etc.)
- Trading mode: real only (no paper/virtual manual trades)
- No per-order confirmation dialog; UI shows a bold "Live Real Trading" badge instead
- Secrets: encrypted at rest (AES-GCM) with server-side key

## User personas
- **Systematic trader** who wants a shell UI for a Delta India automation strategy they will plug in.

## Core (static) requirements
- Google OAuth login/logout, 7-day cookie session
- Dashboard KPIs (virtual balance, cash, realized/unrealized P&L, trade count, open positions)
- Delta India: order placement (market/limit, reduce-only), positions, wallet, order history
- Local trade ledger persisted in MongoDB
- Strategy toggles (logic pending)
- Engine Start/Stop
- Activity log
- Settings: virtual capital, symbol, timeframe, broker credentials

## Architecture
- FastAPI (backend) modularised into `auth.py`, `db.py`, `security.py`, `delta_client.py`, `models.py`, and `routers/{settings,products,trading,history,dashboard,strategies,engine,logs}.py`
- React frontend with pages under `/pages` and shared `Layout` + `ProtectedRoute`
- MongoDB collections: `users`, `user_sessions`, `broker_connections`, `user_settings`, `trades`, `strategies`, `engine_states`, `activity_logs`

## What's been implemented (2026-02-XX)
- Full auth flow (Emergent Google) with session cookie + Bearer fallback
- Dashboard aggregation combining virtual account + real Delta positions/wallet
- Order placement + local trade record + activity log
- Broker connection management (save/test/remove) with encryption
- Strategy list with toggles (built-in placeholders)
- Engine start/stop + status
- Activity log panel with polling
- Settings persistence
- README, test_credentials, auth_testing docs

## Prioritized backlog
- **P0 (next):** Plug user-supplied strategy logic into engine loop
- **P1:** Trade reconciliation against Delta fills → accurate realized P&L
- **P1:** Live position mark refresh via websocket
- **P2:** Multi-account/sub-account support
- **P2:** Alerts + Telegram/email hooks on order events
