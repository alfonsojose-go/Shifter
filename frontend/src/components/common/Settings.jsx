import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react'; // <-- Import the icons
import Button from './Button';
import UserApi from '../../api/userApi';

const Settings = () => {
    // --- STATE ---
    const [formData, setFormData] = useState({
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
    });

    // Toggle states for the eye icons
    const [showPassword, setShowPassword] = useState({
        current: false,
        new: false,
        confirm: false
    });

    // UI Feedback state
    const [status, setStatus] = useState({ type: '', message: '' });
    const [isLoading, setIsLoading] = useState(false);

    // --- HANDLERS ---
    const handleInputChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
        if (status.message) setStatus({ type: '', message: '' });
    };

    const togglePasswordVisibility = (field) => {
        setShowPassword(prev => ({ ...prev, [field]: !prev[field] }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // 1. Frontend Validation
        if (!formData.currentPassword || !formData.newPassword || !formData.confirmPassword) {
            setStatus({ type: 'error', message: 'Please fill in all fields.' });
            return;
        }

        if (formData.newPassword !== formData.confirmPassword) {
            setStatus({ type: 'error', message: 'New passwords do not match.' });
            return;
        }

        if (formData.newPassword.length < 8) {
            setStatus({ type: 'error', message: 'New password must be at least 8 characters long.' });
            return;
        }

        // 2. Real Backend API Call
        setIsLoading(true);
        setStatus({ type: '', message: '' });

        try {
            const userId = localStorage.getItem('userId');

            if (!userId) {
                setStatus({ type: 'error', message: 'Authentication error. Please log out and log back in.' });
                setIsLoading(false);
                return;
            }

            const payload = {
                currentPassword: formData.currentPassword,
                newPassword: formData.newPassword,
                confirmNewPassword: formData.confirmPassword
            };

            await UserApi.updatePassword(userId, payload);

            setStatus({ type: 'success', message: 'Password updated successfully!' });

            // Clear the form and reset eye icons to hidden
            setFormData({
                currentPassword: '',
                newPassword: '',
                confirmPassword: ''
            });
            setShowPassword({ current: false, new: false, confirm: false });

        } catch (error) {
            console.error("Failed to update password:", error);
            setStatus({
                type: 'error',
                message: error.response?.data?.message || 'Failed to update password. Please check your current password and try again.'
            });
        } finally {
            setIsLoading(false);
        }
    };

    // --- RENDER ---
    return (
        <div className="space-y-6">
            {/* --- HEADER --- */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">Settings</h1>
                    <p className="text-gray-500">Manage your account and application preferences.</p>
                </div>
            </div>

            {/* --- PASSWORD MANAGEMENT CARD --- */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden max-w-2xl">
                <div className="p-6 md:p-8">
                    <div className="mb-6">
                        <h2 className="text-xl font-bold text-gray-800">Password Management</h2>
                        <p className="text-sm text-gray-500 mt-1">Change your login password here.</p>
                    </div>

                    {/* Success / Error Message Banner */}
                    {status.message && (
                        <div className={`p-4 mb-6 rounded-lg text-sm font-medium ${status.type === 'error' ? 'bg-red-50 text-red-600 border border-red-100' : 'bg-green-50 text-green-600 border border-green-100'}`}>
                            {status.message}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-5">

                        {/* Current Password */}
                        <div className="space-y-1.5">
                            <label className="text-sm font-semibold text-gray-700">Current Password</label>
                            <div className="relative">
                                <input
                                    type={showPassword.current ? "text" : "password"}
                                    name="currentPassword"
                                    value={formData.currentPassword}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-3 pr-10 rounded-lg border border-gray-200 bg-gray-50 outline-none focus:border-blue-500 focus:bg-white transition-colors"
                                />
                                <button
                                    type="button"
                                    onClick={() => togglePasswordVisibility('current')}
                                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 transition-colors"
                                    tabIndex="-1" // Keeps the user from tabbing to the eye icon
                                >
                                    {showPassword.current ? <EyeOff size={18} /> : <Eye size={18} />}
                                </button>
                            </div>
                        </div>

                        {/* New Password */}
                        <div className="space-y-1.5">
                            <label className="text-sm font-semibold text-gray-700">New Password</label>
                            <div className="relative">
                                <input
                                    type={showPassword.new ? "text" : "password"}
                                    name="newPassword"
                                    value={formData.newPassword}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-3 pr-10 rounded-lg border border-gray-200 bg-gray-50 outline-none focus:border-blue-500 focus:bg-white transition-colors"
                                />
                                <button
                                    type="button"
                                    onClick={() => togglePasswordVisibility('new')}
                                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 transition-colors"
                                    tabIndex="-1"
                                >
                                    {showPassword.new ? <EyeOff size={18} /> : <Eye size={18} />}
                                </button>
                            </div>
                        </div>

                        {/* Confirm Password */}
                        <div className="space-y-1.5">
                            <label className="text-sm font-semibold text-gray-700">Confirm New Password</label>
                            <div className="relative">
                                <input
                                    type={showPassword.confirm ? "text" : "password"}
                                    name="confirmPassword"
                                    value={formData.confirmPassword}
                                    onChange={handleInputChange}
                                    className="w-full px-3 py-3 pr-10 rounded-lg border border-gray-200 bg-gray-50 outline-none focus:border-blue-500 focus:bg-white transition-colors"
                                />
                                <button
                                    type="button"
                                    onClick={() => togglePasswordVisibility('confirm')}
                                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600 transition-colors"
                                    tabIndex="-1"
                                >
                                    {showPassword.confirm ? <EyeOff size={18} /> : <Eye size={18} />}
                                </button>
                            </div>
                        </div>

                        <div className="pt-4">
                            <Button
                                type="submit"
                                variant="primary"
                                disabled={isLoading}
                            >
                                {isLoading ? 'Updating Password...' : 'Change Password'}
                            </Button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default Settings;