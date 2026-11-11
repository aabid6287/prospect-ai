import React from 'react';
import {
  BarChart2,
  TrendingUp,
  DollarSign,
  FileText,
  Loader2,
  AlertCircle,
  Coins,
  ShieldCheck,
} from 'lucide-react';

export default function FinancialsPanel({ data, loading, error, onCitationClick }) {
  if (loading) {
    return (
      <div className="h-full flex flex-col items-center justify-center p-8 text-slate-400">
        <Loader2 className="w-7 h-7 animate-spin text-emerald-600 mb-2" />
        <p className="text-xs">Extracting restated financial statements and key metrics...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6 text-center text-xs text-rose-600 bg-rose-50 rounded-xl m-4 border border-rose-100">
        <AlertCircle className="w-5 h-5 mx-auto mb-1.5" />
        <p className="font-semibold">Unable to load financial analysis</p>
        <p className="text-slate-500 mt-1">{error}</p>
      </div>
    );
  }

  if (!data) return null;

  return (
    <div className="h-full overflow-y-auto p-5 space-y-4 text-xs">
      {/* Header Banner */}
      <div className="p-4 bg-gradient-to-r from-emerald-50 to-teal-50/50 rounded-xl border border-emerald-100 flex items-start justify-between">
        <div>
          <span className="text-[10px] font-bold text-emerald-600 uppercase tracking-wider">
            Financial Analysis
          </span>
          <h2 className="text-sm font-bold text-slate-900 mt-0.5">{data.companyName}</h2>
          <p className="text-slate-600 mt-1 text-[11px] leading-relaxed">
            Consolidated restated summary of financial performance and capital structure.
          </p>
        </div>
        <span className="inline-flex items-center gap-1 text-[10px] text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200 font-semibold shrink-0">
          <ShieldCheck className="w-3 h-3" /> Section VI Data
        </span>
      </div>

      {/* Structured Metrics Table */}
      {data.keyMetrics && data.keyMetrics.length > 0 && (
        <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
          <div className="p-3 bg-slate-50/80 border-b border-slate-200 flex items-center justify-between">
            <span className="font-bold text-slate-800 flex items-center gap-1.5">
              <Coins className="w-4 h-4 text-emerald-600" />
              Restated Financial Performance Indicators
            </span>
            <span className="text-[10px] text-slate-400 font-mono">Source: Audited Prospectus Tables</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-100 text-[10px] text-slate-400 uppercase tracking-wider bg-slate-50/40">
                  <th className="py-2.5 px-3 font-semibold">Financial Metric</th>
                  <th className="py-2.5 px-3 font-semibold">Period</th>
                  <th className="py-2.5 px-3 font-semibold text-right">Value</th>
                  <th className="py-2.5 px-3 font-semibold">Unit</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                {data.keyMetrics.map((m, idx) => (
                  <tr key={idx} className="hover:bg-slate-50/60 transition-colors">
                    <td className="py-2.5 px-3 font-sans font-medium text-slate-800">
                      {m.metricName}
                      {m.sourceNote && (
                        <p className="text-[10px] text-slate-400 font-normal mt-0.5">{m.sourceNote}</p>
                      )}
                    </td>
                    <td className="py-2.5 px-3 text-slate-600">{m.period}</td>
                    <td className={`py-2.5 px-3 text-right font-bold ${
                      m.value.startsWith('-') ? 'text-rose-600' : 'text-slate-900'
                    }`}>
                      {m.value}
                    </td>
                    <td className="py-2.5 px-3 text-slate-500 font-sans text-[10px]">{m.unit}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Revenue Trend Card */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-2">
        <div className="flex items-center gap-2 text-slate-900 font-bold">
          <TrendingUp className="w-4 h-4 text-emerald-600" />
          <span>Revenue Trend & Trajectory</span>
        </div>
        <p className="text-slate-600 leading-relaxed">{data.revenueTrend}</p>
      </div>

      {/* Profitability & EBITDA Card */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-2">
        <div className="flex items-center gap-2 text-slate-900 font-bold">
          <BarChart2 className="w-4 h-4 text-indigo-600" />
          <span>Operating Profitability & EBITDA Margin</span>
        </div>
        <p className="text-slate-600 leading-relaxed">{data.profitabilityAndEbitda}</p>
      </div>

      {/* Capital Structure & Debt */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-2">
        <div className="flex items-center gap-2 text-slate-900 font-bold">
          <DollarSign className="w-4 h-4 text-amber-600" />
          <span>Capital Structure & Total Borrowings</span>
        </div>
        <p className="text-slate-600 leading-relaxed">{data.capitalStructureAndDebt}</p>
      </div>

      {/* Supporting Citations */}
      {data.citations && data.citations.length > 0 && (
        <div className="pt-2 border-t border-slate-200">
          <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">
            Verified Source Pages
          </p>
          <div className="flex flex-wrap gap-1.5">
            {data.citations.map((cit, idx) => (
              <button
                key={idx}
                onClick={() => onCitationClick && onCitationClick(cit)}
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-slate-100 hover:bg-emerald-50 border border-slate-200 hover:border-emerald-200 text-slate-700 hover:text-emerald-700 transition-colors cursor-pointer text-[11px]"
              >
                <FileText className="w-3 h-3 text-emerald-600" />
                <span>Page {cit.pageNumber}</span>
                <span className="text-[10px] text-slate-400 font-normal">({cit.section})</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Disclaimer */}
      {data.disclaimer && (
        <p className="text-[10px] text-slate-400 italic pt-2 border-t border-slate-100 leading-normal">
          {data.disclaimer}
        </p>
      )}
    </div>
  );
}
