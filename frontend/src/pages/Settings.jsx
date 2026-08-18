import { useEffect, useState } from "react";
import { CheckCircle2, KeyRound, Plus, Save, ShieldCheck, Sparkles, Trash2, X, XCircle } from "lucide-react";
import { SET } from "@/constants/testIds";
import { settingsApi, productsApi } from "@/lib/api";
import { toast } from "sonner";

export default function Settings() {
  const [settings, setSettings] = useState({ virtual_capital: 100000, default_symbol: "BTCUSD", symbols: ["BTCUSD"], timeframe: "5m", max_notional_usd: 100 });
  const [broker, setBroker] = useState({ configured: false });
  const [apiKey, setApiKey] = useState("");
  const [apiSecret, setApiSecret] = useState("");
  const [newSymbol, setNewSymbol] = useState("");
  const [saving, setSaving] = useState(false);
  const [brokerBusy, setBrokerBusy] = useState(false);
  const [testResult, setTestResult] = useState(null);
  const [topLoading, setTopLoading] = useState(false);
  const [topList, setTopList] = useState([]);
  const [showTop, setShowTop] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const [s, b] = await Promise.all([settingsApi.get(), settingsApi.getBroker()]);
        setSettings({ ...s, symbols: s.symbols?.length ? s.symbols : [s.default_symbol || "BTCUSD"] });
        setBroker(b);
      } catch {
        toast.error("Failed to load settings");
      }
    })();
  }, []);

  const addSymbol = (raw) => {
    const s = String(raw || "").trim().toUpperCase();
    if (!s) return;
    if (settings.symbols.includes(s)) {
      toast.info(`${s} already in list`);
      return;
    }
    setSettings({ ...settings, symbols: [...settings.symbols, s] });
    setNewSymbol("");
  };
  const removeSymbol = (s) => {
    if (settings.symbols.length <= 1) {
      toast.error("Keep at least one symbol");
      return;
    }
    setSettings({ ...settings, symbols: settings.symbols.filter((x) => x !== s) });
  };

  const loadTop = async () => {
    setTopLoading(true);
    setShowTop(true);
    try {
      const d = await productsApi.top(15);
      setTopList(d.top || []);
    } catch {
      toast.error("Failed to load top perpetuals");
    } finally {
      setTopLoading(false);
    }
  };

  const saveSettings = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const updated = await settingsApi.put({
        virtual_capital: Number(settings.virtual_capital),
        symbols: settings.symbols,
        timeframe: settings.timeframe,
        max_notional_usd: Number(settings.max_notional_usd),
      });
      setSettings({ ...updated, symbols: updated.symbols?.length ? updated.symbols : [updated.default_symbol] });
      toast.success("Settings saved");
    } catch {
      toast.error("Save failed");
    } finally {
      setSaving(false);
    }
  };

  const saveBroker = async (e) => {
    e.preventDefault();
    if (!apiKey || !apiSecret) return toast.error("Enter API key and secret");
    setBrokerBusy(true);
    try {
      const b = await settingsApi.putBroker({ api_key: apiKey, api_secret: apiSecret });
      setBroker(b);
      setApiKey("");
      setApiSecret("");
      toast.success("Delta credentials saved");
    } catch (e) {
      toast.error(e.response?.data?.detail || "Save failed");
    } finally {
      setBrokerBusy(false);
    }
  };

  const testBroker = async () => {
    setBrokerBusy(true);
    setTestResult(null);
    try {
      const r = await settingsApi.testBroker();
      setTestResult({ ok: true, data: r });
      toast.success("Delta connection OK");
    } catch (e) {
      const detail = e.response?.data?.detail;
      setTestResult({ ok: false, error: typeof detail === "string" ? detail : JSON.stringify(detail || e.message) });
      toast.error("Delta connection failed");
    } finally {
      setBrokerBusy(false);
    }
  };

  const removeBroker = async () => {
    setBrokerBusy(true);
    try {
      await settingsApi.deleteBroker();
      setBroker({ configured: false });
      setTestResult(null);
      toast.success("Credentials removed");
    } catch {
      toast.error("Remove failed");
    } finally {
      setBrokerBusy(false);
    }
  };

  return (
    <div className="space-y-8 max-w-5xl">
      <div>
        <div className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Configuration</div>
        <h1 className="font-['Barlow_Condensed',sans-serif] text-3xl md:text-4xl font-black uppercase tracking-tight">
          Settings
        </h1>
      </div>

      <section className="border border-neutral-800 bg-[#121212]">
        <div className="px-6 py-4 border-b border-neutral-800 flex items-center gap-2">
          <ShieldCheck className="h-4 w-4 text-[#34C759]" />
          <h2 className="font-['Barlow_Condensed',sans-serif] text-xl font-bold uppercase">Trading defaults</h2>
        </div>
        <form onSubmit={saveSettings} className="p-6 grid gap-6">
          <div>
            <label className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Symbols to trade (perpetual futures)</label>
            <div data-testid="settings-symbols-list" className="mt-2 flex flex-wrap gap-2 min-h-[44px] p-2 bg-black border border-neutral-800">
              {settings.symbols.map((s) => (
                <span
                  key={s}
                  data-testid={`settings-symbol-chip-${s}`}
                  className="inline-flex items-center gap-1.5 pl-3 pr-1 py-1 border border-[#007AFF]/40 bg-[#007AFF]/10 text-[#007AFF] font-mono text-xs tracking-wider"
                >
                  {s}
                  <button
                    type="button"
                    data-testid={`settings-symbol-remove-${s}`}
                    onClick={() => removeSymbol(s)}
                    className="p-0.5 hover:bg-[#007AFF]/20 rounded-sm"
                    aria-label={`Remove ${s}`}
                  >
                    <X className="h-3 w-3" />
                  </button>
                </span>
              ))}
              {settings.symbols.length === 0 && (
                <span className="text-neutral-500 text-xs px-2 py-1">No symbols selected</span>
              )}
            </div>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <input
                data-testid="settings-add-symbol-input"
                value={newSymbol}
                onChange={(e) => setNewSymbol(e.target.value)}
                onKeyDown={(e) => { if (e.key === "Enter") { e.preventDefault(); addSymbol(newSymbol); } }}
                placeholder="e.g. SOLUSD, XRPUSD, XAUTUSD"
                className="h-10 flex-1 min-w-[220px] bg-black border border-neutral-800 px-3 text-sm text-white focus:border-[#007AFF] focus:outline-none font-mono uppercase"
              />
              <button
                type="button"
                data-testid="settings-add-symbol-btn"
                onClick={() => addSymbol(newSymbol)}
                className="h-10 px-3 border border-neutral-800 text-neutral-200 hover:bg-neutral-900 text-sm inline-flex items-center gap-1.5"
              >
                <Plus className="h-4 w-4" /> Add
              </button>
              <button
                type="button"
                data-testid="settings-load-top-btn"
                onClick={loadTop}
                className="h-10 px-3 border border-[#007AFF]/40 text-[#007AFF] hover:bg-[#007AFF]/10 text-sm inline-flex items-center gap-1.5"
              >
                <Sparkles className="h-4 w-4" /> Top by volume
              </button>
            </div>
            {showTop && (
              <div data-testid="settings-top-panel" className="mt-3 border border-neutral-800 bg-black">
                <div className="px-3 py-2 border-b border-neutral-800 text-[11px] font-mono uppercase tracking-widest text-neutral-500 flex items-center justify-between">
                  <span>Top perpetuals · 24h turnover</span>
                  <button type="button" onClick={() => setShowTop(false)} className="text-neutral-400 hover:text-white">
                    <X className="h-3.5 w-3.5" />
                  </button>
                </div>
                <div className="p-3 flex flex-wrap gap-2">
                  {topLoading && <span className="text-neutral-500 text-xs">Loading…</span>}
                  {!topLoading && topList.map((t) => {
                    const already = settings.symbols.includes(t.symbol);
                    return (
                      <button
                        key={t.symbol}
                        type="button"
                        disabled={already}
                        data-testid={`settings-top-add-${t.symbol}`}
                        onClick={() => addSymbol(t.symbol)}
                        className={`px-2.5 py-1.5 border font-mono text-xs inline-flex items-center gap-2 transition-colors ${
                          already
                            ? "border-neutral-900 bg-neutral-950 text-neutral-600 cursor-not-allowed"
                            : "border-neutral-800 text-neutral-200 hover:bg-neutral-900 hover:border-[#007AFF]/40 hover:text-[#007AFF]"
                        }`}
                      >
                        <span>{t.symbol}</span>
                        <span className="text-[10px] text-neutral-500">
                          ${t.mark_price?.toLocaleString(undefined, { maximumFractionDigits: 4 })}
                        </span>
                        {already && <CheckCircle2 className="h-3 w-3 text-[#34C759]" />}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}
          </div>

          <div className="grid md:grid-cols-3 gap-4">
            <div>
              <label className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Virtual capital ($)</label>
              <input
                data-testid={SET.virtualCapital}
                type="number"
                min="0"
                value={settings.virtual_capital}
                onChange={(e) => setSettings({ ...settings, virtual_capital: e.target.value })}
                className="mt-2 w-full h-11 bg-black border border-neutral-800 px-3 text-sm text-white focus:border-[#007AFF] focus:outline-none font-mono"
              />
            </div>
            <div>
              <label className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Timeframe</label>
              <select
                data-testid={SET.timeframe}
                value={settings.timeframe || "5m"}
                onChange={(e) => setSettings({ ...settings, timeframe: e.target.value })}
                className="mt-2 w-full h-11 bg-black border border-neutral-800 px-3 text-sm text-white focus:border-[#007AFF] focus:outline-none font-mono"
              >
                {["1m", "3m", "5m", "15m", "30m", "1h", "4h", "1d"].map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div>
              <label className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Max notional / entry ($)</label>
              <input
                data-testid="settings-max-notional"
                type="number"
                min="0"
                value={settings.max_notional_usd ?? 100}
                onChange={(e) => setSettings({ ...settings, max_notional_usd: e.target.value })}
                className="mt-2 w-full h-11 bg-black border border-neutral-800 px-3 text-sm text-white focus:border-[#007AFF] focus:outline-none font-mono"
              />
              <div className="mt-1 text-[10px] text-neutral-500 leading-relaxed">
                Auto-caps lot count so notional ≤ this limit, per symbol.
              </div>
            </div>
          </div>

          <div className="flex justify-end">
            <button
              data-testid={SET.saveBtn}
              type="submit"
              disabled={saving}
              className="h-10 px-4 bg-[#007AFF] hover:bg-[#0064d1] text-white text-sm font-medium inline-flex items-center gap-2 transition-colors disabled:opacity-50"
            >
              <Save className="h-4 w-4" /> {saving ? "Saving…" : "Save settings"}
            </button>
          </div>
        </form>
      </section>

      <section className="border border-neutral-800 bg-[#121212]">
        <div className="px-6 py-4 border-b border-neutral-800 flex items-center gap-2">
          <KeyRound className="h-4 w-4 text-[#FF3B30]" />
          <h2 className="font-['Barlow_Condensed',sans-serif] text-xl font-bold uppercase">Delta Exchange India</h2>
        </div>
        <div className="p-6 space-y-5">
          <div data-testid={SET.brokerStatus} className="flex items-center gap-3 text-sm">
            {broker.configured ? (
              <>
                <CheckCircle2 className="h-4 w-4 text-[#34C759]" />
                <span className="text-neutral-200">
                  Connected — key <span className="font-mono text-[#007AFF]">{broker.api_key_masked}</span>
                </span>
                {broker.updated_at && <span className="text-neutral-500 text-xs">· updated {new Date(broker.updated_at).toLocaleString()}</span>}
              </>
            ) : (
              <>
                <XCircle className="h-4 w-4 text-[#FF3B30]" />
                <span className="text-neutral-300">Not configured</span>
              </>
            )}
          </div>

          <form onSubmit={saveBroker} className="grid md:grid-cols-2 gap-4">
            <div>
              <label className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">API Key</label>
              <input
                data-testid={SET.apiKey}
                value={apiKey}
                onChange={(e) => setApiKey(e.target.value)}
                autoComplete="off"
                placeholder="Paste your Delta India API key"
                className="mt-2 w-full h-11 bg-black border border-neutral-800 px-3 text-sm text-white focus:border-[#007AFF] focus:outline-none font-mono"
              />
            </div>
            <div>
              <label className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">API Secret</label>
              <input
                data-testid={SET.apiSecret}
                type="password"
                value={apiSecret}
                onChange={(e) => setApiSecret(e.target.value)}
                autoComplete="off"
                placeholder="Paste secret (encrypted at rest)"
                className="mt-2 w-full h-11 bg-black border border-neutral-800 px-3 text-sm text-white focus:border-[#007AFF] focus:outline-none font-mono"
              />
            </div>
            <div className="md:col-span-2 flex flex-wrap items-center gap-2">
              <button
                data-testid={SET.saveBrokerBtn}
                type="submit"
                disabled={brokerBusy}
                className="h-10 px-4 bg-[#007AFF] hover:bg-[#0064d1] text-white text-sm font-medium inline-flex items-center gap-2 transition-colors disabled:opacity-50"
              >
                <Save className="h-4 w-4" /> Save credentials
              </button>
              <button
                data-testid={SET.testBrokerBtn}
                type="button"
                disabled={brokerBusy || !broker.configured}
                onClick={testBroker}
                className="h-10 px-4 border border-neutral-800 text-neutral-200 hover:bg-neutral-900 text-sm inline-flex items-center gap-2 disabled:opacity-40"
              >
                Test connection
              </button>
              {broker.configured && (
                <button
                  data-testid={SET.removeBrokerBtn}
                  type="button"
                  disabled={brokerBusy}
                  onClick={removeBroker}
                  className="h-10 px-3 border border-[#FF3B30]/40 text-[#FF3B30] hover:bg-[#FF3B30]/10 text-sm inline-flex items-center gap-2 disabled:opacity-40 ml-auto"
                >
                  <Trash2 className="h-4 w-4" /> Remove
                </button>
              )}
            </div>
          </form>

          {testResult && (
            <div className={`border p-3 text-xs font-mono ${testResult.ok ? "border-[#34C759]/40 bg-[#34C759]/5 text-[#34C759]" : "border-[#FF3B30]/40 bg-[#FF3B30]/5 text-[#FF3B30]"}`}>
              {testResult.ok ? "Delta wallet reachable." : `Error: ${testResult.error}`}
            </div>
          )}

          <p className="text-[11px] text-neutral-500 leading-relaxed">
            Secrets are encrypted with AES-GCM using a server-side key before being written to MongoDB. They are never returned to the browser.
          </p>
        </div>
      </section>
    </div>
  );
}
