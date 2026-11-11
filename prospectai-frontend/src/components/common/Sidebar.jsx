import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  FileText,
  MessageSquare,
  BarChart3,
  Settings,
  ShieldCheck,
} from 'lucide-react';

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/dashboard/documents', label: 'My Documents', icon: FileText },
  { to: '/dashboard/chats', label: 'Recent Chats', icon: MessageSquare },
  { to: '/dashboard/analysis', label: 'RHP Analysis', icon: BarChart3 },
];

export default function Sidebar() {
  return (
    <aside className="w-64 bg-white border-r border-slate-200 min-h-[calc(100vh-4rem)] flex flex-col justify-between p-4">
      <div className="space-y-6">
        {/* Navigation list */}
        <div>
          <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider px-3 mb-2">
            Research Workspace
          </p>
          <nav className="space-y-1">
            {NAV_ITEMS.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === '/dashboard'}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-blue-50 text-blue-700 font-semibold'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                    }`
                  }
                >
                  <Icon className="w-4 h-4" />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
          </nav>
        </div>
      </div>

      {/* Footer / Disclaimer status */}
      <div className="pt-4 border-t border-slate-200">
        <div className="flex items-center gap-2 px-3 py-2 text-xs text-slate-500 bg-slate-50 rounded-lg border border-slate-100">
          <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
          <span className="leading-tight">Grounded RAG & Verified Citations</span>
        </div>
      </div>
    </aside>
  );
}
