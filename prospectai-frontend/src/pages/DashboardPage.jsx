import React, { useState, useEffect, useRef } from 'react';
import { Link, useLocation } from 'react-router-dom';
import documentService from '../services/documentService';
import {
  FileText,
  Upload,
  Clock,
  CheckCircle2,
  Trash2,
  ExternalLink,
  AlertCircle,
  FileUp,
  Loader2,
  RefreshCw,
  MessageSquare,
  BarChart3,
  Search,
  ArrowRight,
  TrendingUp,
  ShieldAlert,
  Sparkles,
  Briefcase,
  HelpCircle,
} from 'lucide-react';
import Button from '../components/common/Button';
import StatusBadge from '../components/common/StatusBadge';

export default function DashboardPage() {
  const location = useLocation();
  const isDocuments = location.pathname.endsWith('/documents');
  const isChats = location.pathname.endsWith('/chats');
  const isAnalysis = location.pathname.endsWith('/analysis');

  const [documents, setDocuments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedFile, setSelectedFile] = useState(null);
  const [companyName, setCompanyName] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [deletingId, setDeletingId] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const pollingRef = useRef(null);

  const TERMINAL_STATUSES = ['READY', 'FAILED'];

  const hasPending = (docs) =>
    docs.some((d) => !TERMINAL_STATUSES.includes(d.status));

  const fetchDocuments = async () => {
    try {
      setError('');
      const data = await documentService.getDocuments();
      setDocuments(data || []);
    } catch (err) {
      setError(err.message || 'Failed to load your prospectus library.');
    } finally {
      setIsLoading(false);
    }
  };

  // Initial load
  useEffect(() => {
    fetchDocuments();
  }, []);

  // Auto-poll while any document is still being processed
  useEffect(() => {
    const stopPolling = () => {
      if (pollingRef.current) {
        clearInterval(pollingRef.current);
        pollingRef.current = null;
      }
    };

    if (hasPending(documents)) {
      if (!pollingRef.current) {
        pollingRef.current = setInterval(async () => {
          try {
            const data = await documentService.getDocuments();
            const updated = data || [];
            setDocuments(updated);
            if (!hasPending(updated)) {
              stopPolling();
            }
          } catch {
            // Swallow poll errors silently
          }
        }, 3000);
      }
    } else {
      stopPolling();
    }

    return stopPolling;
  }, [documents]);

  const handleFileChange = (e) => {
    setError('');
    const file = e.target.files[0];
    if (file) {
      if (!file.name.toLowerCase().endsWith('.pdf')) {
        setError('Only PDF documents (.pdf) are supported.');
        return;
      }
      if (file.size > 50 * 1024 * 1024) {
        setError('File size exceeds the 50MB limit.');
        return;
      }
      setSelectedFile(file);
      const cleanName = file.name.replace(/\.pdf$/i, '').replace(/[-_]/g, ' ');
      setCompanyName(cleanName);
    }
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!selectedFile) return;

    setError('');
    setSuccessMsg('');
    setIsUploading(true);

    try {
      await documentService.upload(selectedFile, companyName);
      setSuccessMsg(`Document "${selectedFile.name}" uploaded successfully.`);
      setSelectedFile(null);
      setCompanyName('');
      await fetchDocuments();
    } catch (err) {
      setError(err.message || 'Upload failed. Please ensure the file is a valid PDF.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleDelete = async (docId, filename) => {
    if (!window.confirm(`Are you sure you want to delete "${filename}"? This action cannot be undone.`)) {
      return;
    }

    setDeletingId(docId);
    setError('');
    try {
      await documentService.deleteDocument(docId);
      setDocuments(documents.filter((d) => d.id !== docId));
      setSuccessMsg(`Document deleted successfully.`);
    } catch (err) {
      setError(err.message || 'Failed to delete document.');
    } finally {
      setDeletingId(null);
    }
  };

  const filteredDocs = documents.filter((doc) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      (doc.companyName && doc.companyName.toLowerCase().includes(q)) ||
      (doc.originalFilename && doc.originalFilename.toLowerCase().includes(q))
    );
  });

  const readyDocs = filteredDocs.filter((d) => d.status === 'READY');
  const readyCount = documents.filter((d) => d.status === 'READY').length;
  const processingCount = documents.filter((d) => d.status === 'PROCESSING' || d.status === 'UPLOADED').length;

  // ─────────────────────────────────────────────────────────────
  // RENDER: Recent Chats Section (/dashboard/chats)
  // ─────────────────────────────────────────────────────────────
  if (isChats) {
    return (
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">Recent Chats & Research Inquiries</h1>
            <p className="text-sm text-slate-500 mt-1">
              Access conversational research threads grounded in your indexed Red Herring Prospectus filings.
            </p>
          </div>
          <button
            onClick={fetchDocuments}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slate-600 hover:text-slate-900 bg-white border border-slate-200 rounded-lg shadow-sm hover:bg-slate-50 transition-colors w-fit"
            title="Refresh document library"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Refresh</span>
          </button>
        </div>

        {/* Search */}
        <div className="relative max-w-md">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Filter filings by company name or filename..."
            className="w-full pl-9 pr-4 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 shadow-sm"
          />
        </div>

        {isLoading ? (
          <div className="bg-white rounded-xl border border-slate-200 p-12 text-center space-y-3">
            <Loader2 className="w-8 h-8 text-blue-600 animate-spin mx-auto" />
            <p className="text-xs text-slate-500">Loading your conversational filings...</p>
          </div>
        ) : filteredDocs.length === 0 ? (
          <div className="bg-white rounded-xl border border-slate-200 p-12 text-center space-y-3">
            <MessageSquare className="w-10 h-10 text-slate-300 mx-auto" />
            <p className="text-sm font-medium text-slate-700">No chat sessions available</p>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              Upload an IPO Red Herring Prospectus in My Documents to start asking grounded questions with source page citations.
            </p>
            <Link to="/dashboard/documents" className="inline-block mt-2">
              <Button size="sm">Go to My Documents</Button>
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {filteredDocs.map((doc) => {
              const isReady = doc.status === 'READY';
              return (
                <div
                  key={doc.id}
                  className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm hover:border-blue-300 transition-colors flex flex-col justify-between space-y-4"
                >
                  <div className="space-y-3">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
                          <MessageSquare className="w-5 h-5" />
                        </div>
                        <div>
                          <h3 className="font-semibold text-slate-900">{doc.companyName || 'Unknown Company'}</h3>
                          <p className="text-xs text-slate-400 font-mono truncate max-w-[220px]">{doc.originalFilename}</p>
                        </div>
                      </div>
                      <StatusBadge status={doc.status} />
                    </div>

                    <div className="bg-slate-50 rounded-lg p-3 border border-slate-100 text-xs text-slate-600 space-y-1.5">
                      <p className="font-medium text-slate-700 flex items-center gap-1.5">
                        <Sparkles className="w-3.5 h-3.5 text-blue-600" />
                        Suggested Research Prompts:
                      </p>
                      <ul className="list-disc list-inside space-y-0.5 text-slate-500 pl-1">
                        <li>What are the objects of the offer and fund deployment?</li>
                        <li>Summarize internal vs external risk factors.</li>
                        <li>What was the restated EBITDA and net profit in FY24?</li>
                      </ul>
                    </div>
                  </div>

                  <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
                    <span className="text-xs text-slate-400">
                      {doc.pageCount ? `${doc.pageCount} pages indexed` : 'Pending'}
                    </span>
                    {isReady ? (
                      <Link to={`/workspace/${doc.id}?tab=chat`}>
                        <Button size="sm">
                          <MessageSquare className="w-3.5 h-3.5 mr-1.5" />
                          Open Chat
                          <ArrowRight className="w-3.5 h-3.5 ml-1.5" />
                        </Button>
                      </Link>
                    ) : (
                      <span className="text-xs text-amber-600 font-medium">Indexing in progress...</span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    );
  }

  // ─────────────────────────────────────────────────────────────
  // RENDER: RHP Analysis Section (/dashboard/analysis)
  // ─────────────────────────────────────────────────────────────
  if (isAnalysis) {
    return (
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">RHP Financial & Risk Analysis</h1>
            <p className="text-sm text-slate-500 mt-1">
              Source-backed executive overviews, restated financial statements, and categorized risk factors.
            </p>
          </div>
          <button
            onClick={fetchDocuments}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slate-600 hover:text-slate-900 bg-white border border-slate-200 rounded-lg shadow-sm hover:bg-slate-50 transition-colors w-fit"
            title="Refresh document library"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Refresh</span>
          </button>
        </div>

        {/* Search */}
        <div className="relative max-w-md">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Filter filings by company name or filename..."
            className="w-full pl-9 pr-4 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 shadow-sm"
          />
        </div>

        {isLoading ? (
          <div className="bg-white rounded-xl border border-slate-200 p-12 text-center space-y-3">
            <Loader2 className="w-8 h-8 text-blue-600 animate-spin mx-auto" />
            <p className="text-xs text-slate-500">Loading prospectus filings for analysis...</p>
          </div>
        ) : filteredDocs.length === 0 ? (
          <div className="bg-white rounded-xl border border-slate-200 p-12 text-center space-y-3">
            <BarChart3 className="w-10 h-10 text-slate-300 mx-auto" />
            <p className="text-sm font-medium text-slate-700">No filings available for analysis</p>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              Upload an IPO Red Herring Prospectus to unlock structured financial and risk analytics.
            </p>
            <Link to="/dashboard/documents" className="inline-block mt-2">
              <Button size="sm">Go to My Documents</Button>
            </Link>
          </div>
        ) : (
          <div className="space-y-4">
            {filteredDocs.map((doc) => {
              const isReady = doc.status === 'READY';
              return (
                <div
                  key={doc.id}
                  className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm hover:border-blue-300 transition-colors space-y-4"
                >
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                        <BarChart3 className="w-5 h-5" />
                      </div>
                      <div>
                        <h3 className="text-base font-bold text-slate-900">{doc.companyName || 'Unknown Company'}</h3>
                        <p className="text-xs text-slate-400 font-mono">
                          {doc.originalFilename} • {doc.pageCount ? `${doc.pageCount} pages` : 'Pending'}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <StatusBadge status={doc.status} />
                      {isReady && (
                        <Link to={`/workspace/${doc.id}?tab=overview`}>
                          <Button size="sm">
                            Launch Full Analysis
                            <ArrowRight className="w-3.5 h-3.5 ml-1.5" />
                          </Button>
                        </Link>
                      )}
                    </div>
                  </div>

                  {isReady ? (
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2">
                      <Link
                        to={`/workspace/${doc.id}?tab=overview`}
                        className="p-3.5 rounded-lg border border-slate-200 hover:border-blue-400 hover:bg-blue-50/40 transition-colors group flex items-start gap-3"
                      >
                        <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 group-hover:bg-blue-100">
                          <Briefcase className="w-4 h-4" />
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-slate-900 group-hover:text-blue-700">Executive Overview</p>
                          <p className="text-[11px] text-slate-500 mt-0.5">Business model, platforms & proceeds deployment</p>
                        </div>
                      </Link>

                      <Link
                        to={`/workspace/${doc.id}?tab=financials`}
                        className="p-3.5 rounded-lg border border-slate-200 hover:border-emerald-400 hover:bg-emerald-50/40 transition-colors group flex items-start gap-3"
                      >
                        <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 group-hover:bg-emerald-100">
                          <TrendingUp className="w-4 h-4" />
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-slate-900 group-hover:text-emerald-700">Financial Information</p>
                          <p className="text-[11px] text-slate-500 mt-0.5">Restated revenue, EBITDA, debt & IPO fresh issue</p>
                        </div>
                      </Link>

                      <Link
                        to={`/workspace/${doc.id}?tab=risks`}
                        className="p-3.5 rounded-lg border border-slate-200 hover:border-rose-400 hover:bg-rose-50/40 transition-colors group flex items-start gap-3"
                      >
                        <div className="w-8 h-8 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center shrink-0 group-hover:bg-rose-100">
                          <ShieldAlert className="w-4 h-4" />
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-slate-900 group-hover:text-rose-700">Risk Factors</p>
                          <p className="text-[11px] text-slate-500 mt-0.5">Internal losses, SEBI compliance & pending litigation</p>
                        </div>
                      </Link>
                    </div>
                  ) : (
                    <div className="p-4 rounded-lg bg-slate-50 text-xs text-slate-500 border border-slate-100">
                      This prospectus is currently undergoing page-level text extraction and Pinecone vector indexing. Analysis will become active once status reaches READY.
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    );
  }

  // ─────────────────────────────────────────────────────────────
  // RENDER: My Documents (/dashboard/documents) & Default Dashboard (/dashboard)
  // ─────────────────────────────────────────────────────────────
  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            {isDocuments ? 'My Documents Library' : 'Prospectus Research Library'}
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            {isDocuments
              ? 'Manage uploaded Red Herring Prospectus filings, inspect indexing status, and add new prospectuses.'
              : 'Upload, index, and analyze Red Herring Prospectus filings with document-grounded AI.'}
          </p>
        </div>
        <button
          onClick={fetchDocuments}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slate-600 hover:text-slate-900 bg-white border border-slate-200 rounded-lg shadow-sm hover:bg-slate-50 transition-colors w-fit"
          title="Refresh document library"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          <span>Refresh</span>
        </button>
      </div>

      {/* Alerts */}
      {error && (
        <div className="p-3.5 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-700 flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {successMsg && (
        <div className="p-3.5 rounded-lg bg-emerald-50 border border-emerald-200 text-xs text-emerald-700 flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Documents</p>
            <p className="text-2xl font-bold text-slate-900 mt-1">{documents.length}</p>
          </div>
          <div className="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
            <FileText className="w-5 h-5" />
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Indexed & Ready</p>
            <p className="text-2xl font-bold text-emerald-600 mt-1">{readyCount}</p>
          </div>
          <div className="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <CheckCircle2 className="w-5 h-5" />
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Uploaded / Processing</p>
            <p className="text-2xl font-bold text-amber-600 mt-1">{processingCount}</p>
          </div>
          <div className="w-10 h-10 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
            <Clock className="w-5 h-5" />
          </div>
        </div>
      </div>

      {/* Upload Box */}
      <div className="bg-white p-6 rounded-xl border border-dashed border-slate-300 hover:border-blue-400 transition-colors shadow-sm">
        <form onSubmit={handleUpload} className="max-w-xl mx-auto text-center space-y-4">
          <div className="w-12 h-12 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center mx-auto">
            <FileUp className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-base font-semibold text-slate-900">Upload Red Herring Prospectus (PDF)</h3>
            <p className="text-xs text-slate-500 mt-1">
              Supports standard SEBI / IPO RHP filings up to 50MB. Text and page metadata will be extracted for RAG.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
            <label className="cursor-pointer inline-flex items-center px-4 py-2 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 bg-white hover:bg-slate-50 transition-colors shadow-sm">
              <span>{selectedFile ? selectedFile.name : 'Choose PDF File'}</span>
              <input type="file" accept="application/pdf" onChange={handleFileChange} className="hidden" />
            </label>

            {selectedFile && (
              <input
                type="text"
                value={companyName}
                onChange={(e) => setCompanyName(e.target.value)}
                placeholder="Company Name (e.g. Swiggy Ltd)"
                className="px-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900"
              />
            )}

            {selectedFile && (
              <Button type="submit" isLoading={isUploading}>
                <Upload className="w-4 h-4 mr-1.5" />
                Upload & Ingest
              </Button>
            )}
          </div>
        </form>
      </div>

      {/* Documents Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <h2 className="text-base font-semibold text-slate-900">
            {isDocuments ? 'All Filings' : 'Your Prospectus Library'}
          </h2>
          <div className="flex items-center gap-3">
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search filings..."
                className="pl-8 pr-3 py-1 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-slate-900 w-44"
              />
            </div>
            <span className="text-xs text-slate-500 whitespace-nowrap">{filteredDocs.length} filings</span>
          </div>
        </div>

        {isLoading ? (
          <div className="p-12 text-center space-y-3">
            <Loader2 className="w-8 h-8 text-blue-600 animate-spin mx-auto" />
            <p className="text-xs text-slate-500">Loading document library...</p>
          </div>
        ) : filteredDocs.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <FileText className="w-10 h-10 text-slate-300 mx-auto" />
            <p className="text-sm font-medium text-slate-600">No prospectuses found</p>
            <p className="text-xs text-slate-400">
              {searchQuery ? 'No documents match your filter query.' : 'Upload your first RHP above to begin research.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50/80 text-xs font-semibold text-slate-500 uppercase tracking-wider border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3">Company / Filename</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Size</th>
                  <th className="px-6 py-3">Pages</th>
                  <th className="px-6 py-3">Uploaded</th>
                  <th className="px-6 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredDocs.map((doc) => (
                  <tr key={doc.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
                          <FileText className="w-4 h-4" />
                        </div>
                        <div>
                          <p className="font-semibold text-slate-900">{doc.companyName || 'Unknown Company'}</p>
                          <p className="text-xs text-slate-400 font-mono">{doc.originalFilename}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <StatusBadge status={doc.status} />
                        {(doc.status === 'PROCESSING' || doc.status === 'UPLOADED') && (
                          <span className="inline-flex items-center gap-1 text-[10px] font-medium text-amber-700 bg-amber-50 border border-amber-200 rounded-full px-2 py-0.5 animate-pulse">
                            <Loader2 className="w-2.5 h-2.5 animate-spin" />
                            Indexing…
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-600 font-mono">
                      {doc.fileSize ? `${(doc.fileSize / 1024 / 1024).toFixed(2)} MB` : '—'}
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-600">
                      {doc.pageCount ? `${doc.pageCount} pages` : 'Pending'}
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-500">
                      {new Date(doc.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Link to={`/workspace/${doc.id}`}>
                          <Button variant="secondary" size="sm">
                            Research
                            <ExternalLink className="w-3.5 h-3.5 ml-1.5" />
                          </Button>
                        </Link>
                        <button
                          onClick={() => handleDelete(doc.id, doc.originalFilename)}
                          disabled={deletingId === doc.id}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
                          title="Delete document"
                        >
                          {deletingId === doc.id ? (
                            <Loader2 className="w-4 h-4 animate-spin text-rose-600" />
                          ) : (
                            <Trash2 className="w-4 h-4" />
                          )}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
