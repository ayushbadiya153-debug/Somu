import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { AUTH } from "@/constants/testIds";

export default function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div
        data-testid={AUTH.authLoading}
        className="min-h-screen bg-[#0A0A0A] flex items-center justify-center text-neutral-500"
      >
        <div className="flex items-center gap-3 font-mono text-sm">
          <div className="h-2 w-2 rounded-full bg-[#007AFF] animate-pulse" />
          Loading…
        </div>
      </div>
    );
  }
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return children;
}
