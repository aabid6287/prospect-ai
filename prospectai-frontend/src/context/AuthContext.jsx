import React, { createContext, useContext, useState, useEffect } from 'react';
import authService from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('prospectai_user');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });
  const [token, setToken] = useState(() => localStorage.getItem('prospectai_token') || null);
  const [loading, setLoading] = useState(true);

  // Validate stored session on initial mount with /api/users/me
  useEffect(() => {
    const verifySession = async () => {
      const storedToken = localStorage.getItem('prospectai_token');
      if (!storedToken) {
        setLoading(false);
        return;
      }

      try {
        const userData = await authService.getCurrentUser();
        setUser(userData);
        localStorage.setItem('prospectai_user', JSON.stringify(userData));
      } catch (err) {
        // Token invalid or expired: clear storage
        setToken(null);
        setUser(null);
        localStorage.removeItem('prospectai_token');
        localStorage.removeItem('prospectai_user');
      } finally {
        setLoading(false);
      }
    };

    verifySession();
  }, []);

  const login = async (email, password) => {
    const authData = await authService.login({ email, password });
    setToken(authData.token);
    const userProfile = {
      id: authData.id,
      name: authData.name,
      email: authData.email,
      role: authData.role,
    };
    setUser(userProfile);
    localStorage.setItem('prospectai_token', authData.token);
    localStorage.setItem('prospectai_user', JSON.stringify(userProfile));
    return authData;
  };

  const register = async (name, email, password) => {
    const authData = await authService.register({ name, email, password });
    setToken(authData.token);
    const userProfile = {
      id: authData.id,
      name: authData.name,
      email: authData.email,
      role: authData.role,
    };
    setUser(userProfile);
    localStorage.setItem('prospectai_token', authData.token);
    localStorage.setItem('prospectai_user', JSON.stringify(userProfile));
    return authData;
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('prospectai_token');
    localStorage.removeItem('prospectai_user');
  };

  const isAuthenticated = Boolean(token && user);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated,
        loading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
