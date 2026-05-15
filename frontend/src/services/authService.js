import api from "../api/axiosConfig";

const AUTH_URL = "/auth";

const login = async (username, password) => {
    try {
        const response = await api.post(`${AUTH_URL}/login`, {
            username,
            password,
        });

        const {
            token,
            username: dbUsername,
            role: dbRole,
            userId,
            employeeId,
        } = response.data;

        if (token) {
            localStorage.setItem("token", token);
        }

        if (dbUsername) {
            localStorage.setItem("username", dbUsername);
        }

        const cleanRole = dbRole
            ? dbRole.replace("ROLE_", "").toUpperCase()
            : "EMPLOYEE";
        localStorage.setItem("role", cleanRole);

        if (userId) {
            localStorage.setItem("userId", String(userId));
        }

        if (employeeId) {
            localStorage.setItem("employeeId", String(employeeId));
        } else if (userId) {
            // fallback if backend does not send employeeId separately
            localStorage.setItem("employeeId", String(userId));
        }

        console.log("LOGIN JSON:", response.data);

        return response.data;
    } catch (error) {
        console.error("Login failed", error);
        throw error;
    }
};

const logout = async () => {
    try {
        await api.post(`${AUTH_URL}/logout`);
    } catch (error) {
        console.error("Logout failed on server", error);
    } finally {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("username");
        localStorage.removeItem("userId");
        localStorage.removeItem("employeeId");
    }
};

const getCurrentUser = () => localStorage.getItem("username");
const getRole = () => localStorage.getItem("role");
const getEmployeeId = () =>
    localStorage.getItem("employeeId") || localStorage.getItem("userId");

export default {
    login,
    logout,
    getCurrentUser,
    getRole,
    getEmployeeId,
};