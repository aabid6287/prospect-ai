import React from 'react';
import {
  ShieldAlert,
  Scale,
  FileText,
  Loader2,
  AlertCircle,
  TrendingDown,
  Globe,
} from 'lucide-react';

export default function RisksPanel({ data, loading, error, onCitationClick }) {
  if (loading) {
    return (
      <div className="h-full flex flex-col items-center justify-center p-8 text-slate-400">
        <Loader2 className="w-7 h-7 animate-spin text-rose-600 mb-2" />
        <p className="text-xs">Extracting Section II risk factors & litigation records...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6 text-center text-xs text-rose-600 bg-rose-50 rounded-xl m-4 border border-rose-100">
        <AlertCircle className="w-5 h-5 mx-auto mb-1.5" />
        <p className="font-semibold">Unable to load risk analysis</p>
        <p className="text-slate-500 mt-1">{error}</p>
      </div>
    );
  }

  if (!data) return null;

  return (
    <div className="h-full overflow-y-auto p-5 space-y-4 text-xs">
      {/* Risk Summary Header */}
      <div className="p-4 bg-gradient-to-r from-rose-50 to-amber-50/50 rounded-xl border border-rose-100 flex items-start justify-between">
        <div>
          <span className="text-[10px] font-bold text-rose-600 uppercase tracking-wider">
            Risk Factor Disclosures
          </span>
          <h2 className="text-sm font-bold text-slate-900 mt-0.5">{data.companyName}</h2>
          <p className="text-slate-600 mt-1 text-[11px] leading-relaxed">
            {data.internalRisksSummary}
          </p>
        </div>
        <span className="inline-flex items-center gap-1 text-[10px] text-rose-700 bg-rose-50 px-2 py-0.5 rounded-full border border-rose-200 font-semibold shrink-0">
          <ShieldAlert className="w-3 h-3" /> Section II Analysis
        </span>
      </div>

      {/* Internal Risks Card */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-3">
        <div className="flex items-center gap-2 text-slate-900 font-bold border-b border-slate-100 pb-2">
          <TrendingDown className="w-4 h-4 text-rose-600" />
          <span>Internal Risk Factors (Operational & Financial)</span>
        </div>
        <ul className="space-y-2">
          {data.keyInternalRisks?.map((risk, idx) => (
            <li key={idx} className="flex items-start gap-2.5 text-slate-700 leading-relaxed">
              <span className="w-4 h-4 rounded-full bg-rose-100 text-rose-700 font-bold text-[10px] flex items-center justify-center shrink-0 mt-0.5">
                {idx + 1}
              </span>
              <span>{risk}</span>
            </li>
          ))}
        </ul>
      </div>

      {/* External Risks Card */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-3">
        <div className="flex items-center gap-2 text-slate-900 font-bold border-b border-slate-100 pb-2">
          <Globe className="w-4 h-4 text-amber-600" />
          <span>External & Market Risk Factors</span>
        </div>
        <p className="text-slate-600 text-[11px] italic">{data.externalRisksSummary}</p>
        <ul className="space-y-2">
          {data.keyExternalRisks?.map((risk, idx) => (
            <li key={idx} className="flex items-start gap-2.5 text-slate-700 leading-relaxed">
              <span className="w-4 h-4 rounded-full bg-amber-100 text-amber-800 font-bold text-[10px] flex items-center justify-center shrink-0 mt-0.5">
                {idx + 1}
              </span>
              <span>{risk}</span>
            </li>
          ))}
        </ul>
      </div>

      {/* Outstanding Litigation Card */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-2">
        <div className="flex items-center gap-2 text-slate-900 font-bold">
          <Scale className="w-4 h-4 text-indigo-600" />
          <span>Legal & Material Outstanding Litigation</span>
        </div>
        <p className="text-slate-600 leading-relaxed">{data.litigationSummary}</p>
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
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-slate-100 hover:bg-rose-50 border border-slate-200 hover:border-rose-200 text-slate-700 hover:text-rose-700 transition-colors cursor-pointer text-[11px]"
              >
                <FileText className="w-3 h-3 text-rose-600" />
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
