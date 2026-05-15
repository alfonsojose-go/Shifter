import ScheduleGenerator from './services/ScheduleGenerator';
import fs from 'fs';
import path from 'path';

// Load data from JSON files
function loadTestData() {
  try {
    // Load availabilities
    const availabilitiesPath = path.join(process.cwd(), 'public', 'data', 'availabilities.json');
    const availabilitiesData = JSON.parse(fs.readFileSync(availabilitiesPath, 'utf8'));
    const availabilities = availabilitiesData.data;

    // Sample employees (you might want to load from another JSON file)
    const employees = [
      { id: 1, name: "Employee One" },
      { id: 2, name: "Employee Two" },
      { id: 3, name: "Employee Three" }
    ];

    // Define rules to test
    const rules = {
      ruleRows: [
            {
        "id": 1,
        "ruleType": "Person-based",
        "field1": "Laarni Cerna",
        "field2": "must have",
        "field3": "24",
        "field4": "every day"
        },
        {
        "id": 1770324309035,
        "ruleType": "Day-based",
        "field1": "Monday",
        "field2": "must have",
        "field3": "2",
        "field4": "every week"
        },
        {
        "id": 1770324322202,
        "ruleType": "Time-based",
        "field1": "9:00 AM - 10:00 AM",
        "field2": "must have",
        "field3": "2",
        "field4": "every day"
        }
      ]
    };

    return { availabilities, employees, rules };
  } catch (error) {
    console.error('Error loading test data:', error);
    return null;
  }
}

// Run test with JSON data
function runTestWithJSON() {
  console.log('=== Testing with JSON Data ===\n');
  
  const testData = loadTestData();
  if (!testData) {
    console.error('Failed to load test data');
    return;
  }

  const { availabilities, employees, rules } = testData;
  
  console.log('Loaded Data:');
  console.log(`- Availabilities: ${availabilities.length} records`);
  console.log(`- Employees: ${employees.length} records`);
  console.log(`- Rules: ${rules.ruleRows.length} rules\n`);

  // Create and run generator
  const generator = new ScheduleGenerator(availabilities, rules, employees);
  const result = generator.generate();

  // Display results
  console.log('\n=== Generated Schedules ===');
  result.employees.forEach(employee => {
    const employeeName = employees.find(e => e.id === employee.employeeId)?.name || `Employee ${employee.employeeId}`;
    console.log(`\n${employeeName}:`);
    if (employee.schedules.length === 0) {
      console.log('  No schedules generated');
    } else {
      employee.schedules.forEach(schedule => {
        console.log(`  • ${schedule.dayOfWeek}: ${schedule.startTime} - ${schedule.endTime}`);
      });
    }
  });

  // Summary
  const totalSlots = result.employees.reduce((sum, emp) => sum + emp.schedules.length, 0);
  console.log(`\n=== Summary ===`);
  console.log(`Total employees with schedules: ${result.employees.length}`);
  console.log(`Total schedule slots: ${totalSlots}`);
  
  // Check for conflicts
  const conflicts = checkForConflicts(result);
  if (conflicts.length > 0) {
    console.log(`\n⚠ Found ${conflicts.length} potential conflicts:`);
    conflicts.forEach(conflict => {
      console.log(`  - ${conflict}`);
    });
  }
}

// Helper to check for scheduling conflicts
function checkForConflicts(result) {
  const conflicts = [];
  
  result.employees.forEach(employee => {
    const schedules = employee.schedules;
    
    // Sort by day and start time
    schedules.sort((a, b) => {
      const dayOrder = { MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3, THURSDAY: 4, FRIDAY: 5, SATURDAY: 6, SUNDAY: 7 };
      if (a.dayOfWeek !== b.dayOfWeek) {
        return dayOrder[a.dayOfWeek] - dayOrder[b.dayOfWeek];
      }
      return a.startTime.localeCompare(b.startTime);
    });

    // Check for overlaps within same day
    for (let i = 0; i < schedules.length - 1; i++) {
      const current = schedules[i];
      const next = schedules[i + 1];
      
      if (current.dayOfWeek === next.dayOfWeek) {
        if (current.endTime > next.startTime) {
          conflicts.push(`Employee ${employee.employeeId}: Overlap on ${current.dayOfWeek} (${current.startTime}-${current.endTime} with ${next.startTime}-${next.endTime})`);
        }
      }
    }
  });
  
  return conflicts;
}

// Run the test
runTestWithJSON();