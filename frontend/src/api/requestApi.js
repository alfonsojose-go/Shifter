import api from './axiosConfig';

/**
 * Fetch all employee requests for the manager.
 */
const getAllRequests = async () => {
    const response = await api.get('/requests');
    return response.data;
};

/**
 * Approve or Reject a request.
 * @param {number} id - The request ID
 * @param {string} status - 'APPROVED' or 'REJECTED'
 */
const updateRequestStatus = async (id, status) => {
    const response = await api.put(`/requests/${id}/status?status=${status}`);
    return response.data;
};

export default {
    getAllRequests,
    updateRequestStatus
};