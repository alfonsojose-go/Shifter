import React, { useState } from 'react';
import { Info, X } from 'lucide-react';
import Button from './Button';

const InfoDialog = () => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      {/* Info Button */}
      <Button
        variant="outline"
        size="sm"
        onClick={() => setIsOpen(true)}
        className="flex items-center gap-2"
      >
        <Info size={16} /> Info
      </Button>

      {/* Dialog Overlay */}
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
          <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[80vh] overflow-y-auto">
            {/* Dialog Header */}
            <div className="flex items-center justify-between p-4 border-b">
              <h2 className="text-xl font-bold">How to Create a Rule-Based Schedule</h2>
              <button
                onClick={() => setIsOpen(false)}
                className="p-1 hover:bg-gray-100 rounded-full transition-colors"
              >
                <X size={20} />
              </button>
            </div>

            {/* Dialog Content */}
            <div className="p-6">
              <ol className="space-y-4 list-decimal pl-5">
                <li className="font-semibold">
                  Choose a Rule Type
                  <ul className="font-normal mt-2 space-y-1 list-disc pl-5">
                    <li><span className="font-medium">Time-based:</span> For time-specific scheduling (e.g., 8:00 AM - 9:00 AM)</li>
                    <li><span className="font-medium">Day-based:</span> For day-specific scheduling (e.g., Monday, Tuesday)</li>
                    <li><span className="font-medium">Person-based:</span> For employee-specific scheduling and relationships</li>
                  </ul>
                </li>

                <li className="font-semibold">
                  Choose a Rule Slot
                  <p className="font-normal mt-1">
                    Select the target of your rule: a time range, a specific day, an employee name, or a quantity of employees.
                  </p>
                </li>

                <li className="font-semibold">
                  Choose a Condition Type
                  <ul className="font-normal mt-2 space-y-1 list-disc pl-5">
                    <li><span className="font-medium">must have:</span> Exact number required</li>
                    <li><span className="font-medium">at least:</span> Minimum number required</li>
                    <li><span className="font-medium">at most:</span> Maximum number allowed</li>
                    <li><span className="font-medium">work with:</span> Employee works the exact same shift as another employee</li>
                    <li><span className="font-medium">start with:</span> Employee starts at the same time as another employee</li>
                    <li><span className="font-medium">end with:</span> Employee ends at the same time as another employee</li>
                  </ul>
                </li>

                <li className="font-semibold">
                  Choose a Condition Slot
                  <p className="font-normal mt-1">
                    Select the value that completes your condition: an employee name, a quantity, or a time range.
                  </p>
                </li>

                <li className="font-semibold">
                  Choose a Frequency
                  <p className="font-normal mt-1">
                    Set how often the rule applies: <span className="font-medium">every day</span> or a specific day of the week.
                  </p>
                </li>
              </ol>

              <div className="mt-6 p-4 bg-blue-50 rounded-md border border-blue-100">
                <p className="text-sm text-blue-800">
                  <span className="font-semibold">💡 Note:</span> Rules execute in priority order: Time-based → Day-based → Person-based.
                </p>
              </div>
            </div>

            {/* Dialog Footer */}
            <div className="flex justify-end p-4 border-t">
              <Button onClick={() => setIsOpen(false)}>
                Got it
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default InfoDialog;