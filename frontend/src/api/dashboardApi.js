import api from './axiosConfig';

// --- MANAGER ENDPOINTS ---
const getActiveEmployeesCount = async () => {
    const response = await api.get('/dashboard/manager/active-employees');
    return response.data;
};

const getPendingRequestsCount = async () => {
    const response = await api.get('/dashboard/manager/pending/count');
    return response.data;
};

const getOldestPendingRequests = async () => {
    const response = await api.get('/dashboard/manager/pending/oldest');
    return response.data;
};

// --- EMPLOYEE ENDPOINTS ---
const getEmployeePendingCount = async () => {
    const response = await api.get('/dashboard/employee/pending/count');
    return response.data;
};

const getEmployeeRecentRequests = async () => {
    const response = await api.get('/dashboard/employee/recent');
    return response.data;
};

export default {
    getActiveEmployeesCount,
    getPendingRequestsCount,
    getOldestPendingRequests,
    getEmployeePendingCount,
    getEmployeeRecentRequests
};