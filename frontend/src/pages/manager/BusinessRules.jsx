import React, { useState, useEffect } from 'react';
import { Trash2, Clock, Calendar, User, CheckCircle, X, Download, Upload, Save, Plus } from 'lucide-react';
import Button from '../../components/common/Button';
import InfoDialog from '../../components/common/Info';
import api from '../../api/axiosConfig';
import Schedulum from 'schedulum';


const BusinessRules = () => {
    const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

    const generateTimeIntervals = () => {
        const intervals = [];
        for (let hour = 0; hour < 24; hour++) {
            const displayHour = hour % 12 || 12;
            const period = hour < 12 ? 'AM' : 'PM';
            intervals.push({
                label: `${displayHour}:00 ${period}`,
                startTime: `${String(hour).padStart(2, '0')}:00`,
            });
        }
        return intervals;
    };

    const timeIntervals = generateTimeIntervals();

    const conditions = ['must have', 'at least', 'at most', 'work with', 'start with', 'end with'];
    const frequencies = ['every day', 'every week', 'every month'];
    const ruleTypes = ['Time-based', 'Day-based', 'Person-based'];

    const emptyRow = (id) => ({ id, ruleType: '', field1: '', field2: '', field3: '', field4: '', field5: '' });

    // --- STATE ---
    const [employees, setEmployees] = useState([]);
    const [isLoadingEmployees, setIsLoadingEmployees] = useState(true);
    const [ruleRows, setRuleRows] = useState([emptyRow(1)]);
    const [savedRules, setSavedRules] = useState({ 'Time-based': [], 'Day-based': [], 'Person-based': [] });
    const [notification, setNotification] = useState({ show: false, message: '', type: 'success' });
    const [isGenerating, setIsGenerating] = useState(false);

    useEffect(() => {
        const loadInitialData = async () => {
            setIsLoadingEmployees(true);
            try {
                // Fetch employees/availabilities
                await fetchEmployeesFromAPI();

                // Fetch approved leaves and convert to rules
                await fetchAndAddLeaveRules();

            } catch (error) {
                console.error('Error loading initial data:', error);
            } finally {
                setIsLoadingEmployees(false);
            }
        };

        loadInitialData();
        loadSavedRules();
    }, []);

    
    // --- EMPLOYEES ---
    const fetchEmployeesFromAPI = async () => {
        setIsLoadingEmployees(true);
        try {
            const response = await api.get('/employee/availabilities');
            const data = response.data?.data || [];

            if (data.length > 0) {
                const employeeMap = new Map();
                data.forEach(avail => {
                    if (avail.employeeId) {
                        const employeeName = avail.employeeName || `Employee ${avail.employeeId}`;
                        if (!employeeMap.has(avail.employeeId))
                            employeeMap.set(avail.employeeId, { id: avail.employeeId, name: employeeName });
                    }
                });
                const employeesArray = Array.from(employeeMap.values());
                setEmployees(employeesArray);
                showNotification(`Loaded ${employeesArray.length} employees`, 'success');
            } else {
                showNotification('No availability data returned from server', 'warning');
            }
        } catch (error) {
            console.error('Error fetching availabilities:', error);
            showNotification(`Failed to load employees: ${error.message}`, 'warning');
        } finally {
            setIsLoadingEmployees(false);
        }
    };

    // --- APPROVED LEAVES ---
    // Update the fetchAndAddLeaveRules function to only include leaves in current week
    const fetchAndAddLeaveRules = async () => {
        try {
            // Fetch approved leaves from your API
            const response = await api.get('/api/requests?status=APPROVED');
            const approvedRequests = response.data || [];

            // Filter for leave/time-off types
            const leaveRequests = approvedRequests.filter(request =>
                ['TIME_OFF', 'LEAVE', 'SICK_DAY', 'UNAVAILABLE'].includes(request.type)
            );

            if (leaveRequests.length === 0) {
                console.log('No approved leaves found');
                // Clear any existing leave rules
                setRuleRows(prev => prev.filter(row => row.source !== 'leave-approval'));
                return;
            }

            console.log(`Fetched ${leaveRequests.length} approved leaves`);

            // Get current week date range
            const currentWeekRange = getCurrentWeekRange();
            console.log(`Current week: ${currentWeekRange.start} to ${currentWeekRange.end}`);

            // Convert to rule rows - only for leaves in current week
            const leaveRuleRows = [];

            leaveRequests.forEach(leave => {
                // Single-day leave
                if (leave.date) {
                    // Check if the leave date is within current week
                    if (isDateInCurrentWeek(leave.date)) {
                        const dayOfWeek = getDayOfWeekFromDate(leave.date);
                        const employee = employees.find(e => e.id === leave.userId);
                        const employeeName = employee ? employee.name : `Employee ${leave.userId}`;

                        leaveRuleRows.push({
                            id: `leave-${leave.id}`,
                            ruleType: 'Day-based',
                            field1: dayOfWeek,
                            field2: 'must not have',
                            field3: employeeName,
                            field4: 'every week',
                            source: 'leave-approval',
                            leaveId: leave.id,
                            leaveDate: leave.date,
                            isActive: true,
                            weekOf: currentWeekRange.start // Track which week this rule belongs to
                        });
                    } else {
                        console.log(`Skipping leave ${leave.id} - date ${leave.date} not in current week`);
                    }
                }

                // Multi-day leave
                if (leave.startDate && leave.endDate) {
                    const dates = getDatesInRange(leave.startDate, leave.endDate);

                    // Filter dates to only those in current week
                    const datesInCurrentWeek = dates.filter(date => isDateInCurrentWeek(date));

                    if (datesInCurrentWeek.length > 0) {
                        const employee = employees.find(e => e.id === leave.userId);
                        const employeeName = employee ? employee.name : `Employee ${leave.userId}`;

                        datesInCurrentWeek.forEach((dateStr, idx) => {
                            const dayOfWeek = getDayOfWeekFromDate(dateStr);

                            leaveRuleRows.push({
                                id: `leave-${leave.id}-${dateStr}`, // Use date in ID for uniqueness
                                ruleType: 'Day-based',
                                field1: dayOfWeek,
                                field2: 'must not have',
                                field3: employeeName,
                                field4: 'every week',
                                source: 'leave-approval',
                                leaveId: leave.id,
                                leaveDate: dateStr,
                                isActive: true,
                                weekOf: currentWeekRange.start
                            });
                        });

                        console.log(`Added ${datesInCurrentWeek.length} days from multi-day leave ${leave.id} that fall in current week`);
                    } else {
                        console.log(`Skipping multi-day leave ${leave.id} - no dates in current week`);
                    }
                }
            });

            // Update rule rows: keep manual rules, replace all leave rules with fresh ones
            setRuleRows(prev => {
                const manualRules = prev.filter(row => row.source !== 'leave-approval');

                // Optional: Log how many leave rules were removed/added
                const oldLeaveCount = prev.filter(row => row.source === 'leave-approval').length;
                console.log(`Replacing ${oldLeaveCount} old leave rules with ${leaveRuleRows.length} new ones`);

                return [...manualRules, ...leaveRuleRows];
            });

            if (leaveRuleRows.length > 0) {
                showNotification(`Loaded ${leaveRuleRows.length} leave rules for current week`, 'success');
            } else {
                showNotification('No approved leaves in current week', 'info');
            }

        } catch (error) {
            console.error('Error fetching approved leaves:', error);
            // On error, clear leave rules to avoid stale data
            setRuleRows(prev => prev.filter(row => row.source !== 'leave-approval'));
        }
    };

    // Helper function to check if a date is in the current week
    const isDateInCurrentWeek = (dateString) => {
        const date = new Date(dateString + 'T12:00:00');
        const today = new Date();

        // Get current week's start (Sunday) and end (Saturday)
        const startOfWeek = new Date(today);
        startOfWeek.setDate(today.getDate() - today.getDay()); // Go back to Sunday

        const endOfWeek = new Date(startOfWeek);
        endOfWeek.setDate(startOfWeek.getDate() + 6); // Add 6 days to get to Saturday

        // Set to midnight for proper comparison
        startOfWeek.setHours(0, 0, 0, 0);
        endOfWeek.setHours(23, 59, 59, 999);

        return date >= startOfWeek && date <= endOfWeek;
    };

    // Helper function to get current week range (for display/logging)
    const getCurrentWeekRange = () => {
        const today = new Date();
        const startOfWeek = new Date(today);
        startOfWeek.setDate(today.getDate() - today.getDay()); // Sunday
        startOfWeek.setHours(0, 0, 0, 0);

        const endOfWeek = new Date(startOfWeek);
        endOfWeek.setDate(startOfWeek.getDate() + 6); // Saturday
        endOfWeek.setHours(23, 59, 59, 999);

        return {
            start: startOfWeek.toISOString().split('T')[0],
            end: endOfWeek.toISOString().split('T')[0]
        };
    };

    // Keep your existing helper functions
    const getDayOfWeekFromDate = (dateString) => {
        const date = new Date(dateString + 'T12:00:00');
        const days = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
        return days[date.getDay()].charAt(0) + days[date.getDay()].slice(1).toLowerCase();
    };

    const getDatesInRange = (startDate, endDate) => {
        const dates = [];
        let currentDate = new Date(startDate + 'T12:00:00');
        const end = new Date(endDate + 'T12:00:00');

        while (currentDate <= end) {
            dates.push(currentDate.toISOString().split('T')[0]);
            currentDate.setDate(currentDate.getDate() + 1);
        }

        return dates;
    };

    // --- NOTIFICATIONS ---
    const showNotification = (message, type = 'success') => {
        setNotification({ show: true, message, type });
        setTimeout(() => setNotification({ show: false, message: '', type: 'success' }), 3000);
    };

    // --- RULE ROW MANAGEMENT ---
    const handleRowChange = (rowId, field, value) => {
        setRuleRows(prev => prev.map(row => {
            if (row.id !== rowId) return row;
            if (field === 'ruleType') return { ...emptyRow(rowId), ruleType: value };
            return { ...row, [field]: value };
        }));
    };

    const addRuleRow = () => setRuleRows(prev => [...prev, emptyRow(Date.now())]);

    const removeRuleRow = (rowId) => {
        if (ruleRows.length > 1) setRuleRows(prev => prev.filter(row => row.id !== rowId));
    };

    const getFieldPlaceholders = (ruleType) => {
        switch (ruleType) {
            case 'Time-based':   return ['Start Time', 'End Time', 'Condition', 'Person / Quantity', 'Frequency'];
            case 'Day-based':    return ['Day', 'Condition', 'Person / Quantity', 'Frequency'];
            case 'Person-based': return ['Employee Name', 'Condition', 'Hours / Employee', 'Frequency'];
            default:             return ['Field 1', 'Condition', 'Field 3', 'Frequency'];
        }
    };

    const getFieldOptions = (ruleType, fieldIndex, row=null) => {
        switch (ruleType) {
            case 'Time-based':
                if (fieldIndex === 1) return timeIntervals.map(t => t.label);
                if (fieldIndex === 2) return timeIntervals.map(t => t.label);
                if (fieldIndex === 3) return conditions;
                if (fieldIndex === 4) {
                    const nums = Array.from({ length: employees.length }, (_, i) => String(i + 1));
                    return [...employees.map(e => e.name), ...nums];
                }
                if (fieldIndex === 5) return frequencies;
                return [];
            case 'Day-based':
                if (fieldIndex === 1) return days;
                if (fieldIndex === 2) return conditions;
                if (fieldIndex === 3) {
                    const nums = Array.from({ length: employees.length }, (_, i) => String(i + 1));
                    return [...employees.map(e => e.name), ...nums];
                }
                if (fieldIndex === 4) return frequencies.filter(f => f !== 'every day');
                return [];
            case 'Person-based':
                        if (fieldIndex === 1) return employees.map(e => e.name);
                        if (fieldIndex === 2) return conditions;
                        if (fieldIndex === 3) {
                            // ✅ Check the condition to determine what options to show
                            if (row) {
                                const relationalConditions = ['work with', 'start with', 'end with'];
                                if (relationalConditions.includes(row.field2)) {
                                    // Show only employee names for relational conditions
                                    return employees.map(e => e.name);
                                } else if (row.field2) {
                                    // Show only hours for non-relational conditions
                                    return Array.from({ length: 24 }, (_, i) => String(i + 1));
                                }
                            }
                            // Default: show both (when no condition selected yet)
                            const hours = Array.from({ length: 24 }, (_, i) => String(i + 1));
                            return [...employees.map(e => e.name), ...hours];
                        }
                        if (fieldIndex === 4) return frequencies;
                        return [];
                    default:
                        return [];
                }
    };

    // --- ROW → STRUCTURED JSON ---
    const rowToStructuredRule = (row) => {
        if (!row.ruleType) return null;

        switch (row.ruleType) {
            case 'Time-based': {
                if (!row.field1 || !row.field2 || !row.field3 || !row.field5) return null;
                const startInterval = timeIntervals.find(t => t.label === row.field1);
                const endInterval   = timeIntervals.find(t => t.label === row.field2);
                if (!startInterval || !endInterval) return null;
                const matchedEmployee = employees.find(e => e.name === row.field4);
                const conditionSlot = matchedEmployee
                    ? { type: 'employeeId', value: matchedEmployee.id }
                    : { type: 'quantity',   value: Number(row.field4) || 1 };
                return {
                    rulesType: 'Time-based',
                    condition: row.field3,
                    ruleSlot: { startTime: startInterval.startTime, endTime: endInterval.startTime },
                    conditionSlot,
                    frequency: row.field5
                };
            }
            case 'Day-based': {
                if (!row.field1 || !row.field2 || !row.field4) return null;
                const matchedEmployee = employees.find(e => e.name === row.field3);
                const conditionSlot = matchedEmployee
                    ? { type: 'employeeId', value: matchedEmployee.id }
                    : { type: 'quantity',   value: Number(row.field3) || 1 };
                return {
                    rulesType: 'Day-based',
                    condition: row.field2,
                    ruleSlot: row.field1.toUpperCase(),
                    conditionSlot,
                    frequency: row.field4
                };
            }
            case 'Person-based': {
                if (!row.field1 || !row.field2 || !row.field4) return null;

                // Find the primary employee (ruleSlot)
                const primaryEmployee = employees.find(e => e.name === row.field1);
                if (!primaryEmployee) return null;

                // Determine condition slot type based on the condition
                const relationalConditions = ['work with', 'start with', 'end with'];
                let conditionSlot;

                if (relationalConditions.includes(row.field2)) {
                    // For relational conditions, field3 should be an employee name
                    const secondaryEmployee = employees.find(e => e.name === row.field3);
                    if (!secondaryEmployee) return null;
                    conditionSlot = { type: 'employeeId', value: secondaryEmployee.id };
                } else {
                    // For other conditions (must have, at least, at most), field3 is hours
                    const hours = Number(row.field3);
                    if (!hours) return null;
                    conditionSlot = { type: 'hours', value: hours };
                }

                return {
                    rulesType: 'Person-based',
                    condition: row.field2,
                    ruleSlot: { type: 'employeeId', value: primaryEmployee.id },
                    conditionSlot,
                    frequency: row.field4
                };
            }
            default:
                return null;
        }
    };

    const generateRuleText = (row) => {
        switch (row.ruleType) {
            case 'Time-based':
                return `${row.field1 || 'start'} - ${row.field2 || 'end'} ${row.field3 || 'condition'} ${row.field4 || 'person/quantity'} ${row.field5 || 'frequency'}`;
            case 'Day-based':
                return `${row.field1 || 'day'} ${row.field2 || 'condition'} ${row.field3 || 'person/quantity'} ${row.field4 || 'frequency'}`;
            case 'Person-based':
                return `${row.field1 || 'Employee'} ${row.field2 || 'condition'} ${row.field3 || 'hours'} hrs ${row.field4 || 'frequency'}`;
            default:
                return `Rule ${row.id}`;
        }
    };

    // --- STORAGE ---
    const saveRules = () => {
        // Filter out leave-approval rules - these are ephemeral and should not be saved
        const manualRuleRows = ruleRows.filter(row => row.source !== 'leave-approval');
        const timeBasedRules = [], dayBasedRules = [], personBasedRules = [];
        const structuredRules = [];

        // Only process manual rules (leave rules are excluded)
        manualRuleRows.forEach((row, index) => {
            if (!row.ruleType) return;

            const ruleEntry = {
                id: row.id,
                text: generateRuleText(row),
                source: 'manual' // Always 'manual' since we filtered out leave rules
            };

            switch (row.ruleType) {
                case 'Time-based': timeBasedRules.push(ruleEntry); break;
                case 'Day-based': dayBasedRules.push(ruleEntry); break;
                case 'Person-based': personBasedRules.push(ruleEntry); break;
            }

            const structured = rowToStructuredRule(row);
            if (structured) {
                structuredRules.push({
                    id: index + 1,
                    ...structured,
                    source: 'manual'
                    // Note: leaveId is NOT included since these are manual rules
                });
            }
        });

        const rules = {
            'Time-based': timeBasedRules,
            'Day-based': dayBasedRules,
            'Person-based': personBasedRules
        };

        const rulesData = {
            rules,                    // Display rules by category (manual only)
            structuredRules,          // Schedulum-ready rules (manual only)
            ruleRows: manualRuleRows, // Only save manual rule rows
            employees,                // Employee data (for reference)
            metadata: {
                lastUpdated: new Date().toISOString(),
                version: '1.0',
                totalManualRules: Object.values(rules).flat().length,
                totalEmployees: employees.length,
                // Note: leaveRulesCount is no longer tracked in saved data
                message: 'Leave rules are fetched live from API and not saved'
            }
        };

        try {
            localStorage.setItem('business_rules', JSON.stringify(rulesData, null, 2));

            // Update savedRules state with only manual rules
            setSavedRules(rules);

            // Show appropriate notification
            const leaveRuleCount = ruleRows.filter(r => r.source === 'leave-approval').length;
            if (leaveRuleCount > 0) {
                showNotification(
                    `Saved ${Object.values(rules).flat().length} manual rules. ` +
                    `${leaveRuleCount} leave rules are loaded from API.`,
                    'success'
                );
            } else {
                showNotification(`${Object.values(rules).flat().length} rules saved successfully!`, 'success');
            }

            return rulesData;
        } catch (error) {
            showNotification('Failed to save rules: ' + error.message, 'warning');
            return null;
        }
    };

    // Also update the loadSavedRules function to handle the new format
    const loadSavedRules = () => {
        try {
            const saved = localStorage.getItem('business_rules');
            if (saved) {
                const parsedData = JSON.parse(saved);

                // Only load manual rules (leave rules will be fetched separately)
                setRuleRows(parsedData.ruleRows || [emptyRow(1)]);

                if (parsedData.employees) setEmployees(parsedData.employees);

                // Update savedRules state for display
                setSavedRules(parsedData.rules || {
                    'Time-based': [],
                    'Day-based': [],
                    'Person-based': []
                });

                showNotification('Manual rules loaded from storage', 'success');
            }
        } catch (error) {
            console.error('Error loading saved rules:', error);
            showNotification('Error loading saved rules', 'warning');
        }
    };

    

    const exportRulesToFile = () => {
        const rulesData = saveRules();
        if (!rulesData) return;
        const blob = new Blob([JSON.stringify(rulesData, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `business-rules-${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
        showNotification('Rules exported to JSON file', 'success');
    };

    const importRulesFromFile = (event) => {
        const file = event.target.files?.[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                const imported = JSON.parse(e.target?.result);
                if (!imported.rules) throw new Error('Invalid rules format');
                setSavedRules(imported.rules);
                setRuleRows(imported.ruleRows || [emptyRow(1)]);
                if (imported.employees) setEmployees(imported.employees);
                localStorage.setItem('business_rules', JSON.stringify(imported));
                showNotification('Rules imported successfully!', 'success');
            } catch {
                showNotification('Error importing file: Invalid format', 'warning');
            }
        };
        reader.readAsText(file);
        event.target.value = '';
    };

    const clearAllRules = () => {
        if (window.confirm('Are you sure you want to clear all rules?')) {
            setSavedRules({ 'Time-based': [], 'Day-based': [], 'Person-based': [] });
            setRuleRows([emptyRow(1)]);
            localStorage.removeItem('business_rules');
            showNotification('All rules cleared', 'delete');
        }
    };

   
    const handleGenerateSchedule = async () => {
        setIsGenerating(true);
        try {
            if (employees.length === 0)
                throw new Error('No employees found. Please check employee data.');

            const rulesData = saveRules();
            if (!rulesData) throw new Error('Failed to save rules');
            if (!rulesData.structuredRules?.length)
                throw new Error('No complete rules found. Please fill in all fields for each rule.');

            // 1. Fetch availabilities
            showNotification('Fetching employee availabilities...', 'success');
            const availResponse = await api.get('/employee/availabilities');
            console.log('Raw availResponse:', availResponse);

            // Extract the availabilities array
            const availabilitiesArray = availResponse.data?.data || [];

            console.log('Processed availabilities array:', availabilitiesArray);
            console.log('Number of availabilities:', availabilitiesArray.length);

            if (availabilitiesArray.length === 0) {
                throw new Error('No availabilities found. Please add employee availabilities first.');
            }

            // Create a clean copy of the array
            const cleanAvailabilitiesArray = JSON.parse(JSON.stringify(availabilitiesArray));
            console.log('Clean availabilities array:', cleanAvailabilitiesArray);

            // 2. Generate using Schedulum
            showNotification('Generating schedules based on rules...', 'success');

            const scheduler = new Schedulum();
            scheduler.setAvailabilities(cleanAvailabilitiesArray);
            scheduler.setRules(rulesData.structuredRules);

            const batchScheduleData = scheduler.generateSchedule();

            console.log('Generated schedule data:', batchScheduleData);

            if (!batchScheduleData || !batchScheduleData.employees?.length) {
                throw new Error('No schedules could be generated with the current rules and availabilities.');
            }

            // 3. Clear existing schedules for all employees before posting
            showNotification('Clearing existing schedules...', 'success');

            const employeeIds = batchScheduleData.employees.map(emp => emp.employeeId);
            await Promise.all(
                employeeIds.map(id => api.delete(`/scheduling/employee/${id}`).catch(err =>
                    console.warn(`Could not clear schedules for employee ${id}:`, err.message)
                ))
            );

            // 4. POST to DB
            showNotification('Submitting schedules to server...', 'success');

            const payload = {
                employees: batchScheduleData.employees.map(emp => ({
                    employeeId: emp.employeeId,
                    schedules: emp.schedules || []
                }))
            };

            console.log('Batch payload being sent:', JSON.stringify(payload, null, 2));

            const apiResponse = await api.post('/scheduling/batch', payload);

            if (apiResponse.data?.success) {
                showNotification(`✓ ${apiResponse.data.message}`, 'success');
            } else {
                throw new Error(apiResponse.data?.message || 'Failed to create schedules');
            }

        } catch (error) {
            console.error('Error generating schedule:', error);
            console.error('Server error response:', error.response?.data);
            console.error('Server error status:', error.response?.status);
            showNotification(`Error: ${error.message}`, 'warning');
        } finally {
            setIsGenerating(false);
        }
    };

    // --- RENDER ---
    return (
        <div className="space-y-6 relative">

            {/* NOTIFICATION TOAST */}
            {notification.show && (
                <div className="fixed bottom-6 right-6 z-50 bg-gray-900 text-white px-5 py-4 rounded-xl shadow-2xl flex items-start gap-4 animate-in slide-in-from-bottom-5 fade-in duration-300 max-w-sm border border-gray-700">
                    <div className={`p-2 rounded-full shrink-0 ${
                        notification.type === 'delete' ? 'bg-red-500/20 text-red-400' :
                        notification.type === 'warning' ? 'bg-yellow-500/20 text-yellow-400' :
                        'bg-green-500/20 text-green-400'
                    }`}>
                        {notification.type === 'delete' ? <Trash2 size={20} /> :
                         notification.type === 'warning' ? <X size={20} /> :
                         <CheckCircle size={20} />}
                    </div>
                    <div className="flex-1">
                        <h4 className="font-bold text-sm mb-0.5">
                            {notification.type === 'delete' ? 'Deleted' :
                             notification.type === 'warning' ? 'Warning' : 'Success'}
                        </h4>
                        <p className="text-xs text-gray-400 leading-relaxed">{notification.message}</p>
                    </div>
                    <button onClick={() => setNotification({ ...notification, show: false })} className="text-gray-500 hover:text-white transition-colors">
                        <X size={16} />
                    </button>
                </div>
            )}

            {/* HEADER */}
            <div>
                <h1 className="text-2xl font-bold text-gray-800">Set Rules</h1>
                <p className="text-gray-500">Define constraints and rules for automatic scheduling.</p>
                {isLoadingEmployees ? (
                    <div className="mt-2 text-sm text-blue-600 flex items-center gap-2">
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600" />
                        Loading employee data...
                    </div>
                ) : employees.length > 0 ? (
                    <div className="mt-2 text-sm text-gray-600">
                        Found {employees.length} employee{employees.length !== 1 ? 's' : ''}:
                        <span className="ml-2 text-gray-500">
                            {employees.slice(0, 3).map(e => e.name).join(', ')}
                            {employees.length > 3 ? `, +${employees.length - 3} more` : ''}
                        </span>
                    </div>
                ) : (
                    <div className="mt-2 text-sm text-yellow-600">No employees found. Please add employee data first.</div>
                )}
            </div>

            {/* CONTROL BAR */}
            <div className="flex flex-wrap gap-3 items-center justify-between p-4 bg-gray-50 rounded-lg border border-gray-100">
                <div className="flex items-center gap-2 text-sm text-gray-600">
                    <span className="font-medium">Employees: {employees.length}</span>
                    <span className="text-gray-400">•</span>
                    <span className="font-medium">Total Rules: {Object.values(savedRules).flat().length}</span>
                    <span className="text-gray-400">•</span>
                    <span className="font-medium">Active Forms: {ruleRows.length}</span>
                </div>
                <div className="flex gap-2">
                    <InfoDialog/>
                    <Button variant="outline" size="sm" onClick={clearAllRules}>Clear All</Button>
                    <label>
                        <input type="file" accept=".json" onChange={importRulesFromFile} className="hidden" />
                        <Button variant="outline" size="sm" as="span" className="cursor-pointer flex items-center gap-2">
                            <Upload size={16} /> Import
                        </Button>
                    </label>
                    <Button variant="outline" size="sm" onClick={exportRulesToFile} className="flex items-center gap-2">
                        <Download size={16} /> Export
                    </Button>
                    <Button onClick={saveRules} size="sm" className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700">
                        <Save size={16} /> Save Rules
                    </Button>
                </div>
            </div>

            {/* RULE FORMS */}
            {isLoadingEmployees ? (
                <div className="bg-white p-8 rounded-xl shadow-sm border border-gray-100 text-center">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4" />
                    <p className="text-gray-600">Loading employee data...</p>
                </div>
            ) : employees.length === 0 ? (
                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 text-center py-8">
                    <User className="h-12 w-12 text-gray-300 mx-auto mb-4" />
                    <h3 className="text-lg font-semibold text-gray-800 mb-2">No Employees Found</h3>
                    <p className="text-gray-600 mb-4">Employee data is required to create rules.</p>
                    <Button variant="outline" onClick={fetchEmployeesFromAPI}>Retry Loading Employees</Button>
                </div>
            ) : (
                <div className="space-y-4">
                    {ruleRows.map((row, index) => {
                        const placeholders = getFieldPlaceholders(row.ruleType);
                        const isTimeBased  = row.ruleType === 'Time-based';
                        const fieldNums    = isTimeBased ? [1, 2, 3, 4, 5] : [1, 2, 3, 4];
                        const structured   = rowToStructuredRule(row);
                        const isComplete   = structured !== null;

                        return (
                            <div key={row.id} className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                                <div className="flex items-center justify-between mb-6">
                                    <div className="flex items-center gap-3">
                                        <h3 className="font-bold text-gray-800">Rule #{index + 1}</h3>
                                        {row.ruleType && (
                                            <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                                                isComplete ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                                            }`}>
                                                {isComplete ? '✓ Complete' : 'Incomplete'}
                                            </span>
                                        )}
                                    </div>
                                    {ruleRows.length > 1 && (
                                        <button onClick={() => removeRuleRow(row.id)} className="text-gray-400 hover:text-red-500 transition-colors">
                                            <Trash2 size={18} />
                                        </button>
                                    )}
                                </div>

                                <div className={`grid grid-cols-1 gap-4 ${isTimeBased ? 'md:grid-cols-6' : 'md:grid-cols-5'}`}>
                                    <div>
                                        <label className="text-xs font-semibold text-gray-600 mb-1 block">Rule Type</label>
                                        <select
                                            value={row.ruleType}
                                            onChange={(e) => handleRowChange(row.id, 'ruleType', e.target.value)}
                                            className="w-full px-3 py-2 border rounded-lg text-sm bg-white outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition-all"
                                        >
                                            <option value="">Select Type</option>
                                            {ruleTypes.map(type => <option key={type} value={type}>{type}</option>)}
                                        </select>
                                    </div>

                                    {fieldNums.map(fieldNum => {
                                        const fieldKey = `field${fieldNum}`;
                                        const options  = getFieldOptions(row.ruleType, fieldNum, row);

                                        // ✅ Determine if this should be a number input based on condition
                                        const relationalConditions = ['work with', 'start with', 'end with'];
                                        const isHoursInput = row.ruleType === 'Person-based' &&
                                                             fieldNum === 3 &&
                                                             !relationalConditions.includes(row.field2);

                                        // ✅ Get dynamic placeholder
                                        const getPlaceholder = () => {
                                            if (row.ruleType === 'Person-based' && fieldNum === 3) {
                                                return relationalConditions.includes(row.field2) ? 'Select Employee' : 'e.g. 8';
                                            }
                                            return placeholders[fieldNum - 1];
                                        };

                                        return (
                                            <div key={fieldNum}>
                                                <label className="text-xs font-semibold text-gray-600 mb-1 block">
                                                    {placeholders[fieldNum - 1]}
                                                </label>
                                                {isHoursInput ? (
                                                    <input
                                                        type="number"
                                                        min={1}
                                                        value={row[fieldKey]}
                                                        onChange={(e) => handleRowChange(row.id, fieldKey, e.target.value)}
                                                        placeholder="e.g. 8"
                                                        className="w-full px-3 py-2 border rounded-lg text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition-all"
                                                    />
                                                ) : options.length > 0 ? (
                                                    <select
                                                        value={row[fieldKey]}
                                                        onChange={(e) => handleRowChange(row.id, fieldKey, e.target.value)}
                                                        disabled={!row.ruleType}
                                                        className="w-full px-3 py-2 border rounded-lg text-sm bg-white outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition-all disabled:opacity-40 disabled:cursor-not-allowed"
                                                    >
                                                        <option value="">{getPlaceholder()}</option>
                                                        {options.map(opt => <option key={opt} value={opt}>{opt}</option>)}
                                                    </select>
                                                ) : (
                                                    <input
                                                        type="text"
                                                        value={row[fieldKey]}
                                                        onChange={(e) => handleRowChange(row.id, fieldKey, e.target.value)}
                                                        placeholder={placeholders[fieldNum - 1]}
                                                        disabled={!row.ruleType}
                                                        className="w-full px-3 py-2 border rounded-lg text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition-all disabled:opacity-40 disabled:cursor-not-allowed"
                                                    />
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>

                                {isComplete && (
                                    <details className="mt-4">
                                        <summary className="text-xs text-gray-400 cursor-pointer hover:text-blue-500 transition-colors select-none">
                                            Preview JSON output
                                        </summary>
                                        <pre className="mt-2 text-xs bg-gray-50 border border-gray-100 rounded-lg p-3 overflow-x-auto text-gray-600">
                                            {JSON.stringify(structured, null, 2)}
                                        </pre>
                                    </details>
                                )}
                            </div>
                        );
                    })}

                    <Button variant="outline" onClick={addRuleRow} className="w-full flex items-center justify-center gap-2 py-3">
                        <Plus size={18} /> Add Rule Set
                    </Button>
                </div>
            )}

            {/* ACTION BUTTONS */}
            <div className="flex gap-3 pt-6 border-t border-gray-100">
                <Button
                    className="px-6"
                    onClick={handleGenerateSchedule}
                    disabled={isGenerating || ruleRows.every(r => !r.ruleType) || employees.length === 0}
                >
                    {isGenerating ? 'Generating...' : 'Generate Schedule'}
                </Button>
                {employees.length === 0 && (
                    <Button variant="outline" onClick={fetchEmployeesFromAPI}>Reload Employees</Button>
                )}
            </div>
        </div>
    );
};

export default BusinessRules;