import { useEffect, useState, useCallback } from "react";
import { RefreshCcw, Play, Square, ArrowUpRight, ArrowDownRight, Wallet, TrendingUp, TrendingDown, Activity, ShieldAlert, Timer } from "lucide-react";
import { DASH } from "@/constants/testIds";
import { dashboardApi, engineApi } from "@/lib/api";
import { toast } from "sonner";
import { Link } from "react-router-dom";

const fmt = (v, digits = 2) => {
  if (v === null || v === undefined || Number.isNaN(Number(v))) return "—";
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: digits, maximumFractionDigits: digits });
};

function Kpi({ testid, label, value, hint, tone = "default", icon: Icon }) {
  const tones = {
    default: "text-white",
    up: "text-[#34C759]",
    down: "text-[#FF3B30]",
    accent: "text-[#007AFF]",
  };
  return (
    <div data-testid={testid} className="border border-neutral-800 bg-[#121212] p-5">
      <div className="flex items-center justify-between text-neutral-500 text-[11px] uppercase tracking-widest font-mono">
        <span>{label}</span>
        {Icon && <Icon className="h-3.5 w-3.5" />}
      </div>
      <div className={`mt-2 font-['JetBrains_Mono',monospace] text-2xl md:text-3xl font-semibold ${tones[tone]}`}>
        {value}
      </div>
      {hint && <div className="mt-1 text-xs text-neutral-500">{hint}</div>}
    </div>
  );
}

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [engineBusy, setEngineBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const d = await dashboardApi.get();
      setData(d);
    } catch (e) {
      toast.error("Failed to load dashboard");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    const id = setInterval(load, 15000);
    return () => clearInterval(id);
  }, [load]);

  const toggleEngine = async () => {
    setEngineBusy(true);
    try {
      let next;
      if (data?.engine?.running) {
        next = await engineApi.stop();
        toast.success("Engine stopped");
      } else {
        next = await engineApi.start();
        toast.success("Engine started");
      }
      window.dispatchEvent(new CustomEvent("engine:change", { detail: next }));
      await load();
    } catch (e) {
      toast.error(e.response?.data?.detail || "Engine action failed");
    } finally {
      setEngineBusy(false);
    }
  };

  const running = data?.engine?.running;
  const brokerConfigured = data?.broker?.configured;
  const positions = data?.open_positions || [];

  return (
    <div data-testid={DASH.container} className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Command Center</div>
          <h1 className="font-['Barlow_Condensed',sans-serif] text-3xl md:text-4xl font-black uppercase tracking-tight">
            Trading Dashboard
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <button
            data-testid={DASH.refreshBtn}
            onClick={load}
            className="h-10 px-3 border border-neutral-800 bg-neutral-950 hover:bg-neutral-900 text-neutral-300 text-sm inline-flex items-center gap-2 transition-colors duration-200"
          >
            <RefreshCcw className="h-4 w-4" /> Refresh
          </button>
          {running ? (
            <button
              data-testid={DASH.engineStopBtn}
              disabled={engineBusy}
              onClick={toggleEngine}
              className="h-10 px-4 bg-[#FF3B30] hover:bg-[#e0332a] text-white font-medium text-sm inline-flex items-center gap-2 transition-colors duration-200 disabled:opacity-50"
            >
              <Square className="h-4 w-4" /> Stop Engine
            </button>
          ) : (
            <button
              data-testid={DASH.engineStartBtn}
              disabled={engineBusy}
              onClick={toggleEngine}
              className="h-10 px-4 bg-[#007AFF] hover:bg-[#0064d1] text-white font-medium text-sm inline-flex items-center gap-2 transition-colors duration-200 disabled:opacity-50"
            >
              <Play className="h-4 w-4" /> Start Engine
            </button>
          )}
        </div>
      </div>

      {!brokerConfigured && (
        <div data-testid={DASH.brokerBanner} className="border border-[#FFCC00]/40 bg-[#FFCC00]/10 text-[#FFCC00] px-4 py-3 text-sm flex items-center justify-between gap-4">
          <span>Delta Exchange India credentials are not configured. Real trading is disabled until you add them.</span>
          <Link to="/settings" className="underline underline-offset-2 hover:text-white">
            Go to Settings
          </Link>
        </div>
      )}
      {brokerConfigured && data?.broker?.error && (
        <div className="border border-[#FF3B30]/40 bg-[#FF3B30]/10 text-[#FF3B30] px-4 py-3 text-sm">
          Broker error: {String(data.broker.error)}
        </div>
      )}

      {data?.risk && (
        <div
          data-testid="risk-panel"
          className={`border p-4 grid sm:grid-cols-2 lg:grid-cols-4 gap-4 ${
            data.risk.blocked_reason
              ? "border-[#FF3B30]/40 bg-[#FF3B30]/5"
              : "border-neutral-800 bg-[#121212]"
          }`}
        >
          <div>
            <div className="flex items-center gap-1.5 text-[11px] font-mono uppercase tracking-widest text-neutral-500">
              <ShieldAlert className="h-3 w-3" /> Risk manager · today ({data.risk.date})
            </div>
            {data.risk.blocked_reason ? (
              <div data-testid="risk-blocked" className="mt-1 text-sm text-[#FF3B30] font-medium">
                Entries BLOCKED — {data.risk.blocked_reason}
              </div>
            ) : (
              <div className="mt-1 text-sm text-[#34C759] font-medium">Entries allowed</div>
            )}
          </div>
          <div data-testid="risk-entries">
            <div className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Entries today</div>
            <div className="mt-1 font-['JetBrains_Mono',monospace] text-lg">
              <span className={data.risk.entries >= data.risk.entries_max ? "text-[#FF3B30]" : "text-white"}>
                {data.risk.entries}
              </span>
              <span className="text-neutral-500"> / {data.risk.entries_max}</span>
            </div>
          </div>
          <div data-testid="risk-sl">
            <div className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">SL hits today</div>
            <div className="mt-1 font-['JetBrains_Mono',monospace] text-lg">
              <span className={data.risk.sl_hits >= data.risk.sl_max ? "text-[#FF3B30]" : "text-white"}>
                {data.risk.sl_hits}
              </span>
              <span className="text-neutral-500"> / {data.risk.sl_max}</span>
            </div>
          </div>
          <div data-testid="risk-cooldown">
            <div className="flex items-center gap-1.5 text-[11px] font-mono uppercase tracking-widest text-neutral-500">
              <Timer className="h-3 w-3" /> SL cooldown
            </div>
            <div className="mt-1 text-sm text-neutral-300 font-mono">
              {data.risk.last_sl_at
                ? `Last SL: ${new Date(data.risk.last_sl_at).toLocaleTimeString()}`
                : "No SL yet"}
              <span className="text-neutral-500"> · {data.risk.cooldown_min} min</span>
            </div>
          </div>
        </div>
      )}

      {data?.notional_preview && Array.isArray(data.notional_preview.symbols) && data.notional_preview.symbols.length > 0 && (
        <div data-testid="notional-preview" className="border border-neutral-800 bg-[#121212]">
          <div className="px-5 py-3 border-b border-neutral-800 flex items-center justify-between">
            <div className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">
              Notional cap · ${fmt(data.notional_preview.max_notional_usd, 0)} per entry
            </div>
            <div className="text-[11px] text-neutral-500 font-mono">{data.notional_preview.symbols.length} symbol{data.notional_preview.symbols.length === 1 ? "" : "s"}</div>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm font-['JetBrains_Mono',monospace]">
              <thead>
                <tr className="text-neutral-500 text-[11px] uppercase tracking-widest">
                  <th className="text-left px-5 py-2 font-normal">Symbol</th>
                  <th className="text-right px-5 py-2 font-normal">Price</th>
                  <th className="text-right px-5 py-2 font-normal">Notional / lot</th>
                  <th className="text-right px-5 py-2 font-normal">Max lots</th>
                  <th className="text-right px-5 py-2 font-normal">Notional used</th>
                </tr>
              </thead>
              <tbody>
                {data.notional_preview.symbols.map((s) => (
                  <tr key={s.symbol} data-testid={`notional-row-${s.symbol}`} className="border-t border-neutral-900 hover:bg-neutral-950">
                    <td className="px-5 py-2 text-white">{s.symbol}</td>
                    <td className="px-5 py-2 text-right">{s.ref_price != null ? fmt(s.ref_price, 4) : "—"}</td>
                    <td className="px-5 py-2 text-right">{s.notional_per_lot != null ? `$${fmt(s.notional_per_lot, 2)}` : "—"}</td>
                    <td className={`px-5 py-2 text-right ${s.max_lots != null && s.max_lots < 1 ? "text-[#FF3B30]" : "text-[#34C759]"}`}>
                      {s.max_lots != null ? s.max_lots : "—"}
                    </td>
                    <td className="px-5 py-2 text-right text-neutral-400">
                      {s.max_lots != null && s.notional_per_lot != null
                        ? `$${fmt(s.max_lots * s.notional_per_lot, 2)}`
                        : (s.error ? <span className="text-[#FF3B30]">{s.error}</span> : "—")}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <Kpi testid={DASH.cardVirtualBalance} label="Virtual Balance" icon={Wallet}
             value={loading ? "…" : `$${fmt(data?.virtual_account_balance)}`}
             hint="Capital + realized + unrealized" tone="accent" />
        <Kpi testid={DASH.cardCash} label={`Exchange Cash${data?.broker?.wallet_currency ? ` · ${data.broker.wallet_currency}` : ""}`} icon={Wallet}
             value={loading
               ? "…"
               : (brokerConfigured
                   ? (data?.broker?.wallet_available != null
                       ? fmt(data.broker.wallet_available)
                       : (data?.broker?.wallet_balance != null ? fmt(data.broker.wallet_balance) : "—"))
                   : "—")}
             hint={brokerConfigured
               ? (data?.broker?.wallet_balance != null
                   ? `Total in Delta wallet: ${fmt(data.broker.wallet_balance)} ${data?.broker?.wallet_currency || ""}`
                   : "No wallet balance reported")
               : "Add Delta keys in Settings"} />
        <Kpi testid={DASH.cardUnrealizedPnl} label="Unrealized P&L" icon={TrendingUp}
             value={loading ? "…" : `$${fmt(data?.unrealized_pnl)}`}
             tone={(data?.unrealized_pnl || 0) >= 0 ? "up" : "down"} />
        <Kpi testid={DASH.cardRealizedPnl} label="Realized P&L" icon={TrendingDown}
             value={loading ? "…" : `$${fmt(data?.realized_pnl)}`}
             tone={(data?.realized_pnl || 0) >= 0 ? "up" : "down"} />
        <Kpi testid={DASH.cardOpenPositions} label="Open Positions" icon={Activity}
             value={loading ? "…" : `${data?.open_positions_count ?? 0}`}
             hint="Live on Delta India" />
        <Kpi testid={DASH.cardTotalTrades} label="Total Trades" icon={Activity}
             value={loading ? "…" : `${data?.total_trades ?? 0}`}
             hint="All-time record count" />
      </div>

      <div className="border border-neutral-800 bg-[#121212]">
        <div className="px-5 py-4 border-b border-neutral-800 flex items-center justify-between">
          <div>
            <div className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Live</div>
            <h2 className="font-['Barlow_Condensed',sans-serif] text-2xl font-bold uppercase">Open Positions</h2>
          </div>
          <Link to="/trade" className="text-xs text-[#007AFF] hover:text-white inline-flex items-center gap-1">
            New order <ArrowUpRight className="h-3 w-3" />
          </Link>
        </div>
        <div data-testid={DASH.positionsTable} className="overflow-x-auto">
          <table className="w-full text-sm font-['JetBrains_Mono',monospace]">
            <thead>
              <tr className="text-neutral-500 text-[11px] uppercase tracking-widest">
                <th className="text-left px-5 py-3 font-normal">Symbol</th>
                <th className="text-right px-5 py-3 font-normal">Size</th>
                <th className="text-right px-5 py-3 font-normal">Entry</th>
                <th className="text-right px-5 py-3 font-normal">Mark</th>
                <th className="text-right px-5 py-3 font-normal">Unrealized</th>
                <th className="text-right px-5 py-3 font-normal">Liq.</th>
              </tr>
            </thead>
            <tbody>
              {positions.length === 0 && (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-neutral-500 text-xs">
                    No open positions.
                  </td>
                </tr>
              )}
              {positions.map((p) => (
                <tr key={`${p.symbol || p.product_id}-${p.entry_price}`} className="border-t border-neutral-900 hover:bg-neutral-950">
                  <td className="px-5 py-3 text-white">
                    <span className="inline-flex items-center gap-2">
                      {Number(p.size) > 0 ? <ArrowUpRight className="h-3 w-3 text-[#34C759]" /> : <ArrowDownRight className="h-3 w-3 text-[#FF3B30]" />}
                      {p.symbol || `#${p.product_id}`}
                    </span>
                  </td>
                  <td className="px-5 py-3 text-right">{fmt(p.size, 0)}</td>
                  <td className="px-5 py-3 text-right">{fmt(p.entry_price, 4)}</td>
                  <td className="px-5 py-3 text-right">{fmt(p.mark_price, 4)}</td>
                  <td className={`px-5 py-3 text-right ${Number(p.unrealized_pnl) >= 0 ? "text-[#34C759]" : "text-[#FF3B30]"}`}>
                    {fmt(p.unrealized_pnl)}
                  </td>
                  <td className="px-5 py-3 text-right text-neutral-500">{fmt(p.liquidation_price, 4)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
