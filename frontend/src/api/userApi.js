import api from './axiosConfig';

const BASE_URL = '/emp/employees';

// --- EMPLOYEE MANAGEMENT ENDPOINTS ---

const getAllEmployees = async () => {
    const response = await api.get(BASE_URL);
    console.log("RAW BACKEND RESPONSE:", response.data); // peek at the data

    // Bulletproof extraction: Find the array no matter how it's wrapped
    if (Array.isArray(response.data)) return response.data;
    if (Array.isArray(response.data.data)) return response.data.data;
    if (Array.isArray(response.data.content)) return response.data.content;

    return []; // Return empty array as a safe fallback
};

const updateEmployee = async (employeeId, employeeData) => {
    const response = await api.put(`${BASE_URL}/${employeeId}`, employeeData);
    return response.data;
};

// --- USER ACCOUNT ENDPOINTS ---

/**
 * Update the user's password.
 * @param {number} userId - The ID of the logged-in user
 * @param {object} passwordData - Matches UpdatePasswordDTO { currentPassword, newPassword, confirmNewPassword }
 */
const updatePassword = async (userId, passwordData) => {
    // We explicitly use '/users' here instead of BASE_URL
    const response = await api.put(`/users/update-password?userId=${userId}`, passwordData);
    return response.data;
};

export default {
    getAllEmployees,
    updateEmployee,
    updatePassword
};