import api from './api';

export const documentService = {
  /**
   * Upload an RHP PDF document.
   * @param {File} file - PDF file to upload
   * @param {string} [companyName] - Optional company name
   */
  async upload(file, companyName) {
    const formData = new FormData();
    formData.append('file', file);
    if (companyName) {
      formData.append('companyName', companyName);
    }

    const response = await api.post('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  /**
   * Manually trigger or retry vector ingestion for a document.
   * @param {string} documentId
   */
  async processDocument(documentId) {
    const response = await api.post(`/documents/${documentId}/process`);
    return response.data; // DocumentResponse
  },

  /**
   * List all documents owned by the authenticated user.
   */
  async getDocuments() {
    const response = await api.get('/documents');
    return response.data; // List<DocumentResponse>
  },

  /**
   * Get metadata and processing status for a single document.
   * @param {string} documentId
   */
  async getDocument(documentId) {
    const response = await api.get(`/documents/${documentId}`);
    return response.data; // DocumentResponse
  },

  /**
   * Delete an owned document and its stored files.
   * @param {string} documentId
   */
  async deleteDocument(documentId) {
    const response = await api.delete(`/documents/${documentId}`);
    return response.data;
  },
};

export default documentService;
