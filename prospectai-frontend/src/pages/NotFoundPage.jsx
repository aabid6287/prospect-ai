import React from 'react';
import { Link } from 'react-router-dom';
import { FileQuestion, ArrowLeft } from 'lucide-react';
import Button from '../components/common/Button';

export default function NotFoundPage() {
  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center text-center px-4 space-y-4">
      <div className="w-12 h-12 rounded-xl bg-slate-100 text-slate-500 flex items-center justify-center">
        <FileQuestion className="w-6 h-6" />
      </div>
      <h1 className="text-2xl font-bold text-slate-900">Page Not Found</h1>
      <p className="text-sm text-slate-500 max-w-sm">
        The requested page or document could not be located.
      </p>
      <Link to="/dashboard">
        <Button variant="secondary" size="sm">
          <ArrowLeft className="w-4 h-4 mr-1.5" />
          Back to Dashboard
        </Button>
      </Link>
    </div>
  );
}
