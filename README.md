# NexusTrade — Delta India Automated Trading Dashboard

A clean, modular full-stack real trading dashboard for **Delta Exchange India** (perpetual futures + options).

- Backend: FastAPI + MongoDB
- Frontend: React 19 + Tailwind + shadcn/ui + Lucide
- Auth: Emergent-managed Google OAuth (7-day httpOnly session cookie)
- Broker: Delta Exchange India REST v2, HMAC-SHA256 signed per-user
- Secrets: AES-GCM encryption at rest for API secrets

## Features
- Dashboard with virtual balance, cash, realized/unrealized P&L, trade count, open positions
- Real Buy/Sell ticket (market/limit, reduce-only) — routes to Delta India
- Trade history (local ledger) + exchange order history endpoint
- Strategies list with enable/disable toggles (execution logic is deferred, you plug it in later)
- Engine Start/Stop with live status badge
- Activity log panel
- Settings for virtual capital, default symbol, timeframe, and encrypted Delta credentials

## Local dev

Backend:
```
cd backend
# create backend/.env
MONGO_URL="mongodb://localhost:27017"
DB_NAME="nexustrade"
CORS_ORIGINS="http://localhost:3000"
ENCRYPTION_KEY="<any random 32+ char string>"
DELTA_BASE_URL="https://api.india.delta.exchange"

pip install -r requirements.txt
uvicorn server:app --host 0.0.0.0 --port 8001 --reload
```

Frontend:
```
cd frontend
# frontend/.env
REACT_APP_BACKEND_URL=http://localhost:8001

yarn install
yarn start
```

Then open http://localhost:3000, sign in with Google, go to **Settings** and paste your Delta India API key + secret.

## Production notes
- The frontend never sees Delta credentials. All signed calls happen server-side.
- Session cookie: httpOnly, secure, SameSite=None. Requires HTTPS.
- Rotate `ENCRYPTION_KEY` only when re-encrypting stored secrets (or reset broker connection).
- All routes prefixed with `/api` — safe behind ingress.

## API reference (selected)
- `POST /api/auth/session` — exchange Emergent `X-Session-ID` for our session cookie
- `GET  /api/auth/me` — current user
- `POST /api/auth/logout` — invalidate session
- `GET  /api/dashboard` — aggregated metrics + open positions
- `GET  /api/products?contract_types=perpetual_futures,call_options,put_options`
- `POST /api/orders` — place a real order on Delta India
- `GET  /api/positions` — live Delta positions
- `GET  /api/wallet` — Delta wallet balances
- `GET  /api/history` — local persisted trades
- `GET  /api/history/exchange` — Delta order history
- `GET  /api/strategies` · `POST /api/strategies/{key}/toggle`
- `GET  /api/engine` · `POST /api/engine/start` · `POST /api/engine/stop`
- `GET  /api/logs`
- `GET/PUT /api/settings` · `GET/PUT/DELETE /api/settings/broker` · `POST /api/settings/broker/test`

## Roadmap
- Wire your custom strategy signal generator into `/engine` (loop skeleton is in place)
- Trade reconciliation against Delta fills for precise realized P&L
- Optional websocket price stream for live position marks
