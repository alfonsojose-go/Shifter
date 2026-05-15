import api from './axiosConfig'; // Adjust path as needed

const BASE_URL = '/employee/availabilities';

const getAllAvailabilities = async () => {
    const response = await api.get(BASE_URL);
    return response.data.data || response.data; // Unpack the ApiResponse
};

export default {
    getAllAvailabilities
};