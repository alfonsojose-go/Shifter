import axios from "axios";

const API = axios.create({
    baseURL: "http://localhost:8081/api",
});

API.interceptors.request.use(config => {
    const token = localStorage.getItem("token");
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

/* ---------- ADMIN ---------- */

export const getAdminStats = () =>
    API.get("/admin/stats").then(res => res.data);

export const getUsers = () =>
    API.get("/admin/users").then(res => res.data);

export const createUser = (userData) =>
    API.post("/admin/users", userData).then(res => res.data);




/* ---------- SCHEDULING ---------- */


export const scheduleAPI = {
    /**
     * Get all availabilities
     */
    async getAllAvailabilities() {
        try {
        const response = await API.get('/employee/availabilities');
        return response.data;
        } catch (error) {
        throw new Error(error.response?.data?.message || 'Failed to fetch availabilities');
        }
    },

    /**
     * Create batch schedules
     */
    async createBatchSchedules(batchData) {
        try {
        const response = await API.post('/scheduling/batch', batchData);
        return response.data;
        } catch (error) {
        throw new Error(error.response?.data?.message || 'Failed to create schedules');
        }
    },

    /**
     * Get all schedules
     */
    async getAllSchedules() {
        try {
        const response = await API.get('/scheduling');
        return response.data;
        } catch (error) {
        throw new Error(error.response?.data?.message || 'Failed to fetch schedules');
        }
    }
};


export default API;