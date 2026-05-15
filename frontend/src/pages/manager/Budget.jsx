import React, { useState, useEffect, forwardRef, useCallback } from 'react';
import { Calendar, AlertCircle, X, Send } from 'lucide-react';
import DatePicker from 'react-datepicker';
import "react-datepicker/dist/react-datepicker.css";
import { format, startOfWeek, endOfWeek, addDays } from 'date-fns';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import BudgetApi from '../../api/budgetApi';
import Button from '../../components/common/Button';

const Budget = () => {
    // --- STRICT DATE ENFORCEMENT ---
    const today = new Date();
    const currentWeekStart = startOfWeek(today, { weekStartsOn: 0 });

    const [startDate, setStartDate] = useState(currentWeekStart);

    // --- DATA STATE ---
    const [scheduledCosts, setScheduledCosts] = useState({
        SUNDAY: 0, MONDAY: 0, TUESDAY: 0, WEDNESDAY: 0, THURSDAY: 0, FRIDAY: 0, SATURDAY: 0
    });

    const [budgets, setBudgets] = useState({
        SUNDAY: 0, MONDAY: 0, TUESDAY: 0, WEDNESDAY: 0, THURSDAY: 0, FRIDAY: 0, SATURDAY: 0
    });

    const [totals, setTotals] = useState({ scheduled: 0, budgeted: 0 });
    const [isLoading, setIsLoading] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    // --- TOAST STATE ---
    const [toast, setToast] = useState({ show: false, message: '', type: 'success' });

    const showToast = (message, type = 'success') => {
        setToast({ show: true, message, type });
        setTimeout(() => setToast({ show: false, message: '', type: 'success' }), 3000);
    };

    // --- HELPERS ---
    const daysOrder = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
    const shortDays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

    // --- DATA LOADING ---
    const loadBudgetData = useCallback(async () => {
        if (!startDate) return;

        setIsLoading(true);
        try {
            const formattedDate = format(startDate, 'yyyy-MM-dd');
            const response = await BudgetApi.getWeeklyLaborCost(formattedDate);

            const data = response.data || response;

            const newBudgets = { SUNDAY: 0, MONDAY: 0, TUESDAY: 0, WEDNESDAY: 0, THURSDAY: 0, FRIDAY: 0, SATURDAY: 0 };
            const newScheduled = { SUNDAY: 0, MONDAY: 0, TUESDAY: 0, WEDNESDAY: 0, THURSDAY: 0, FRIDAY: 0, SATURDAY: 0 };
            let totalB = 0;
            let totalS = 0;

            if (Array.isArray(data)) {
                data.forEach(row => {
                    const day = (row.dayOfWeek || row.day_of_week || '').toUpperCase();
                    if (newBudgets[day] !== undefined) {
                        const bAmount = parseFloat(row.budgetAmount || row.budget_amount || row.budget || 0);
                        const sAmount = parseFloat(row.scheduledCost || row.scheduled_cost || 0);

                        newBudgets[day] = bAmount;
                        newScheduled[day] = sAmount;

                        totalB += bAmount;
                        totalS += sAmount;
                    }
                });
            } else {
                Object.assign(newBudgets, data.dailyBudget || {});
                Object.assign(newScheduled, data.dailyScheduledCost || {});
                totalB = data.totalBudget || Object.values(newBudgets).reduce((a,b)=>a+b,0);
                totalS = data.totalScheduled || Object.values(newScheduled).reduce((a,b)=>a+b,0);
            }

            setBudgets(newBudgets);
            setScheduledCosts(newScheduled);
            setTotals({ scheduled: totalS, budgeted: totalB });

        } catch (error) {
            console.error("Failed to load budget data:", error);
            showToast("Failed to load budget data. Check connection.", "error");
        } finally {
            setIsLoading(false);
        }
    }, [startDate]);

    useEffect(() => {
        loadBudgetData();
    }, [loadBudgetData]);


    // --- HANDLERS ---
    const handleBudgetChange = (dayString, value) => {
        const numValue = parseFloat(value) || 0;
        setBudgets(prev => ({ ...prev, [dayString]: numValue }));
    };

    const handleSave = async () => {
        setIsSaving(true);
        try {
            const updatePromises = daysOrder.map((dayString, index) => {
                const specificDate = addDays(startDate, index);
                const formattedDate = format(specificDate, 'yyyy-MM-dd');
                const amount = budgets[dayString] || 0;

                // Using existing API structure based on your provided file
                return BudgetApi.updateBudget(formattedDate, amount);
            });

            await Promise.all(updatePromises);

            // Replaced alert() with showToast()
            showToast("Weekly budget saved successfully!", "success");
            loadBudgetData();
        } catch (error) {
            console.error("Failed to save budgets:", error);
            showToast("Failed to save budgets. Please try again.", "error");
        } finally {
            setIsSaving(false);
        }
    };

    const handleDateSelect = (date) => {
        const sunday = startOfWeek(date, { weekStartsOn: 0 });
        setStartDate(sunday);
    };

    const chartData = daysOrder.map((day, index) => ({
        name: shortDays[index],
        Scheduled: scheduledCosts[day] || 0,
        Budget: budgets[day] || 0
    }));

    const CustomDateInput = forwardRef(({ value, onClick }, ref) => {
        const endDate = addDays(startDate, 6);
        return (
            <button onClick={onClick} ref={ref} className="flex items-center gap-2 bg-white border border-gray-200 text-gray-700 px-4 py-2 rounded-lg shadow-sm hover:bg-gray-50 focus:ring-2 focus:ring-blue-500 transition-colors">
                <Calendar size={18} className="text-gray-500" />
                <span className="font-medium text-sm">
                    {`${format(startDate, 'MMM dd, yyyy')} - ${format(endDate, 'MMM dd, yyyy')}`}
                </span>
            </button>
        )
    });

    return (
        <div className="space-y-6 relative">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">Schedule Budget</h1>
                    <p className="text-gray-500">Visualize and manage your scheduled labor costs for the selected week.</p>
                </div>

                <div className="relative z-40">
                    <DatePicker
                        selected={startDate}
                        onChange={handleDateSelect}
                        customInput={<CustomDateInput />}
                    />
                </div>
            </div>

            {isLoading ? (
                <div className="flex justify-center py-20">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-teal-600"></div>
                </div>
            ) : (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 relative z-10">
                    <div className="lg:col-span-2 bg-white rounded-xl shadow-sm border border-gray-200 p-6">
                        <div className="mb-6">
                            <h2 className="text-xl font-bold text-gray-800">Daily Labor Cost: Scheduled vs. Budget</h2>
                            <p className="text-sm text-gray-500 mt-1">
                                Total Scheduled: <span className="font-medium text-gray-700">${totals.scheduled.toFixed(2)}</span> |
                                Total Budgeted: <span className="font-medium text-gray-700">${totals.budgeted.toFixed(2)}</span>
                            </p>
                        </div>

                        <div className="h-[400px] w-full">
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart data={chartData} margin={{ top: 20, right: 0, left: -20, bottom: 0 }} barGap={2}>
                                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
                                    <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#6B7280', fontSize: 12 }} dy={10} />
                                    <YAxis axisLine={false} tickLine={false} tick={{ fill: '#6B7280', fontSize: 12 }} tickFormatter={(value) => `$${value}`} />
                                    <Tooltip cursor={{ fill: '#F3F4F6' }} formatter={(value) => [`$${value.toFixed(2)}`, undefined]} />
                                    <Legend iconType="square" wrapperStyle={{ paddingTop: '20px' }} />
                                    <Bar dataKey="Scheduled" name="Scheduled Cost" fill="#E57358" radius={[2, 2, 0, 0]} />
                                    <Bar dataKey="Budget" name="Budgeted Cost" fill="#2E9E8B" radius={[2, 2, 0, 0]} />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </div>

                    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 self-start">
                        <div className="mb-6">
                            <h2 className="text-xl font-bold text-gray-800">Edit Daily Budget</h2>
                            <p className="text-sm text-gray-500 mt-1">Set your target labor cost for each day.</p>
                        </div>

                        <div className="space-y-4">
                            {daysOrder.map((day) => (
                                <div key={day} className="flex items-center justify-between">
                                    <label className="text-sm font-medium text-gray-700 w-1/3">
                                        {day.charAt(0) + day.slice(1).toLowerCase()}
                                    </label>
                                    <div className="relative w-2/3">
                                        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                            <span className="text-gray-500 sm:text-sm">$</span>
                                        </div>
                                        <input
                                            type="number"
                                            min="0"
                                            step="0.01"
                                            value={budgets[day] === 0 ? '' : budgets[day]}
                                            onChange={(e) => handleBudgetChange(day, e.target.value)}
                                            className="w-full pl-7 pr-3 py-2 bg-gray-50 border border-gray-100 rounded-md text-gray-700 outline-none focus:border-teal-500 focus:ring-1 focus:ring-teal-500 transition-all"
                                        />
                                    </div>
                                </div>
                            ))}
                        </div>
                        <div className="mt-6 pt-6 border-t border-gray-100 flex justify-end">
                            <Button variant="primary" onClick={handleSave} disabled={isSaving || !startDate} className="w-full md:w-auto">
                                {isSaving ? 'Saving...' : 'Save Budget'}
                            </Button>
                        </div>
                    </div>
                </div>
            )}

            {/* --- CUSTOM TOAST NOTIFICATION --- */}
            {toast.show && (
                <div className="fixed bottom-5 right-5 z-[100] animate-in slide-in-from-right-10 duration-300">
                    <div className={`flex items-center gap-3 px-6 py-4 rounded-lg shadow-2xl border ${
                        toast.type === 'error'
                            ? 'bg-red-50 border-red-200 text-red-800'
                            : 'bg-green-50 border-green-200 text-green-800'
                    }`}>
                        {toast.type === 'error' ? <AlertCircle size={20} /> : <Send size={20} />}
                        <p className="font-semibold">{toast.message}</p>
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

export default Budget;