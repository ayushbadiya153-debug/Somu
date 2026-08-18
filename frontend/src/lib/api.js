import axios from "axios";

const BACKEND_URL = process.env.REACT_APP_BACKEND_URL;
export const API_BASE = `${BACKEND_URL}/api`;

export const api = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

export const authApi = {
  session: (sessionId) =>
    api.post("/auth/session", null, { headers: { "X-Session-ID": sessionId } }).then((r) => r.data),
  me: () => api.get("/auth/me").then((r) => r.data),
  logout: () => api.post("/auth/logout").then((r) => r.data),
};

export const dashboardApi = {
  get: () => api.get("/dashboard").then((r) => r.data),
};

export const settingsApi = {
  get: () => api.get("/settings").then((r) => r.data),
  put: (data) => api.put("/settings", data).then((r) => r.data),
  getBroker: () => api.get("/settings/broker").then((r) => r.data),
  putBroker: (data) => api.put("/settings/broker", data).then((r) => r.data),
  testBroker: () => api.post("/settings/broker/test").then((r) => r.data),
  deleteBroker: () => api.delete("/settings/broker").then((r) => r.data),
};

export const productsApi = {
  list: (contract_types) => api.get("/products", { params: { contract_types } }).then((r) => r.data),
  top: (limit = 15) => api.get("/products/top", { params: { limit } }).then((r) => r.data),
};

export const tradingApi = {
  place: (payload) => api.post("/orders", payload).then((r) => r.data),
  openOrders: () => api.get("/orders/open").then((r) => r.data),
  positions: () => api.get("/positions").then((r) => r.data),
  wallet: () => api.get("/wallet").then((r) => r.data),
};

export const historyApi = {
  local: () => api.get("/history").then((r) => r.data),
  exchange: () => api.get("/history/exchange").then((r) => r.data),
};

export const strategiesApi = {
  list: () => api.get("/strategies").then((r) => r.data),
  toggle: (key, enabled) => api.post(`/strategies/${key}/toggle`, { enabled }).then((r) => r.data),
  updateParams: (key, params) => api.put(`/strategies/${key}/params`, params).then((r) => r.data),
};

export const engineApi = {
  get: () => api.get("/engine").then((r) => r.data),
  start: () => api.post("/engine/start").then((r) => r.data),
  stop: () => api.post("/engine/stop").then((r) => r.data),
};

export const logsApi = {
  list: () => api.get("/logs").then((r) => r.data),
};
