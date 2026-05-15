import React, { useState, useEffect } from "react";
import { Calendar, User, Lock, AlertCircle, Eye, EyeOff } from "lucide-react";
import { useNavigate } from "react-router-dom";
import authService from "../services/authService";
import api from "../api/axiosConfig";
import Button from "../components/common/Button";

const Login = () => {
    const navigate = useNavigate();

    useEffect(() => {
        console.log("LOGIN PAGE MOUNTED");
        return () => console.log("LOGIN PAGE UNMOUNTED (Reload detected)");
    }, []);

    const [formData, setFormData] = useState({
        username: "",
        password: "",
    });

    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [isLoading, setIsLoading] = useState(false);

    const handleChange = (e) => {
        setFormData((prev) => ({
            ...prev,
            [e.target.name]: e.target.value,
        }));
    };

    const togglePasswordVisibility = () => {
        setShowPassword((v) => !v);
    };

    const ensureEmployeeId = async (dbUsername) => {
        // If authService already stored it, great
        const existing =
            localStorage.getItem("employeeId") || localStorage.getItem("userId");
        if (existing) return Number(existing);

        // Otherwise, fetch from backend (matches your security matcher: /api/user/*/employee-id)
        try {
            const res = await api.get(`/user/${dbUsername}/employee-id`);

            // support a few possible response shapes
            // ex: { employeeId: 9 } OR { success:true, data: 9 } OR { success:true, data:{employeeId:9}}
            const data = res.data;
            const employeeId =
                data?.employeeId ?? data?.data?.employeeId ?? data?.data ?? data?.id;

            if (employeeId) {
                localStorage.setItem("employeeId", String(employeeId));
                localStorage.setItem("userId", String(employeeId));
                return Number(employeeId);
            }

            console.warn(
                "Could not determine employeeId from /user/{username}/employee-id response:",
                res.data
            );
            return null;
        } catch (e) {
            console.warn("Failed to fetch employeeId:", e);
            return null;
        }
    };

    const handleLogin = async (e) => {
        e.preventDefault();
        console.log("1. Form submit intercepted. Preventing default.");

        setIsLoading(true);
        setError("");

        try {
            // 1) login using your service
            const loginJson = await authService.login(
                formData.username,
                formData.password
            );

            // 2) show login json in console
            console.log("LOGIN JSON:", loginJson);

            // 3) role redirect
            const role = authService.getRole(); // "ADMIN" / "MANAGER" / "EMPLOYEE"
            const dbUsername = authService.getCurrentUser() || formData.username;

            // 4) ensure employeeId exists for availability screen (employeeId == userId in your case)
            await ensureEmployeeId(dbUsername);

            // 5) Navigate
            if (role === "ADMIN") navigate("/admin/dashboard");
            else if (role === "MANAGER") navigate("/manager/dashboard");
            else navigate("/emp/dashboard");
        } catch (err) {
            console.error("Login Error:", err);
            const msg =
                err.response?.data?.error ||
                err.response?.data?.message ||
                "Invalid username or password";
            setError(msg);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
            <div className="bg-white w-full max-w-md rounded-2xl shadow-xl border border-gray-100 overflow-hidden">
                <div className="pt-10 pb-6 px-8 flex flex-col items-center">
                    <div className="bg-orange-100 p-3 rounded-xl mb-4 text-orange-600">
                        <Calendar size={32} strokeWidth={2.5} />
                    </div>
                    <h1 className="text-3xl font-extrabold text-orange-600 tracking-tight mb-1">
                        Shifter
                    </h1>
                    <p className="text-gray-500 text-sm">
                        Welcome Back! Please sign in to continue.
                    </p>
                </div>

                <div className="p-8 pt-0">
                    <form onSubmit={handleLogin} className="space-y-5">
                        {/* Username */}
                        <div className="space-y-1.5">
                            <label
                                htmlFor="username"
                                className="block text-sm font-semibold text-gray-700 ml-1"
                            >
                                Username
                            </label>
                            <div className="relative group">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400 group-focus-within:text-orange-600 transition-colors">
                                    <User size={18} />
                                </div>
                                <input
                                    type="text"
                                    id="username"
                                    name="username"
                                    value={formData.username}
                                    onChange={handleChange}
                                    placeholder="Enter your username"
                                    className="w-full pl-10 pr-4 py-3 rounded-lg border border-gray-200 text-gray-700 bg-gray-50/50 focus:bg-white focus:border-orange-500 focus:ring-2 focus:ring-orange-200 outline-none transition-all duration-200 placeholder-gray-400"
                                    required
                                />
                            </div>
                        </div>

                        {/* Password */}
                        <div className="space-y-1.5">
                            <label
                                htmlFor="password"
                                className="block text-sm font-semibold text-gray-700 ml-1"
                            >
                                Password
                            </label>
                            <div className="relative group">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400 group-focus-within:text-orange-600 transition-colors">
                                    <Lock size={18} />
                                </div>
                                <input
                                    type={showPassword ? "text" : "password"}
                                    id="password"
                                    name="password"
                                    value={formData.password}
                                    onChange={handleChange}
                                    placeholder="••••••••"
                                    className="w-full pl-10 pr-12 py-3 rounded-lg border border-gray-200 text-gray-700 bg-gray-50/50 focus:bg-white focus:border-orange-500 focus:ring-2 focus:ring-orange-200 outline-none transition-all duration-200 placeholder-gray-400"
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={togglePasswordVisibility}
                                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 transition-colors focus:outline-none"
                                >
                                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                                </button>
                            </div>

                            {/* --- THE NEW PASSWORD HINT --- */}
                            <div className="text-[11px] leading-tight text-gray-400 ml-1 mt-2 space-y-1">
                                <p>• For password resets, please contact your administrator.</p>
                            </div>
                        </div>

                        {/* Error */}
                        {error && (
                            <div className="flex items-center gap-2 text-red-600 bg-red-50 p-3 rounded-lg text-sm animate-pulse border border-red-100">
                                <AlertCircle size={16} />
                                <span>{error}</span>
                            </div>
                        )}

                        {/* Submit */}
                        <Button
                            variant="primary"
                            size="lg"
                            type="submit"
                            isLoading={isLoading}
                            className="w-full font-bold shadow-lg shadow-orange-500/20 mt-2"
                        >
                            Sign In
                        </Button>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default Login;