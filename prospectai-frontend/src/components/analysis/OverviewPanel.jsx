import React from 'react';
import {
  Building2,
  Briefcase,
  Target,
  Users2,
  FileText,
  AlertCircle,
  Loader2,
  ShieldCheck,
} from 'lucide-react';

export default function OverviewPanel({ data, loading, error, onCitationClick }) {
  if (loading) {
    return (
      <div className="h-full flex flex-col items-center justify-center p-8 text-slate-400">
        <Loader2 className="w-7 h-7 animate-spin text-blue-600 mb-2" />
        <p className="text-xs">Extracting business model & objects of the offer...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6 text-center text-xs text-rose-600 bg-rose-50 rounded-xl m-4 border border-rose-100">
        <AlertCircle className="w-5 h-5 mx-auto mb-1.5" />
        <p className="font-semibold">Unable to load overview analysis</p>
        <p className="text-slate-500 mt-1">{error}</p>
      </div>
    );
  }

  if (!data) return null;

  return (
    <div className="h-full overflow-y-auto p-5 space-y-4 text-xs">
      {/* Header Banner */}
      <div className="p-4 bg-gradient-to-r from-blue-50 to-indigo-50/50 rounded-xl border border-blue-100 flex items-start justify-between">
        <div>
          <span className="text-[10px] font-bold text-blue-600 uppercase tracking-wider">
            Issuer Profile
          </span>
          <h2 className="text-sm font-bold text-slate-900 mt-0.5">{data.companyName}</h2>
          <div className="inline-flex items-center gap-1.5 mt-2 px-2.5 py-1 bg-white rounded-md border border-blue-200 text-blue-800 font-medium text-[11px] shadow-2xs">
            <Building2 className="w-3.5 h-3.5 text-blue-600" />
            <span>{data.industry || 'Technology & Digital Commerce'}</span>
          </div>
        </div>
        <span className="inline-flex items-center gap-1 text-[10px] text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200 font-medium">
          <ShieldCheck className="w-3 h-3" /> Grounded in RHP
        </span>
      </div>

      {/* Business Model Card */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-2">
        <div className="flex items-center gap-2 text-slate-900 font-bold">
          <Briefcase className="w-4 h-4 text-blue-600" />
          <span>Core Business Model & Monetization</span>
        </div>
        <p className="text-slate-600 leading-relaxed">{data.businessModel}</p>
      </div>

      {/* Objects of the Issue Card */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-2">
        <div className="flex items-center gap-2 text-slate-900 font-bold">
          <Target className="w-4 h-4 text-indigo-600" />
          <span>Objects of the Issue (Use of Net Proceeds)</span>
        </div>
        <p className="text-slate-600 leading-relaxed">{data.objectsOfIssue}</p>
      </div>

      {/* Promoters & Shareholding Card */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-2">
        <div className="flex items-center gap-2 text-slate-900 font-bold">
          <Users2 className="w-4 h-4 text-emerald-600" />
          <span>Promoters & Capital Structure</span>
        </div>
        <p className="text-slate-600 leading-relaxed">{data.promotersAndShareholding}</p>
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
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-slate-100 hover:bg-blue-50 border border-slate-200 hover:border-blue-200 text-slate-700 hover:text-blue-700 transition-colors cursor-pointer text-[11px]"
              >
                <FileText className="w-3 h-3 text-blue-600" />
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
