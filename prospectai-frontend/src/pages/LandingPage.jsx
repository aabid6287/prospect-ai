import React from 'react';
import { Link } from 'react-router-dom';
import {
  FileText,
  Search,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  TrendingUp,
  Shield,
  Layers,
  Database,
} from 'lucide-react';
import Button from '../components/common/Button';

export default function LandingPage() {
  return (
    <div className="space-y-20 pb-20">
      {/* Hero Section */}
      <section className="pt-20 pb-16 px-4 sm:px-6 lg:px-8 max-w-5xl mx-auto text-center space-y-8">
        <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-50 border border-blue-200 text-blue-700 text-xs font-semibold">
          <span className="w-2 h-2 rounded-full bg-blue-600 animate-pulse" />
          <span>Production RAG for IPO Prospectuses</span>
        </div>

        <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold tracking-tight text-slate-900 leading-[1.15]">
          Understand every RHP with <br />
          <span className="text-blue-600">evidence-backed AI</span>
        </h1>

        <p className="text-lg sm:text-xl text-slate-600 max-w-3xl mx-auto font-normal leading-relaxed">
          Upload any 400+ page Red Herring Prospectus. ProspectAI extracts financial tables,
          analyzes risk disclosures, and answers complex queries with exact source page citations.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
          <Link to="/register">
            <Button size="lg" className="w-full sm:w-auto text-base px-6 py-3">
              Start Free Research
              <ArrowRight className="w-4 h-4 ml-2" />
            </Button>
          </Link>
          <Link to="/login">
            <Button variant="secondary" size="lg" className="w-full sm:w-auto text-base px-6 py-3">
              Sign In to Library
            </Button>
          </Link>
        </div>

        {/* Informational Disclaimer Banner */}
        <div className="p-3.5 bg-amber-50/70 border border-amber-200/80 rounded-xl max-w-2xl mx-auto text-xs text-amber-800 flex items-center justify-center gap-2">
          <Shield className="w-4 h-4 shrink-0 text-amber-600" />
          <span>ProspectAI provides informational research assistance and does not provide personalized investment advice.</span>
        </div>
      </section>

      {/* Feature Pillars */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-2xl mx-auto mb-12">
          <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
            Designed for Rigorous Financial Diligence
          </h2>
          <p className="text-sm text-slate-600 mt-2">
            No hallucinations. Every answer is grounded directly in extracted PDF chunks with traceable page numbers.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Card 1 */}
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm space-y-4 hover:border-blue-300 transition-colors">
            <div className="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <CheckCircle2 className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-semibold text-slate-900">Zero Hallucination Grounding</h3>
            <p className="text-sm text-slate-600 leading-relaxed">
              Vector retrieval strictly scoped to your selected RHP. If disclosures are not found in the prospectus, the AI explicitly states so.
            </p>
          </div>

          {/* Card 2 */}
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm space-y-4 hover:border-blue-300 transition-colors">
            <div className="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <Search className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-semibold text-slate-900">Exact Source Page Citations</h3>
            <p className="text-sm text-slate-600 leading-relaxed">
              Every synthesized claim links directly to its source page and excerpt in the prospectus, allowing instant verification.
            </p>
          </div>

          {/* Card 3 */}
          <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm space-y-4 hover:border-blue-300 transition-colors">
            <div className="w-10 h-10 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <h3 className="text-lg font-semibold text-slate-900">Structured Risk & Financials</h3>
            <p className="text-sm text-slate-600 leading-relaxed">
              Automated extraction of promoter holdings, litigation disclosures, use of IPO proceeds, and restated financial summaries.
            </p>
          </div>
        </div>
      </section>

      {/* Tech Pipeline */}
      <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-slate-900 text-white rounded-2xl p-8 sm:p-12 shadow-xl space-y-8">
          <div className="max-w-xl">
            <span className="text-xs font-semibold text-blue-400 uppercase tracking-wider">Enterprise Architecture</span>
            <h3 className="text-2xl sm:text-3xl font-bold mt-1">Enterprise-Grade Spring AI & Pinecone</h3>
            <p className="text-sm text-slate-300 mt-2">
              Powered by Spring Boot 3.4, Java 21, Spring AI PDF Document Reader, and vector search with Pinecone.
            </p>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-4 border-t border-slate-800 text-xs">
            <div className="flex items-center gap-2">
              <FileText className="w-4 h-4 text-blue-400" />
              <span>Spring AI PDF Reader</span>
            </div>
            <div className="flex items-center gap-2">
              <Database className="w-4 h-4 text-blue-400" />
              <span>Pinecone Vector DB</span>
            </div>
            <div className="flex items-center gap-2">
              <Layers className="w-4 h-4 text-blue-400" />
              <span>MySQL Persistence</span>
            </div>
            <div className="flex items-center gap-2">
              <Shield className="w-4 h-4 text-blue-400" />
              <span>JWT Spring Security</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
