import React, { useState, useEffect } from 'react';
import { Edit, X, User, AlertCircle, Send } from 'lucide-react';
import Button from '../../components/common/Button';
import EmployeeApi from '../../api/userApi';

const Employees = () => {
    // --- STATE ---
    const [employees, setEmployees] = useState([]);
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);

    // --- STANDARD TOAST STATE ---
    const [toast, setToast] = useState({ show: false, message: '', type: 'success' });

    const showToast = (message, type = 'success') => {
        setToast({ show: true, message, type });
        setTimeout(() => setToast({ show: false, message: '', type: 'success' }), 3000);
    };

    const contractTypesList = [
        'Part Time-Student',
        'Part Time-OWP',
        'Full Time',
        'Part Time'
    ];

    const [selectedEmpId, setSelectedEmpId] = useState('');

    const [formData, setFormData] = useState({
        positionName: '',
        contractTypeName: '',
        skillsCsv: '',
        hourlyWage: ''
    });

    // --- DATA LOADING ---
    const loadEmployees = async () => {
        try {
            const rawData = await EmployeeApi.getAllEmployees();
            const dataArray = Array.isArray(rawData) ? rawData : [];

            const mappedData = dataArray.map(emp => {
                let rawRole = emp.role || emp.roles || 'EMPLOYEE';
                let roleStr = 'EMPLOYEE';

                if (typeof rawRole === 'string') {
                    roleStr = rawRole;
                } else if (Array.isArray(rawRole) && rawRole.length > 0) {
                    roleStr = rawRole[0].name || rawRole[0].authority || rawRole[0];
                } else if (typeof rawRole === 'object') {
                    roleStr = rawRole.name || rawRole.authority || 'EMPLOYEE';
                }

                roleStr = String(roleStr).toUpperCase().replace('ROLE_', '');

                return {
                    id: emp.userId || emp.id || emp.employeeId,
                    fullName: emp.fullName || emp.name || 'Unknown Employee',
                    email: emp.email || 'No Email Provided',
                    role: roleStr,
                    contractType: emp.contractTypeName || emp.contractType || '',
                    position: emp.positionName || emp.position || '',
                    hourlyWage: emp.hourlyWage || emp.wage || '',
                    skills: emp.skillsCsv || (Array.isArray(emp.skills) ? emp.skills.join(', ') : (emp.skills || ''))
                };
            }).filter(emp => emp.role === 'EMPLOYEE');

            setEmployees(mappedData);
        } catch (error) {
            console.error("Failed to load employees:", error);
            showToast("Failed to load employees from the server.", "error");
        }
    };

    useEffect(() => {
        loadEmployees();
    }, []);

    // --- HANDLERS ---
    const handleOpenEditModal = () => {
        setSelectedEmpId('');
        setFormData({ positionName: '', contractTypeName: '', skillsCsv: '', hourlyWage: '' });
        setIsEditModalOpen(true);
    };

    const handleEmployeeSelect = (e) => {
        const empId = e.target.value;
        setSelectedEmpId(empId);

        const emp = employees.find(e => e.id.toString() === empId);
        if (emp) {
            setFormData({
                positionName: emp.position || '',
                contractTypeName: emp.contractType || '',
                skillsCsv: emp.skills || '',
                hourlyWage: emp.hourlyWage || ''
            });
        }
    };

    const handleInputChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSave = async () => {
        if (!selectedEmpId) {
            showToast("Please select an employee first.", "error");
            return;
        }

        try {
            const cleanSkills = formData.skillsCsv
                .split(',')
                .map(s => s.trim())
                .filter(s => s !== '')
                .join(', ');

            const parsedWage = parseFloat(formData.hourlyWage) || 0;

            const updatePayload = {
                positionName: formData.positionName,
                contractTypeName: formData.contractTypeName,
                skillsCsv: cleanSkills,
                hourlyWage: parsedWage
            };

            await EmployeeApi.updateEmployee(selectedEmpId, updatePayload);

            showToast("Employee details updated successfully!", "success");

            // Optimistic UI Update
            setEmployees(prevEmployees => prevEmployees.map(emp => {
                if (emp.id.toString() === selectedEmpId) {
                    return {
                        ...emp,
                        position: formData.positionName,
                        contractType: formData.contractTypeName,
                        skills: cleanSkills,
                        hourlyWage: parsedWage
                    };
                }
                return emp;
            }));

            setIsEditModalOpen(false);

            setTimeout(() => {
                loadEmployees();
            }, 500);

        } catch (error) {
            console.error("Failed to update employee", error);
            showToast("Failed to update employee. Check connection.", "error");
        }
    };

    const selectedEmployeeData = employees.find(e => e.id.toString() === selectedEmpId) || {};

    // --- RENDER ---
    return (
        <div className="space-y-6 relative">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">Employees</h1>
                    <p className="text-gray-500">Manage your team members and their roles.</p>
                </div>
                <Button variant="primary" icon={Edit} onClick={handleOpenEditModal}>
                    Edit Employee
                </Button>
            </div>

            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                <div className="p-6 border-b border-gray-100">
                    <h2 className="text-lg font-bold text-gray-800">Team Members</h2>
                    <p className="text-sm text-gray-500">A list of all employees in your team.</p>
                </div>

                {/* --- UX ENHANCEMENT: Scrollable Body, Sticky Header --- */}
                <div className="overflow-x-auto overflow-y-auto max-h-[75vh] custom-scrollbar bg-white relative">
                    <table className="w-full text-left border-collapse min-w-[900px]">
                        <thead className="sticky top-0 z-40 bg-gray-50 shadow-sm border-b border-gray-200">
                        <tr>
                            <th className="p-4 text-sm font-semibold text-gray-500 bg-gray-50 border-b border-gray-200">Name</th>
                            <th className="p-4 text-sm font-semibold text-gray-500 bg-gray-50 border-b border-gray-200">Email</th>
                            <th className="p-4 text-sm font-semibold text-gray-500 bg-gray-50 border-b border-gray-200">Role</th>
                            <th className="p-4 text-sm font-semibold text-gray-500 bg-gray-50 border-b border-gray-200">Contract Type</th>
                            <th className="p-4 text-sm font-semibold text-gray-500 bg-gray-50 border-b border-gray-200">Position</th>
                            <th className="p-4 text-sm font-semibold text-gray-500 bg-gray-50 border-b border-gray-200">Skills</th>
                            <th className="p-4 text-sm font-semibold text-gray-500 text-right bg-gray-50 border-b border-gray-200">Hourly Wage</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                        {employees.map(emp => (
                            <tr key={emp.id} className="hover:bg-gray-50 transition-colors">
                                <td className="p-4 text-sm text-gray-800 font-medium bg-white">{emp.fullName}</td>
                                <td className="p-4 text-sm text-gray-600 bg-white">{emp.email}</td>
                                <td className="p-4 bg-white">
                                    <span className="px-3 py-1 text-xs font-semibold rounded-full bg-blue-50 text-blue-700 border border-blue-100">
                                        {emp.role}
                                    </span>
                                </td>
                                <td className="p-4 text-sm text-gray-600 bg-white">
                                    {emp.contractType ? (
                                        <span className="px-2 py-1 bg-gray-50 border border-gray-200 rounded text-xs text-gray-600">
                                            {emp.contractType}
                                        </span>
                                    ) : <span className="text-gray-300">-</span>}
                                </td>
                                <td className="p-4 text-sm text-gray-600 bg-white">{emp.position || <span className="text-gray-300">-</span>}</td>
                                <td className="p-4 bg-white">
                                    <div className="flex flex-wrap gap-1">
                                        {emp.skills ? emp.skills.split(',').map((skill, index) => (
                                            <span key={index} className="px-2 py-1 text-xs bg-white border border-gray-200 rounded-md text-gray-600 shadow-sm">
                                                {skill.trim()}
                                            </span>
                                        )) : <span className="text-gray-300">-</span>}
                                    </div>
                                </td>
                                <td className="p-4 text-sm font-medium text-gray-800 text-right bg-white">
                                    {emp.hourlyWage ? `$${parseFloat(emp.hourlyWage).toFixed(2)}` : <span className="text-gray-300">-</span>}
                                </td>
                            </tr>
                        ))}
                        {employees.length === 0 && (
                            <tr>
                                <td colSpan="7" className="p-8 text-center text-gray-500 bg-white">No employees found.</td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* EDIT MODAL */}
            {isEditModalOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in-95 duration-200">
                        <div className="flex items-center justify-between p-6 border-b border-gray-100 bg-gray-50/50">
                            <h3 className="text-xl font-bold text-gray-800">Edit Employee Details</h3>
                            <button onClick={() => setIsEditModalOpen(false)} className="text-gray-400 hover:text-gray-600 transition-colors">
                                <X size={20} />
                            </button>
                        </div>

                        <div className="p-6 space-y-5 max-h-[75vh] overflow-y-auto">
                            <div className="space-y-1.5">
                                <label className="text-sm font-semibold text-gray-700">Select Employee</label>
                                <div className="relative">
                                    <User className="absolute left-3 top-3.5 text-gray-400" size={18} />
                                    <select
                                        className="w-full pl-10 pr-3 py-3 rounded-lg border border-gray-200 bg-white outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-50 transition-all"
                                        value={selectedEmpId}
                                        onChange={handleEmployeeSelect}
                                    >
                                        <option value="" disabled>Choose an employee...</option>
                                        {employees.map(emp => (
                                            <option key={emp.id} value={emp.id}>{emp.fullName}</option>
                                        ))}
                                    </select>
                                </div>
                            </div>

                            <div className="space-y-1.5">
                                <label className="text-sm font-semibold text-gray-700">Email</label>
                                <input
                                    type="text"
                                    value={selectedEmployeeData.email || ''}
                                    readOnly
                                    placeholder="Auto-filled"
                                    className="w-full px-3 py-3 rounded-lg border border-gray-200 bg-gray-100 text-gray-500 outline-none cursor-not-allowed"
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-1.5">
                                    <label className="text-sm font-semibold text-gray-700">Role</label>
                                    <input
                                        type="text"
                                        value={selectedEmployeeData.role || ''}
                                        readOnly
                                        placeholder="Auto-filled"
                                        className="w-full px-3 py-3 rounded-lg border border-gray-200 bg-gray-100 text-gray-500 outline-none cursor-not-allowed"
                                    />
                                </div>
                                <div className="space-y-1.5">
                                    <label className="text-sm font-semibold text-gray-700">Contract Type</label>
                                    <select
                                        name="contractTypeName"
                                        value={formData.contractTypeName}
                                        onChange={handleInputChange}
                                        disabled={!selectedEmpId}
                                        className="w-full px-3 py-3 rounded-lg border border-gray-200 bg-white outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-50 transition-all disabled:bg-gray-50 disabled:cursor-not-allowed"
                                    >
                                        <option value="" disabled>Select a contract...</option>
                                        {contractTypesList.map(contract => (
                                            <option key={contract} value={contract}>
                                                {contract}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            </div>

                            <hr className="border-gray-100" />

                            <div className="space-y-1.5">
                                <label className="text-sm font-semibold text-gray-700">Position</label>
                                <input
                                    type="text"
                                    name="positionName"
                                    value={formData.positionName}
                                    onChange={handleInputChange}
                                    disabled={!selectedEmpId}
                                    placeholder="e.g. Barista"
                                    className="w-full px-3 py-3 rounded-lg border border-gray-200 bg-white outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-50 transition-all disabled:bg-gray-50 disabled:cursor-not-allowed"
                                />
                            </div>

                            <div className="space-y-1.5">
                                <label className="text-sm font-semibold text-gray-700">Skills</label>
                                <input
                                    type="text"
                                    name="skillsCsv"
                                    value={formData.skillsCsv}
                                    onChange={handleInputChange}
                                    disabled={!selectedEmpId}
                                    placeholder="e.g. Cashier, Customer Service"
                                    className="w-full px-3 py-3 rounded-lg border border-gray-200 bg-white outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-50 transition-all disabled:bg-gray-50 disabled:cursor-not-allowed"
                                />
                                <p className="text-xs text-gray-400">Enter a comma-separated list of additional skills.</p>
                            </div>

                            <div className="space-y-1.5">
                                <label className="text-sm font-semibold text-gray-700">Hourly Wage ($)</label>
                                <div className="relative">
                                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                        <span className="text-gray-500 sm:text-sm">$</span>
                                    </div>
                                    <input
                                        type="number"
                                        name="hourlyWage"
                                        step="0.01"
                                        value={formData.hourlyWage}
                                        onChange={handleInputChange}
                                        disabled={!selectedEmpId}
                                        placeholder="0.00"
                                        className="w-full pl-7 pr-3 py-3 rounded-lg border border-gray-200 bg-white outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-50 transition-all disabled:bg-gray-50 disabled:cursor-not-allowed"
                                    />
                                </div>
                            </div>
                        </div>

                        <div className="p-6 flex justify-end gap-3 border-t border-gray-100 bg-gray-50/50">
                            <Button variant="ghost" onClick={() => setIsEditModalOpen(false)}>Cancel</Button>
                            <Button variant="primary" onClick={handleSave} disabled={!selectedEmpId}>Save Changes</Button>
                        </div>
                    </div>
                </div>
            )}

            {/* --- UNIFIED STANDARD TOAST NOTIFICATION --- */}
            {toast.show && (
                <div className="fixed bottom-5 right-5 z-[100] animate-in slide-in-from-right-10 duration-300">
                    <div className={`flex items-center gap-3 px-6 py-4 rounded-lg shadow-2xl border ${
                        toast.type === 'error'
                            ? 'bg-red-50 border-red-200 text-red-800'
                            : 'bg-green-50 border-green-200 text-green-800'
                    }`}>
                        {toast.type === 'error' ? <AlertCircle size={20} /> : <Send size={20} />}
                        <p className="font-semibold text-sm">{toast.message}</p>
                        <button
                            onClick={() => setToast({ ...toast, show: false })}
                            className="ml-4 opacity-50 hover:opacity-100 transition-opacity"
                        >
                            <X size={18} />
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Employees;