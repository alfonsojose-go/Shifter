import axios from 'axios';

// 1. Create the Axios instance
const api = axios.create({
    baseURL: 'http://localhost:8081/api', // Confirm your port matches backend
    headers: {
        'Content-Type': 'application/json',
    },
});

// 2. Request Interceptor: Auto-attach JWT
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// 3. Response Interceptor: Auto-logout on 401 (BUT NOT ON LOGIN FAILURES)
api.interceptors.response.use(
    (response) => response,
    (error) => {
        // Check if this error came from a Login attempt
        const isLoginRequest = error.config && error.config.url.includes('/login');

        // Only redirect if it's 401 AND NOT a login attempt
        if (error.response && error.response.status === 401 && !isLoginRequest) {
            localStorage.removeItem('token');
            localStorage.removeItem('role');
            localStorage.removeItem('user');
            window.location.href = '/login';
        }

        return Promise.reject(error);
    }
);

export default api;