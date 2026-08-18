import { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { Activity, BarChart3, History, LineChart, LogOut, Menu, Settings2, X, Zap } from "lucide-react";
import { NAV } from "@/constants/testIds";
import { useAuth } from "@/context/AuthContext";
import { engineApi } from "@/lib/api";

const NAV_ITEMS = [
  { to: "/dashboard", label: "Dashboard", icon: BarChart3, id: NAV.dashboard },
  { to: "/trade", label: "Trade", icon: LineChart, id: NAV.trade },
  { to: "/history", label: "History", icon: History, id: NAV.history },
  { to: "/strategies", label: "Strategies", icon: Zap, id: NAV.strategies },
  { to: "/logs", label: "Activity", icon: Activity, id: NAV.logs },
  { to: "/settings", label: "Settings", icon: Settings2, id: NAV.settings },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [engine, setEngine] = useState({ running: false, mode: "paused" });
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    let stop = false;
    const load = async () => {
      try {
        const e = await engineApi.get();
        if (!stop) setEngine(e);
      } catch (err) {
        if (err?.response?.status !== 401) {
          console.warn("[layout] engine status refresh failed", err);
        }
      }
    };
    load();
    const id = setInterval(load, 10000);
    const onEngineChange = (ev) => { if (ev.detail) setEngine(ev.detail); };
    window.addEventListener("engine:change", onEngineChange);
    return () => {
      stop = true;
      clearInterval(id);
      window.removeEventListener("engine:change", onEngineChange);
    };
  }, []);

  return (
    <div className="min-h-screen bg-[#0A0A0A] text-white font-['Inter',sans-serif]">
      <header className="sticky top-0 z-50 border-b border-neutral-800 bg-black/80 backdrop-blur-xl">
        <div className="mx-auto max-w-[1600px] px-4 md:px-8 h-16 flex items-center justify-between gap-4">
          <button
            data-testid={NAV.logo}
            onClick={() => navigate("/dashboard")}
            className="flex items-center gap-2 group"
          >
            <div className="h-8 w-8 rounded-sm bg-[#007AFF] grid place-items-center shadow-[0_0_20px_rgba(0,122,255,0.4)]">
              <span className="font-['Barlow_Condensed',sans-serif] font-black text-white text-lg leading-none">N</span>
            </div>
            <span className="font-['Barlow_Condensed',sans-serif] font-black text-xl uppercase tracking-widest hidden sm:block">
              Nexus<span className="text-[#007AFF]">Trade</span>
            </span>
          </button>

          <nav className="hidden lg:flex items-center gap-1 flex-1 justify-center">
            {NAV_ITEMS.map((n) => (
              <NavLink
                key={n.to}
                to={n.to}
                data-testid={n.id}
                className={({ isActive }) =>
                  `px-3 py-2 rounded-sm text-sm font-medium tracking-wide flex items-center gap-2 transition-colors duration-200 ${
                    isActive
                      ? "bg-[#007AFF]/15 text-[#007AFF]"
                      : "text-neutral-400 hover:text-white hover:bg-neutral-900"
                  }`
                }
              >
                <n.icon className="h-4 w-4" />
                <span>{n.label}</span>
              </NavLink>
            ))}
          </nav>

          <div className="flex items-center gap-3">
            <span
              data-testid={NAV.liveBadge}
              className="hidden md:inline-flex items-center gap-1.5 px-2.5 py-1 border border-[#FF3B30]/40 bg-[#FF3B30]/10 text-[#FF3B30] font-mono text-[10px] uppercase tracking-widest"
            >
              <span className="h-1.5 w-1.5 rounded-full bg-[#FF3B30] animate-pulse" />
              Live Real Trading
            </span>
            <span
              data-testid={NAV.engineStatus}
              className={`hidden md:inline-flex items-center gap-1.5 px-2.5 py-1 border font-mono text-[10px] uppercase tracking-widest ${
                engine.running
                  ? "border-[#34C759]/40 bg-[#34C759]/10 text-[#34C759]"
                  : "border-neutral-700 bg-neutral-900 text-neutral-400"
              }`}
            >
              <span className={`h-1.5 w-1.5 rounded-full ${engine.running ? "bg-[#34C759]" : "bg-neutral-500"}`} />
              Engine {engine.running ? "Running" : "Idle"}
            </span>

            <div className="hidden md:flex items-center gap-2 pl-3 border-l border-neutral-800">
              {user?.picture ? (
                <img src={user.picture} alt="" className="h-7 w-7 rounded-full object-cover" />
              ) : (
                <div className="h-7 w-7 rounded-full bg-neutral-800" />
              )}
              <span data-testid={NAV.userMenu} className="text-xs text-neutral-300 font-mono max-w-[120px] truncate">
                {user?.email}
              </span>
              <button
                data-testid={NAV.logo + "-logout"}
                onClick={logout}
                className="p-1.5 rounded-sm hover:bg-neutral-900 text-neutral-400 hover:text-white transition-colors"
                aria-label="Log out"
              >
                <LogOut className="h-4 w-4" />
              </button>
            </div>

            <button
              data-testid={NAV.mobileToggle}
              onClick={() => setMobileOpen((v) => !v)}
              className="lg:hidden p-2 rounded-sm text-neutral-300 hover:bg-neutral-900"
              aria-label="Toggle menu"
            >
              {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </button>
          </div>
        </div>

        {mobileOpen && (
          <div className="lg:hidden border-t border-neutral-800 bg-black/95 backdrop-blur-xl px-4 py-3 grid gap-1">
            {NAV_ITEMS.map((n) => (
              <NavLink
                key={n.to}
                to={n.to}
                data-testid={n.id + "-mobile"}
                onClick={() => setMobileOpen(false)}
                className={({ isActive }) =>
                  `px-3 py-2.5 rounded-sm text-sm flex items-center gap-3 ${
                    isActive
                      ? "bg-[#007AFF]/15 text-[#007AFF]"
                      : "text-neutral-300 hover:bg-neutral-900"
                  }`
                }
              >
                <n.icon className="h-4 w-4" />
                {n.label}
              </NavLink>
            ))}
            <button
              onClick={logout}
              className="mt-1 px-3 py-2.5 rounded-sm text-sm flex items-center gap-3 text-[#FF3B30] hover:bg-[#FF3B30]/10"
            >
              <LogOut className="h-4 w-4" /> Log out
            </button>
          </div>
        )}
      </header>

      <main className="mx-auto max-w-[1600px] px-4 md:px-8 py-6 md:py-8">
        <Outlet />
      </main>
    </div>
  );
}
