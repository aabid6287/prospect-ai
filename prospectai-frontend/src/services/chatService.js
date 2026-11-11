import api from './api';

export const chatService = {
  /**
   * Ask a question about a specific uploaded RHP document.
   * @param {string} documentId - The target document UUID
   * @param {string} question - The user's query
   * @param {string} [conversationId] - Optional conversation tracking ID
   * @returns {Promise<Object>} ChatResponse containing answer and citations
   */
  async askQuestion(documentId, question, conversationId) {
    const payload = { question, conversationId };
    const response = await api.post(`/chat/${documentId}`, payload);
    return response.data?.data || response.data;
  },

  /**
   * Fetch historical Q&A interactions for a specific document.
   * @param {string} documentId - The target document UUID
   * @returns {Promise<Array>} List of ChatResponse objects
   */
  async getChatHistory(documentId) {
    const response = await api.get(`/chat/${documentId}/history`);
    return response.data?.data || response.data || [];
  },
};

export default chatService;
