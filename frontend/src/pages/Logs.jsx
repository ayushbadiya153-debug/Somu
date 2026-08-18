import { useEffect, useState } from "react";
import { RefreshCcw } from "lucide-react";
import { LOG } from "@/constants/testIds";
import { logsApi } from "@/lib/api";
import { toast } from "sonner";

const LEVEL_COLORS = {
  info: "text-[#007AFF]",
  warn: "text-[#FFCC00]",
  error: "text-[#FF3B30]",
  debug: "text-neutral-500",
};

const when = (iso) => {
  if (!iso) return "—";
  try { return new Date(iso).toLocaleTimeString(); } catch { return iso; }
};

export default function Logs() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const d = await logsApi.list();
      setLogs(d.logs || []);
    } catch {
      toast.error("Failed to load logs");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    const id = setInterval(load, 8000);
    return () => clearInterval(id);
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <div className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Telemetry</div>
          <h1 className="font-['Barlow_Condensed',sans-serif] text-3xl md:text-4xl font-black uppercase tracking-tight">
            Activity Log
          </h1>
        </div>
        <button
          data-testid={LOG.refreshBtn}
          onClick={load}
          className="h-10 px-3 border border-neutral-800 bg-neutral-950 hover:bg-neutral-900 text-neutral-300 text-sm inline-flex items-center gap-2"
        >
          <RefreshCcw className="h-4 w-4" /> Refresh
        </button>
      </div>

      <div data-testid={LOG.panel} className="border border-neutral-800 bg-black font-['JetBrains_Mono',monospace] text-xs">
        <div className="max-h-[calc(100vh-260px)] overflow-y-auto divide-y divide-neutral-900">
          {loading && <div className="p-6 text-neutral-500">Loading…</div>}
          {!loading && logs.length === 0 && (
            <div data-testid={LOG.emptyState} className="p-6 text-neutral-500">
              No activity yet. Actions and broker events will appear here.
            </div>
          )}
          {logs.map((l) => (
            <div key={`${l.created_at}-${l.event}-${l.message}`} className="px-5 py-2.5 flex items-start gap-4 hover:bg-neutral-950">
              <span className="text-neutral-600 min-w-[70px]">{when(l.created_at)}</span>
              <span className={`uppercase min-w-[52px] font-semibold ${LEVEL_COLORS[l.level] || "text-neutral-400"}`}>
                {l.level}
              </span>
              <span className="text-neutral-400 min-w-[140px]">{l.event}</span>
              <span className="text-neutral-200 flex-1 break-words">{l.message}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
