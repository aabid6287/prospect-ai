import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { FileText, LogOut, User as UserIcon } from 'lucide-react';

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();

  return (
    <header className="sticky top-0 z-30 bg-white/95 backdrop-blur-sm border-b border-slate-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <Link to="/" className="flex items-center gap-2.5 group">
          <div className="w-9 h-9 rounded-lg bg-blue-600 flex items-center justify-center text-white shadow-sm transition-transform group-hover:scale-105">
            <FileText className="w-5 h-5" />
          </div>
          <div className="flex flex-col">
            <span className="font-semibold text-slate-900 tracking-tight text-base">ProspectAI</span>
            <span className="text-[10px] text-slate-500 uppercase tracking-wider font-medium -mt-1">RHP Intelligence</span>
          </div>
        </Link>

        {/* Right Nav */}
        <div className="flex items-center gap-3">
          {isAuthenticated ? (
            <>
              <Link
                to="/dashboard"
                className="text-sm font-medium text-slate-700 hover:text-blue-600 transition-colors"
              >
                Dashboard
              </Link>
              <div className="h-4 w-px bg-slate-200" />
              <div className="flex items-center gap-2 text-sm text-slate-600">
                <div className="w-7 h-7 rounded-full bg-slate-100 flex items-center justify-center text-slate-700 font-medium">
                  <UserIcon className="w-4 h-4" />
                </div>
                <span className="font-medium text-slate-800">{user?.name || user?.email || 'User'}</span>
              </div>
              <button
                onClick={logout}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slate-600 hover:text-rose-600 hover:bg-rose-50 rounded-md transition-colors"
                title="Sign out"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Sign out</span>
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors px-3 py-1.5"
              >
                Sign In
              </Link>
              <Link
                to="/register"
                className="inline-flex items-center justify-center px-4 py-2 text-sm font-medium rounded-lg text-white bg-blue-600 hover:bg-blue-700 shadow-sm transition-colors"
              >
                Get Started
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
