import { useEffect, useState } from "react";
import { AlertTriangle, Cpu, Pencil, Save, X } from "lucide-react";
import { STRAT } from "@/constants/testIds";
import { strategiesApi } from "@/lib/api";
import { toast } from "sonner";

function ParamsEditor({ strategy, onClose, onSaved }) {
  const [values, setValues] = useState(strategy.params || {});
  const [saving, setSaving] = useState(false);

  const save = async () => {
    setSaving(true);
    try {
      const cleaned = {};
      for (const [k, v] of Object.entries(values)) {
        const num = Number(v);
        cleaned[k] = Number.isFinite(num) ? num : v;
      }
      await strategiesApi.updateParams(strategy.key, cleaned);
      toast.success("Parameters saved");
      onSaved(cleaned);
      onClose();
    } catch (e) {
      toast.error(e.response?.data?.detail || "Save failed");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="w-full max-w-md border border-neutral-800 bg-[#121212]">
        <div className="flex items-center justify-between px-5 py-4 border-b border-neutral-800">
          <div>
            <div className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Parameters</div>
            <h3 className="font-['Barlow_Condensed',sans-serif] text-xl font-bold uppercase">{strategy.name}</h3>
          </div>
          <button
            data-testid={`strategy-params-close-${strategy.key}`}
            onClick={onClose}
            className="p-2 text-neutral-400 hover:text-white"
            aria-label="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        <div className="p-5 grid gap-3">
          {Object.entries(strategy.params || {}).map(([k, v]) => (
            <div key={k}>
              <label className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">{k}</label>
              <input
                data-testid={`strategy-param-${strategy.key}-${k}`}
                type="number"
                step="any"
                value={values[k] ?? ""}
                onChange={(e) => setValues({ ...values, [k]: e.target.value })}
                className="mt-2 w-full h-10 bg-black border border-neutral-800 px-3 text-sm text-white focus:border-[#007AFF] focus:outline-none font-mono"
              />
            </div>
          ))}
          {(!strategy.params || Object.keys(strategy.params).length === 0) && (
            <div className="text-neutral-500 text-sm">No parameters.</div>
          )}
        </div>
        <div className="px-5 py-4 border-t border-neutral-800 flex justify-end gap-2">
          <button onClick={onClose} className="h-9 px-3 border border-neutral-800 text-neutral-300 hover:bg-neutral-900 text-sm">
            Cancel
          </button>
          <button
            data-testid={`strategy-params-save-${strategy.key}`}
            onClick={save}
            disabled={saving}
            className="h-9 px-4 bg-[#007AFF] hover:bg-[#0064d1] text-white text-sm inline-flex items-center gap-2 disabled:opacity-50"
          >
            <Save className="h-3.5 w-3.5" /> {saving ? "Saving…" : "Save"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function Strategies() {
  const [strategies, setStrategies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState({});
  const [editing, setEditing] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const d = await strategiesApi.list();
      setStrategies(d.strategies || []);
    } catch {
      toast.error("Failed to load strategies");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const toggle = async (key, next) => {
    setBusy((b) => ({ ...b, [key]: true }));
    try {
      await strategiesApi.toggle(key, next);
      setStrategies((prev) => prev.map((s) => s.key === key ? { ...s, enabled: next } : s));
      toast.success(`${key} ${next ? "enabled" : "disabled"}`);
    } catch {
      toast.error("Toggle failed");
    } finally {
      setBusy((b) => ({ ...b, [key]: false }));
    }
  };

  const onSaved = (key, params) => {
    setStrategies((prev) => prev.map((s) => s.key === key ? { ...s, params } : s));
  };

  return (
    <div className="space-y-6">
      <div>
        <div className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Automation</div>
        <h1 className="font-['Barlow_Condensed',sans-serif] text-3xl md:text-4xl font-black uppercase tracking-tight">
          Strategies
        </h1>
        <p className="mt-1 text-sm text-neutral-400">
          Enable a strategy and start the engine — signals compute on each closed bar of your default symbol/timeframe and fire real Delta orders.
        </p>
      </div>

      <div className="border border-neutral-800 bg-neutral-950/60 text-neutral-300 px-4 py-3 text-sm flex items-start gap-2">
        <AlertTriangle className="h-4 w-4 mt-0.5 text-[#FFCC00]" />
        <div>
          Engine trades the <span className="text-white font-mono">default symbol</span> + <span className="text-white font-mono">timeframe</span> set in Settings. Position size per signal is the <code className="text-[#007AFF]">size</code> parameter on each strategy card.
        </div>
      </div>

      <div data-testid={STRAT.list} className="grid md:grid-cols-2 gap-4">
        {loading && <div className="text-neutral-500 text-sm">Loading…</div>}
        {strategies.map((s) => (
          <div
            key={s.key}
            data-testid={`${STRAT.cardPrefix}${s.key}`}
            className="border border-neutral-800 bg-[#121212] p-5"
          >
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <Cpu className="h-4 w-4 text-[#007AFF]" />
                  <h3 className="font-['Barlow_Condensed',sans-serif] text-xl font-bold uppercase tracking-wide">
                    {s.name}
                  </h3>
                </div>
                <p className="mt-1 text-sm text-neutral-400 leading-relaxed">{s.description}</p>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  data-testid={`${STRAT.togglePrefix}${s.key}`}
                  type="checkbox"
                  checked={!!s.enabled}
                  disabled={!!busy[s.key]}
                  onChange={(e) => toggle(s.key, e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-neutral-800 rounded-full peer peer-checked:bg-[#007AFF] transition-colors duration-200 after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-transform peer-checked:after:translate-x-5" />
              </label>
            </div>

            {s.params && Object.keys(s.params).length > 0 && (
              <div className="mt-4 flex flex-wrap items-center gap-2">
                {Object.entries(s.params).map(([k, v]) => (
                  <span key={k} className="font-mono text-[10px] uppercase tracking-widest px-2 py-1 border border-neutral-800 text-neutral-400">
                    {k}: <span className="text-neutral-200">{String(v)}</span>
                  </span>
                ))}
                <button
                  data-testid={`strategy-edit-${s.key}`}
                  onClick={() => setEditing(s)}
                  className="ml-auto h-7 px-2 border border-neutral-800 text-neutral-300 hover:bg-neutral-900 hover:text-white text-xs inline-flex items-center gap-1 transition-colors"
                >
                  <Pencil className="h-3 w-3" /> Edit
                </button>
              </div>
            )}
          </div>
        ))}
      </div>

      {editing && (
        <ParamsEditor
          strategy={editing}
          onClose={() => setEditing(null)}
          onSaved={(params) => onSaved(editing.key, params)}
        />
      )}
    </div>
  );
}
