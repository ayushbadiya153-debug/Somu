import { useEffect, useState } from "react";
import { RefreshCcw } from "lucide-react";
import { HIST } from "@/constants/testIds";
import { historyApi } from "@/lib/api";
import { toast } from "sonner";

const fmt = (v) => {
  if (v === null || v === undefined || Number.isNaN(Number(v))) return "—";
  return Number(v).toLocaleString(undefined, { maximumFractionDigits: 4 });
};

const when = (iso) => {
  if (!iso) return "—";
  try { return new Date(iso).toLocaleString(); } catch { return iso; }
};

export default function History() {
  const [trades, setTrades] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const d = await historyApi.local();
      setTrades(d.trades || []);
    } catch {
      toast.error("Failed to load history");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <div className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Ledger</div>
          <h1 className="font-['Barlow_Condensed',sans-serif] text-3xl md:text-4xl font-black uppercase tracking-tight">
            Trade History
          </h1>
        </div>
        <button
          data-testid={HIST.refreshBtn}
          onClick={load}
          className="h-10 px-3 border border-neutral-800 bg-neutral-950 hover:bg-neutral-900 text-neutral-300 text-sm inline-flex items-center gap-2"
        >
          <RefreshCcw className="h-4 w-4" /> Refresh
        </button>
      </div>

      <div className="border border-neutral-800 bg-[#121212] overflow-x-auto">
        <table data-testid={HIST.table} className="w-full text-sm font-['JetBrains_Mono',monospace]">
          <thead>
            <tr className="text-neutral-500 text-[11px] uppercase tracking-widest">
              <th className="text-left px-5 py-3 font-normal">Date / Time</th>
              <th className="text-left px-5 py-3 font-normal">Symbol</th>
              <th className="text-left px-5 py-3 font-normal">Side</th>
              <th className="text-right px-5 py-3 font-normal">Qty</th>
              <th className="text-right px-5 py-3 font-normal">Entry</th>
              <th className="text-right px-5 py-3 font-normal">Exit</th>
              <th className="text-right px-5 py-3 font-normal">P&amp;L</th>
              <th className="text-right px-5 py-3 font-normal">Status</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr><td colSpan={8} className="text-center py-10 text-neutral-500 text-xs">Loading…</td></tr>
            )}
            {!loading && trades.length === 0 && (
              <tr>
                <td data-testid={HIST.emptyState} colSpan={8} className="text-center py-10 text-neutral-500 text-xs">
                  No trades yet.
                </td>
              </tr>
            )}
            {trades.map((t) => (
              <tr key={t.exchange_order_id || t.client_order_id || `${t.created_at}-${t.symbol}-${t.side}`} className="border-t border-neutral-900 hover:bg-neutral-950">
                <td className="px-5 py-3 text-neutral-400">{when(t.created_at)}</td>
                <td className="px-5 py-3 text-white">{t.symbol || `#${t.product_id}`}</td>
                <td className={`px-5 py-3 uppercase ${t.side === "buy" ? "text-[#34C759]" : "text-[#FF3B30]"}`}>
                  {t.side}
                </td>
                <td className="px-5 py-3 text-right">{fmt(t.quantity)}</td>
                <td className="px-5 py-3 text-right">{fmt(t.entry_price)}</td>
                <td className="px-5 py-3 text-right">{fmt(t.exit_price)}</td>
                <td className={`px-5 py-3 text-right ${(t.realized_pnl ?? 0) >= 0 ? "text-[#34C759]" : "text-[#FF3B30]"}`}>
                  {fmt(t.realized_pnl)}
                </td>
                <td className="px-5 py-3 text-right text-neutral-400 uppercase text-[11px]">{t.status || "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
