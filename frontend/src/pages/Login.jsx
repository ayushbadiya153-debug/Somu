import { useNavigate } from "react-router-dom";
import { useEffect } from "react";
import { AUTH } from "@/constants/testIds";
import { useAuth } from "@/context/AuthContext";
import { LineChart, ShieldCheck, Zap } from "lucide-react";

// REMINDER: DO NOT HARDCODE THE URL, OR ADD ANY FALLBACKS OR REDIRECT URLS, THIS BREAKS THE AUTH
export default function Login() {
  const navigate = useNavigate();
  const { user, loading } = useAuth();

  useEffect(() => {
    if (!loading && user) navigate("/dashboard", { replace: true });
  }, [loading, user, navigate]);

  const handleLogin = () => {
    const redirectUrl = window.location.origin + "/dashboard";
    window.location.href = `https://auth.emergentagent.com/?redirect=${encodeURIComponent(redirectUrl)}`;
  };

  return (
    <div className="min-h-screen bg-[#0A0A0A] text-white font-['Inter',sans-serif] relative overflow-hidden">
      <div className="absolute inset-0 opacity-30 pointer-events-none"
           style={{ background: "radial-gradient(circle at 20% 20%, rgba(0,122,255,0.2), transparent 40%), radial-gradient(circle at 80% 80%, rgba(255,59,48,0.15), transparent 40%)" }} />
      <div className="absolute inset-0 opacity-[0.03] pointer-events-none"
           style={{ backgroundImage: "linear-gradient(rgba(255,255,255,0.5) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.5) 1px, transparent 1px)", backgroundSize: "48px 48px" }} />

      <div className="relative mx-auto max-w-6xl px-6 py-10 md:py-16 grid lg:grid-cols-2 gap-10 min-h-screen items-center">
        <div>
          <div className="inline-flex items-center gap-2 px-2.5 py-1 border border-[#FF3B30]/40 bg-[#FF3B30]/10 text-[#FF3B30] font-mono text-[10px] uppercase tracking-widest mb-6">
            <span className="h-1.5 w-1.5 rounded-full bg-[#FF3B30] animate-pulse" /> Real Trading · Delta India
          </div>
          <h1 className="font-['Barlow_Condensed',sans-serif] font-black uppercase tracking-tight text-4xl sm:text-5xl lg:text-6xl leading-[0.95]">
            Precision built<br />
            for the <span className="text-[#007AFF]">execution</span> edge.
          </h1>
          <p className="mt-5 text-neutral-400 max-w-lg leading-relaxed">
            NexusTrade is a tactical dashboard for automated derivatives trading on Delta Exchange India — perpetual futures and options. Wire up your API keys, plug in your strategy, and run.
          </p>

          <ul className="mt-8 grid gap-3 text-sm text-neutral-300 max-w-md">
            <li className="flex items-start gap-3 border border-neutral-800 bg-neutral-950/60 p-3">
              <ShieldCheck className="h-4 w-4 text-[#34C759] mt-0.5" />
              <span>Secrets encrypted at rest with AES-GCM. Never exposed to the browser.</span>
            </li>
            <li className="flex items-start gap-3 border border-neutral-800 bg-neutral-950/60 p-3">
              <Zap className="h-4 w-4 text-[#FFCC00] mt-0.5" />
              <span>Real orders on Delta India — perpetuals, call &amp; put options.</span>
            </li>
            <li className="flex items-start gap-3 border border-neutral-800 bg-neutral-950/60 p-3">
              <LineChart className="h-4 w-4 text-[#007AFF] mt-0.5" />
              <span>Live positions, P&amp;L, and activity logs at a glance.</span>
            </li>
          </ul>
        </div>

        <div className="w-full max-w-md justify-self-center lg:justify-self-end">
          <div className="border border-neutral-800 bg-[#121212] p-8">
            <div className="font-['Barlow_Condensed',sans-serif] uppercase tracking-widest text-xs text-neutral-500 mb-2">Sign in</div>
            <h2 className="font-['Barlow_Condensed',sans-serif] text-3xl font-bold mb-1">Access your desk</h2>
            <p className="text-sm text-neutral-400 mb-8">Continue with your Google account. Your Delta credentials are added later inside Settings.</p>

            <button
              data-testid={AUTH.loginBtn}
              onClick={handleLogin}
              className="w-full h-11 bg-white text-black hover:bg-neutral-200 flex items-center justify-center gap-3 font-medium transition-colors duration-200"
            >
              <svg className="h-5 w-5" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.99.66-2.26 1.06-3.71 1.06-2.85 0-5.27-1.93-6.13-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.87 14.1c-.22-.66-.35-1.36-.35-2.1s.13-1.44.35-2.1V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l3.69-2.84z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.69 2.84C6.73 7.31 9.15 5.38 12 5.38z"/>
              </svg>
              Continue with Google
            </button>

            <p className="mt-6 text-[11px] text-neutral-500 leading-relaxed">
              By continuing you accept that this app places <span className="text-[#FF3B30]">real orders</span> on Delta Exchange India when configured. You are responsible for any resulting P&amp;L.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
