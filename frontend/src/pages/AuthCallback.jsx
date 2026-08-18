import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { authApi } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";

// REMINDER: DO NOT HARDCODE THE URL, OR ADD ANY FALLBACKS OR REDIRECT URLS, THIS BREAKS THE AUTH
export default function AuthCallback() {
  const location = useLocation();
  const navigate = useNavigate();
  const { setUser } = useAuth();

  useEffect(() => {
    const hash = location.hash || window.location.hash;
    const match = hash.match(/session_id=([^&]+)/);
    if (!match) {
      navigate("/login", { replace: true });
      return;
    }
    const sessionId = match[1];
    // Clear hash so refresh doesn't reprocess
    window.history.replaceState(null, "", window.location.pathname);

    (async () => {
      try {
        const user = await authApi.session(sessionId);
        setUser(user);
        navigate("/dashboard", { replace: true, state: { user } });
      } catch (e) {
        navigate("/login", { replace: true });
      }
    })();
  }, []);

  return (
    <div className="min-h-screen bg-[#0A0A0A] flex items-center justify-center text-neutral-300">
      <div className="flex items-center gap-3">
        <div className="h-3 w-3 rounded-full bg-[#007AFF] animate-pulse" />
        <span className="font-mono text-sm tracking-wide">Signing you in…</span>
      </div>
    </div>
  );
}
