import api from './api';

export const authService = {
  /**
   * Register a new user account.
   * @param {Object} data - { name, email, password }
   */
  async register(data) {
    const response = await api.post('/auth/register', data);
    return response.data; // AuthResponse: { token, id, name, email, role }
  },

  /**
   * Authenticate user with credentials.
   * @param {Object} data - { email, password }
   */
  async login(data) {
    const response = await api.post('/auth/login', data);
    return response.data; // AuthResponse: { token, id, name, email, role }
  },

  /**
   * Fetch current authenticated user profile.
   */
  async getCurrentUser() {
    const response = await api.get('/users/me');
    return response.data; // UserResponse: { id, name, email, role, createdAt }
  },
};

export default authService;
