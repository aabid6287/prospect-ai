import React, { useState, useEffect } from 'react';
import { useParams, Link, useSearchParams } from 'react-router-dom';
import documentService from '../services/documentService';
import chatService from '../services/chatService';
import analysisService from '../services/analysisService';
import OverviewPanel from '../components/analysis/OverviewPanel';
import RisksPanel from '../components/analysis/RisksPanel';
import FinancialsPanel from '../components/analysis/FinancialsPanel';
import {
  FileText,
  Send,
  Sparkles,
  BookOpen,
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  ShieldAlert,
  MessageSquare,
  BarChart2,
  FileCheck2,
  Loader2,
  AlertTriangle,
  Briefcase,
} from 'lucide-react';
import StatusBadge from '../components/common/StatusBadge';
import Button from '../components/common/Button';

export default function DocumentWorkspacePage() {
  const { documentId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedTab = searchParams.get('tab');
  const [document, setDocument] = useState(null);
  const [isLoadingDoc, setIsLoadingDoc] = useState(true);
  const [docError, setDocError] = useState('');

  const [activeTab, setActiveTab] = useState(
    ['chat', 'overview', 'financials', 'risks'].includes(requestedTab) ? requestedTab : 'chat'
  );

  useEffect(() => {
    if (requestedTab && ['chat', 'overview', 'financials', 'risks'].includes(requestedTab)) {
      setActiveTab(requestedTab);
    }
  }, [requestedTab]);

  const switchTab = (tab) => {
    setActiveTab(tab);
    setSearchParams({ tab });
  };
  const [currentPage, setCurrentPage] = useState(1);
  const totalPages = document?.pageCount || 524;

  // Analysis State
  const [overviewData, setOverviewData] = useState(null);
  const [overviewLoading, setOverviewLoading] = useState(false);
  const [overviewError, setOverviewError] = useState('');

  const [financialsData, setFinancialsData] = useState(null);
  const [financialsLoading, setFinancialsLoading] = useState(false);
  const [financialsError, setFinancialsError] = useState('');

  const [risksData, setRisksData] = useState(null);
  const [risksLoading, setRisksLoading] = useState(false);
  const [risksError, setRisksError] = useState('');

  // Suggested questions based on PRD
  const SUGGESTED_PROMPTS = [
    'What are the primary risk factors regarding government regulations?',
    'Summarize the restated revenue and EBITDA for FY24 and FY25.',
    'How will the net IPO proceeds be deployed across capital expenditure?',
    'Who are the promoters and what is their pre-IPO shareholding percentage?',
  ];

  const [messages, setMessages] = useState([]);
  const [inputQuestion, setInputQuestion] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedCitation, setSelectedCitation] = useState(null);

  useEffect(() => {
    const fetchDocAndHistory = async () => {
      if (!documentId) return;
      try {
        setIsLoadingDoc(true);
        setDocError('');
        const data = await documentService.getDocument(documentId);
        setDocument(data);
        if (data.pageCount) {
          setCurrentPage(Math.min(1, data.pageCount));
        }

        // Fetch conversation history from backend
        try {
          const history = await chatService.getChatHistory(documentId);
          if (history && history.length > 0) {
            const formatted = [];
            history.forEach((item) => {
              formatted.push({
                id: `u-${item.id}`,
                role: 'user',
                content: item.question,
              });
              formatted.push({
                id: `a-${item.id}`,
                role: 'assistant',
                content: item.answer,
                citations: item.citations?.map((c) => ({
                  page: c.pageNumber,
                  section: c.section,
                  excerpt: c.excerpt,
                })) || [],
              });
            });
            setMessages(formatted);
          } else {
            setMessages([
              {
                id: 'msg-init',
                role: 'assistant',
                content: `Welcome to the research workspace for "${data.companyName || data.originalFilename}". Status: ${data.status}.\n\nAsk questions about risk factors, objects of the offer, promoter shareholding, or financial disclosures.`,
                citations: [],
              },
            ]);
          }
        } catch {
          // If history fetch fails, show welcome greeting
          setMessages([
            {
              id: 'msg-init',
              role: 'assistant',
              content: `Welcome to the research workspace for "${data.companyName || data.originalFilename}". Status: ${data.status}.\n\nAsk questions about risk factors, objects of the offer, promoter shareholding, or financial disclosures.`,
              citations: [],
            },
          ]);
        }
      } catch (err) {
        setDocError(err.message || 'Could not load document details. You may not have permission to view it.');
      } finally {
        setIsLoadingDoc(false);
      }
    };

    fetchDocAndHistory();
  }, [documentId]);

  const handleSend = async (questionText) => {
    const q = (questionText || inputQuestion).trim();
    if (!q || isSubmitting) return;

    const userMsg = { id: `msg-${Date.now()}`, role: 'user', content: q };
    setMessages((prev) => [...prev, userMsg]);
    setInputQuestion('');
    setIsSubmitting(true);

    try {
      const response = await chatService.askQuestion(documentId, q);
      const assistantMsg = {
        id: response.id || `msg-${Date.now() + 1}`,
        role: 'assistant',
        content: response.answer,
        citations: response.citations?.map((c) => ({
          page: c.pageNumber,
          section: c.section,
          excerpt: c.excerpt,
        })) || [],
      };
      setMessages((prev) => [...prev, assistantMsg]);
      if (assistantMsg.citations.length > 0 && assistantMsg.citations[0].page) {
        setCurrentPage(assistantMsg.citations[0].page);
        setSelectedCitation(assistantMsg.citations[0]);
      }
    } catch (err) {
      const errorMsg = {
        id: `msg-err-${Date.now()}`,
        role: 'assistant',
        content: `Unable to process question: ${err.response?.data?.message || err.message || 'An unexpected error occurred. Verify the document is READY.'}`,
        citations: [],
        isError: true,
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    if (!documentId || !document || document.status !== 'READY') return;

    if (activeTab === 'overview' && !overviewData && !overviewLoading) {
      setOverviewLoading(true);
      setOverviewError('');
      analysisService.getOverview(documentId)
        .then((res) => setOverviewData(res))
        .catch((err) => setOverviewError(err.response?.data?.message || err.message || 'Failed to load company overview.'))
        .finally(() => setOverviewLoading(false));
    }

    if (activeTab === 'financials' && !financialsData && !financialsLoading) {
      setFinancialsLoading(true);
      setFinancialsError('');
      analysisService.getFinancials(documentId)
        .then((res) => setFinancialsData(res))
        .catch((err) => setFinancialsError(err.response?.data?.message || err.message || 'Failed to load financial analysis.'))
        .finally(() => setFinancialsLoading(false));
    }

    if (activeTab === 'risks' && !risksData && !risksLoading) {
      setRisksLoading(true);
      setRisksError('');
      analysisService.getRisks(documentId)
        .then((res) => setRisksData(res))
        .catch((err) => setRisksError(err.response?.data?.message || err.message || 'Failed to load risk factor disclosures.'))
        .finally(() => setRisksLoading(false));
    }
  }, [activeTab, documentId, document, overviewData, overviewLoading, financialsData, financialsLoading, risksData, risksLoading]);

  const handleCitationClick = (citation) => {
    if (!citation) return;
    const page = citation.pageNumber || citation.page;
    if (page) {
      setCurrentPage(page);
    }
    setSelectedCitation({
      page: page,
      section: citation.section,
      excerpt: citation.excerpt,
    });
  };

  if (isLoadingDoc) {
    return (
      <div className="h-[calc(100vh-8rem)] flex items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
          <p className="text-xs text-slate-500 font-medium">Opening prospectus research workspace...</p>
        </div>
      </div>
    );
  }

  if (docError) {
    return (
      <div className="max-w-lg mx-auto mt-16 p-6 bg-white rounded-xl border border-rose-200 shadow-sm text-center space-y-4">
        <div className="w-12 h-12 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center mx-auto">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <h2 className="text-lg font-bold text-slate-900">Document Access Denied or Not Found</h2>
        <p className="text-xs text-slate-500">{docError}</p>
        <Link to="/dashboard">
          <Button variant="secondary" size="sm">
            <ArrowLeft className="w-4 h-4 mr-1.5" />
            Back to Library
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="h-[calc(100vh-5rem)] flex flex-col space-y-3 -m-6 p-4 bg-slate-100/70">
      {/* Top Workspace Header */}
      <div className="bg-white px-5 py-3 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between shrink-0">
        <div className="flex items-center gap-3">
          <Link
            to="/dashboard"
            className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div className="h-5 w-px bg-slate-200" />
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base font-bold text-slate-900">
                {document?.companyName || document?.originalFilename}
              </h1>
              <StatusBadge status={document?.status} />
            </div>
            <p className="text-xs text-slate-400 font-mono">
              Document ID: {document?.id} • {document?.originalFilename} • {document?.fileSize ? `${(document.fileSize / 1024 / 1024).toFixed(2)} MB` : ''}
            </p>
          </div>
        </div>

        {/* Tab Switcher */}
        <div className="flex items-center bg-slate-100 p-1 rounded-lg border border-slate-200 text-xs font-medium">
          <button
            onClick={() => switchTab('chat')}
            className={`px-2.5 py-1.5 rounded-md flex items-center gap-1.5 transition-colors ${
              activeTab === 'chat'
                ? 'bg-white text-blue-600 shadow-sm font-semibold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <MessageSquare className="w-3.5 h-3.5" />
            RAG Chat
          </button>
          <button
            onClick={() => switchTab('overview')}
            className={`px-2.5 py-1.5 rounded-md flex items-center gap-1.5 transition-colors ${
              activeTab === 'overview'
                ? 'bg-white text-blue-600 shadow-sm font-semibold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <Briefcase className="w-3.5 h-3.5" />
            Overview
          </button>
          <button
            onClick={() => switchTab('financials')}
            className={`px-2.5 py-1.5 rounded-md flex items-center gap-1.5 transition-colors ${
              activeTab === 'financials'
                ? 'bg-white text-emerald-600 shadow-sm font-semibold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <BarChart2 className="w-3.5 h-3.5" />
            Financials
          </button>
          <button
            onClick={() => switchTab('risks')}
            className={`px-2.5 py-1.5 rounded-md flex items-center gap-1.5 transition-colors ${
              activeTab === 'risks'
                ? 'bg-white text-rose-600 shadow-sm font-semibold'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <ShieldAlert className="w-3.5 h-3.5" />
            Risk Factors
          </button>
        </div>
      </div>

      {/* 3-Panel Main Workspace Body */}
      <div className="flex-1 grid grid-cols-1 lg:grid-cols-12 gap-3 min-h-0 overflow-hidden">
        {/* Left Panel: Document Outline & Metadata (col-span-3) */}
        <div className="hidden lg:flex lg:col-span-3 bg-white rounded-xl border border-slate-200 shadow-sm flex-col overflow-hidden">
          <div className="p-3.5 border-b border-slate-200 bg-slate-50/70 flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
              <BookOpen className="w-3.5 h-3.5 text-blue-600" />
              Prospectus Structure
            </span>
            <span className="text-[11px] text-slate-400">Chapters</span>
          </div>

          <div className="flex-1 overflow-y-auto p-2 space-y-1 text-xs">
            {[
              { title: 'Section I: General & Definitions', page: 1 },
              { title: 'Section II: Risk Factors', page: 12, highlight: true },
              { title: 'Section III: Introduction & Summary', page: 45 },
              { title: 'Section IV: Objects of the Offer', page: 84 },
              { title: 'Section V: About the Company', page: 120 },
              { title: 'Section VI: Financial Statements', page: 210 },
              { title: 'Section VII: Legal & Litigation', page: 340 },
            ].map((section, idx) => (
              <button
                key={idx}
                onClick={() => setCurrentPage(section.page)}
                className={`w-full text-left px-3 py-2 rounded-lg flex items-center justify-between transition-colors ${
                  currentPage === section.page
                    ? 'bg-blue-50 text-blue-700 font-semibold border border-blue-200'
                    : 'text-slate-600 hover:bg-slate-50'
                }`}
              >
                <span className="truncate pr-2">{section.title}</span>
                <span className="text-[10px] text-slate-400 font-mono shrink-0">p. {section.page}</span>
              </button>
            ))}
          </div>

          <div className="p-3 bg-slate-50 border-t border-slate-200 text-[11px] text-slate-500 flex items-center gap-2">
            <FileCheck2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>Document verified & stored on server.</span>
          </div>
        </div>

        {/* Center Panel: Document Content View (col-span-5) */}
        <div className="lg:col-span-5 bg-white rounded-xl border border-slate-200 shadow-sm flex flex-col overflow-hidden">
          {/* Reader Toolbar */}
          <div className="p-3 border-b border-slate-200 bg-slate-50/70 flex items-center justify-between shrink-0">
            <div className="flex items-center gap-2 text-xs text-slate-600">
              <span className="font-semibold text-slate-800">Page {currentPage}</span>
              <span>of {totalPages}</span>
            </div>
            <div className="flex items-center gap-1">
              <button
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                className="p-1 rounded hover:bg-slate-200 text-slate-600"
                title="Previous page"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                className="p-1 rounded hover:bg-slate-200 text-slate-600"
                title="Next page"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Reader Content Excerpt View */}
          <div className="flex-1 overflow-y-auto p-6 font-serif text-sm leading-relaxed text-slate-800 bg-amber-50/20 select-text">
            <div className="max-w-prose mx-auto space-y-4">
              <div className="text-center pb-4 border-b border-slate-200 text-xs font-sans text-slate-400 uppercase tracking-wider">
                {document?.companyName || 'PROSPECTUS'} — PAGE {currentPage}
              </div>

              {selectedCitation ? (
                <div className="p-4 bg-blue-50/90 border border-blue-200 rounded-lg text-xs font-sans space-y-2">
                  <div className="flex items-center justify-between text-blue-800 font-semibold">
                    <span>Supporting Source Citation (Page {selectedCitation.page})</span>
                    <button onClick={() => setSelectedCitation(null)} className="text-blue-600 hover:underline">
                      Clear Highlight
                    </button>
                  </div>
                  <p className="italic text-slate-800 bg-white p-2.5 rounded border border-blue-100">
                    "{selectedCitation.excerpt}"
                  </p>
                </div>
              ) : null}

              <h3 className="font-sans font-bold text-base text-slate-900">
                SECTION II — RISK FACTORS (PAGE {currentPage})
              </h3>
              <p>
                1. <strong>Internal Risk Factor:</strong> Any failure to comply with statutory standards or evolving sector regulations may adversely impact our business operations, cash flows, and credit profile.
              </p>
              <p>
                2. <strong>Market Competition:</strong> We operate in an industry characterized by evolving technology, customer acquisition demands, and pricing sensitivity.
              </p>
            </div>
          </div>
        </div>

        {/* Right Panel: AI RAG Chat or RHP Analysis Panels (col-span-4) */}
        <div className="lg:col-span-4 bg-white rounded-xl border border-slate-200 shadow-sm flex flex-col overflow-hidden">
          {activeTab === 'overview' && (
            <OverviewPanel
              data={overviewData}
              loading={overviewLoading}
              error={overviewError}
              onCitationClick={handleCitationClick}
            />
          )}

          {activeTab === 'financials' && (
            <FinancialsPanel
              data={financialsData}
              loading={financialsLoading}
              error={financialsError}
              onCitationClick={handleCitationClick}
            />
          )}

          {activeTab === 'risks' && (
            <RisksPanel
              data={risksData}
              loading={risksLoading}
              error={risksError}
              onCitationClick={handleCitationClick}
            />
          )}

          {activeTab === 'chat' && (
            <>
              {/* Header */}
              <div className="p-3.5 border-b border-slate-200 bg-slate-50/70 flex items-center justify-between shrink-0">
                <span className="text-xs font-semibold text-slate-800 flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-blue-600" />
                  Document-Grounded AI Chat
                </span>
                <span className="text-[10px] text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                  Pinecone Filtered
                </span>
              </div>

              {/* Messages list */}
              <div className="flex-1 overflow-y-auto p-4 space-y-4 text-xs">
                {messages.map((msg) => (
                  <div
                    key={msg.id}
                    className={`flex flex-col space-y-1.5 ${
                      msg.role === 'user' ? 'items-end' : 'items-start'
                    }`}
                  >
                    <div
                      className={`max-w-[90%] rounded-xl p-3 leading-relaxed ${
                        msg.role === 'user'
                          ? 'bg-blue-600 text-white shadow-sm'
                          : 'bg-slate-100 text-slate-800 border border-slate-200/80'
                      }`}
                    >
                      <p className="whitespace-pre-line">{msg.content}</p>
                    </div>

                    {/* Grounded Citation Chips */}
                    {msg.citations && msg.citations.length > 0 && (
                      <div className="flex flex-wrap gap-1.5 pt-1">
                        {msg.citations.map((cit, cIdx) => (
                          <button
                            key={cIdx}
                            onClick={() => handleCitationClick(cit)}
                            className="inline-flex items-center gap-1 px-2 py-1 rounded bg-blue-50 hover:bg-blue-100 border border-blue-200 text-blue-700 font-medium transition-colors cursor-pointer text-[11px]"
                          >
                            <FileText className="w-3 h-3" />
                            <span>Source: Page {cit.page}</span>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                ))}

                {isSubmitting && (
                  <div className="flex items-center gap-2 text-slate-400 text-xs italic">
                    <div className="w-2 h-2 rounded-full bg-blue-600 animate-ping" />
                    <span>Searching vector store and synthesizing cited answer...</span>
                  </div>
                )}
              </div>

              {/* Suggested Prompts */}
              <div className="px-3 pt-2 pb-1 border-t border-slate-100 bg-slate-50/50">
                <p className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1.5">
                  Suggested Questions
                </p>
                <div className="flex flex-wrap gap-1">
                  {SUGGESTED_PROMPTS.slice(0, 2).map((prompt, pIdx) => (
                    <button
                      key={pIdx}
                      onClick={() => handleSend(prompt)}
                      className="text-left text-[11px] text-slate-600 hover:text-blue-700 bg-white hover:bg-blue-50 border border-slate-200 rounded px-2 py-1 transition-colors truncate max-w-full"
                    >
                      {prompt}
                    </button>
                  ))}
                </div>
              </div>

              {/* Input Box */}
              <div className="p-3 border-t border-slate-200 bg-white shrink-0">
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    handleSend();
                  }}
                  className="flex items-center gap-2"
                >
                  <input
                    type="text"
                    value={inputQuestion}
                    onChange={(e) => setInputQuestion(e.target.value)}
                    placeholder="Ask anything about this RHP..."
                    disabled={isSubmitting}
                    className="flex-1 px-3 py-2 text-xs border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-slate-900"
                  />
                  <button
                    type="submit"
                    disabled={isSubmitting || !inputQuestion.trim()}
                    className="p-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50 transition-colors"
                  >
                    <Send className="w-4 h-4" />
                  </button>
                </form>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
