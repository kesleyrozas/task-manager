import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { clearToken, setToken } from '../api/client';
import { auth } from '../api/endpoints';
import type { AuthResponse } from '../types';

interface AuthState {
  user: AuthResponse | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string) => Promise<void>;
  logout: () => void;
}

const STORAGE_KEY = 'tm_user';
const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(null);

  useEffect(() => {
    const cached = localStorage.getItem(STORAGE_KEY);
    if (cached) {
      try {
        setUser(JSON.parse(cached));
      } catch {
        localStorage.removeItem(STORAGE_KEY);
      }
    }
  }, []);

  const persist = (data: AuthResponse) => {
    setUser(data);
    setToken(data.token);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  };

  const value: AuthState = {
    user,
    login: async (email, password) => {
      const data = await auth.login(email, password);
      persist(data);
    },
    register: async (email, password, name) => {
      const data = await auth.register(email, password, name);
      persist(data);
    },
    logout: () => {
      clearToken();
      localStorage.removeItem(STORAGE_KEY);
      setUser(null);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
