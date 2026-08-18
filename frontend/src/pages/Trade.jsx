import { useEffect, useMemo, useState } from "react";
import { AlertTriangle } from "lucide-react";
import { TRADE } from "@/constants/testIds";
import { productsApi, tradingApi, settingsApi } from "@/lib/api";
import { toast } from "sonner";

export default function Trade() {
  const [products, setProducts] = useState([]);
  const [broker, setBroker] = useState({ configured: false });
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [form, setForm] = useState({
    product_id: "",
    size: 1,
    side: "buy",
    order_type: "market_order",
    limit_price: "",
    reduce_only: false,
  });

  useEffect(() => {
    (async () => {
      try {
        const [p, b] = await Promise.all([
          productsApi.list("perpetual_futures,call_options,put_options"),
          settingsApi.getBroker().catch(() => ({ configured: false })),
        ]);
        setProducts(p.products || []);
        setBroker(b);
      } catch (e) {
        toast.error("Failed to load products");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const selected = useMemo(() => products.find((p) => String(p.id) === String(form.product_id)), [products, form.product_id]);

  const submit = async (e) => {
    e.preventDefault();
    if (!form.product_id) return toast.error("Pick an instrument");
    if (form.order_type === "limit_order" && !form.limit_price) return toast.error("Enter a limit price");

    setSubmitting(true);
    setResult(null);
    try {
      const payload = {
        product_id: Number(form.product_id),
        symbol: selected?.symbol,
        size: Number(form.size),
        side: form.side,
        order_type: form.order_type,
        reduce_only: form.reduce_only,
      };
      if (form.order_type === "limit_order") payload.limit_price = String(form.limit_price);
      const r = await tradingApi.place(payload);
      setResult(r.order);
      toast.success(`Order ${r.order?.status || "submitted"}`);
    } catch (err) {
      const detail = err.response?.data?.detail;
      const msg = typeof detail === "string" ? detail : JSON.stringify(detail || err.message);
      setResult({ error: msg });
      toast.error("Order failed");
    } finally {
      setSubmitting(false);
    }
  };

  const groupedProducts = useMemo(() => {
    const groups = { perpetual_futures: [], call_options: [], put_options: [] };
    for (const p of products) {
      if (groups[p.contract_type]) groups[p.contract_type].push(p);
    }
    return groups;
  }, [products]);

  return (
    <div className="space-y-6">
      <div>
        <div className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Order Ticket</div>
        <h1 className="font-['Barlow_Condensed',sans-serif] text-3xl md:text-4xl font-black uppercase tracking-tight">
          Place Real Trade
        </h1>
        <p className="mt-1 text-sm text-neutral-400">
          Orders route directly to Delta Exchange India. No paper mode.
        </p>
      </div>

      {!broker.configured && (
        <div className="border border-[#FFCC00]/40 bg-[#FFCC00]/10 text-[#FFCC00] px-4 py-3 text-sm flex items-center gap-2">
          <AlertTriangle className="h-4 w-4" />
          Broker not configured — add your Delta India API key/secret in Settings before submitting an order.
        </div>
      )}

      <div className="grid lg:grid-cols-3 gap-6">
        <form data-testid={TRADE.form} onSubmit={submit} className="lg:col-span-2 border border-neutral-800 bg-[#121212] p-6 space-y-5">
          <div>
            <label className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Instrument</label>
            <select
              data-testid={TRADE.productSelect}
              value={form.product_id}
              onChange={(e) => set("product_id", e.target.value)}
              className="mt-2 w-full h-11 bg-black border border-neutral-800 px-3 text-sm text-white focus:border-[#007AFF] focus:outline-none font-mono"
              disabled={loading}
            >
              <option value="">{loading ? "Loading…" : "Select instrument"}</option>
              {Object.entries(groupedProducts).map(([grp, list]) =>
                list.length > 0 && (
                  <optgroup key={grp} label={grp.replace("_", " ").toUpperCase()}>
                    {list.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.symbol} — {p.description || p.contract_type}
                      </option>
                    ))}
                  </optgroup>
                )
              )}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              data-testid={TRADE.sideBuyBtn}
              onClick={() => set("side", "buy")}
              className={`h-12 border font-medium tracking-widest uppercase text-sm transition-colors duration-200 ${
                form.side === "buy"
                  ? "border-[#34C759] bg-[#34C759]/15 text-[#34C759]"
                  : "border-neutral-800 bg-neutral-950 text-neutral-400 hover:text-white"
              }`}
            >
              Buy · Long
            </button>
            <button
              type="button"
              data-testid={TRADE.sideSellBtn}
              onClick={() => set("side", "sell")}
              className={`h-12 border font-medium tracking-widest uppercase text-sm transition-colors duration-200 ${
                form.side === "sell"
                  ? "border-[#FF3B30] bg-[#FF3B30]/15 text-[#FF3B30]"
                  : "border-neutral-800 bg-neutral-950 text-neutral-400 hover:text-white"
              }`}
            >
              Sell · Short
            </button>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Size (lots)</label>
              <input
                data-testid={TRADE.sizeInput}
                type="number"
                min="1"
                value={form.size}
                onChange={(e) => set("size", e.target.value)}
                className="mt-2 w-full h-11 bg-black border border-neutral-800 px-3 text-sm text-white focus:border-[#007AFF] focus:outline-none font-mono"
              />
            </div>
            <div>
              <label className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Order Type</label>
              <select
                data-testid={TRADE.orderTypeSelect}
                value={form.order_type}
                onChange={(e) => set("order_type", e.target.value)}
                className="mt-2 w-full h-11 bg-black border border-neutral-800 px-3 text-sm text-white focus:border-[#007AFF] focus:outline-none font-mono"
              >
                <option value="market_order">Market</option>
                <option value="limit_order">Limit</option>
              </select>
            </div>
          </div>

          {form.order_type === "limit_order" && (
            <div>
              <label className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Limit Price</label>
              <input
                data-testid={TRADE.limitPriceInput}
                value={form.limit_price}
                onChange={(e) => set("limit_price", e.target.value)}
                placeholder="e.g. 59000"
                className="mt-2 w-full h-11 bg-black border border-neutral-800 px-3 text-sm text-white focus:border-[#007AFF] focus:outline-none font-mono"
              />
            </div>
          )}

          <label className="flex items-center gap-2 text-sm text-neutral-300 cursor-pointer select-none">
            <input
              data-testid={TRADE.reduceOnlyToggle}
              type="checkbox"
              checked={form.reduce_only}
              onChange={(e) => set("reduce_only", e.target.checked)}
              className="h-4 w-4 accent-[#007AFF]"
            />
            <span>Reduce-only (close-only, will not increase exposure)</span>
          </label>

          <button
            data-testid={TRADE.submitBtn}
            type="submit"
            disabled={submitting || !broker.configured}
            className={`w-full h-12 font-medium tracking-widest uppercase text-sm transition-colors duration-200 disabled:opacity-40 disabled:cursor-not-allowed ${
              form.side === "buy" ? "bg-[#34C759] hover:bg-[#2fb350] text-black" : "bg-[#FF3B30] hover:bg-[#e0332a] text-white"
            }`}
          >
            {submitting ? "Submitting…" : `Submit ${form.side.toUpperCase()} Order`}
          </button>
        </form>

        <aside data-testid={TRADE.resultPanel} className="border border-neutral-800 bg-[#121212] p-6 text-sm space-y-3">
          <div>
            <div className="text-[11px] font-mono uppercase tracking-widest text-neutral-500">Selected instrument</div>
            {selected ? (
              <div className="mt-2 space-y-1 font-mono text-xs text-neutral-300">
                <div><span className="text-neutral-500">Symbol:</span> {selected.symbol}</div>
                <div><span className="text-neutral-500">Type:</span> {selected.contract_type}</div>
                <div><span className="text-neutral-500">Underlying:</span> {selected.underlying_asset}</div>
                <div><span className="text-neutral-500">Tick:</span> {selected.tick_size}</div>
                <div><span className="text-neutral-500">Product ID:</span> {selected.id}</div>
              </div>
            ) : (
              <div className="mt-2 text-neutral-500 text-xs">No instrument selected.</div>
            )}
          </div>
          <div className="border-t border-neutral-800 pt-3">
            <div className="text-[11px] font-mono uppercase tracking-widest text-neutral-500 mb-2">Last submission</div>
            {result ? (
              <pre className="font-mono text-[11px] whitespace-pre-wrap break-all text-neutral-300 max-h-80 overflow-auto bg-black border border-neutral-900 p-3">
{JSON.stringify(result, null, 2)}
              </pre>
            ) : (
              <div className="text-neutral-500 text-xs">Submit an order to see the exchange response.</div>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
}
