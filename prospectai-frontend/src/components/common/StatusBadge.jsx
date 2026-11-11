import React from 'react';

const STATUS_CONFIG = {
  READY: {
    bg: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    dot: 'bg-emerald-500',
    label: 'Ready',
  },
  PROCESSING: {
    bg: 'bg-amber-50 text-amber-700 border-amber-200',
    dot: 'bg-amber-500 animate-pulse',
    label: 'Processing',
  },
  UPLOADED: {
    bg: 'bg-blue-50 text-blue-700 border-blue-200',
    dot: 'bg-blue-500',
    label: 'Uploaded',
  },
  FAILED: {
    bg: 'bg-rose-50 text-rose-700 border-rose-200',
    dot: 'bg-rose-500',
    label: 'Failed',
  },
  DELETING: {
    bg: 'bg-slate-50 text-slate-700 border-slate-200',
    dot: 'bg-slate-400',
    label: 'Deleting',
  },
};

export default function StatusBadge({ status }) {
  const config = STATUS_CONFIG[status] || {
    bg: 'bg-slate-100 text-slate-700 border-slate-200',
    dot: 'bg-slate-400',
    label: status || 'Unknown',
  };

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium border ${config.bg}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${config.dot}`} />
      {config.label}
    </span>
  );
}
