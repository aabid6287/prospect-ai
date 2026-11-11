import api from './api';

export const analysisService = {
  /**
   * Fetch executive summary, business model, and objects of the issue.
   * @param {string} documentId - Document UUID
   * @returns {Promise<Object>} RhpOverviewResponse
   */
  async getOverview(documentId) {
    const response = await api.get(`/analysis/${documentId}/overview`);
    return response.data?.data || response.data;
  },

  /**
   * Fetch categorized internal, external, and litigation risk factors.
   * @param {string} documentId - Document UUID
   * @returns {Promise<Object>} RiskAnalysisResponse
   */
  async getRisks(documentId) {
    const response = await api.get(`/analysis/${documentId}/risks`);
    return response.data?.data || response.data;
  },

  /**
   * Fetch restated financial summary, EBITDA, balance sheet, and metrics.
   * @param {string} documentId - Document UUID
   * @returns {Promise<Object>} FinancialAnalysisResponse
   */
  async getFinancials(documentId) {
    const response = await api.get(`/analysis/${documentId}/financials`);
    return response.data?.data || response.data;
  },
};

export default analysisService;
