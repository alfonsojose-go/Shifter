Shifter
A full-stack employee scheduling system powered by a custom scheduling engine, REST APIs, and a modern React dashboard.
 React 

 Spring Boot 

 Node.js 

 License: MIT 
Problem Statement
Small-to-medium businesses in the food service and retail sectors lose thousands of dollars annually to:
Overstaffing during slow periods
Understaffing during rushes
Scheduling conflicts that lead to no-shows or overtime violations
Manual spreadsheet scheduling that consumes 4-6 hours per week of manager time
Existing solutions (When I Work, 7shifts) charge $2-4 per employee per month. For a 30-person team, that's $720-$1,440/year. Shifter provides a self-hosted, zero-subscription alternative with constraint-based intelligent scheduling.
Demo
Dashboard View
Manager dashboard showing weekly schedule, conflict alerts, and labor cost projections.
Schedule Generation
One-click generation of optimized schedules based on employee availability, seniority, and labor law constraints.
Tech Stack
Table
Layer	Technology	Purpose
Frontend	React 18 + Vite	Interactive scheduling dashboard
State Management	React Hooks + Context API	Lightweight state without Redux overhead
Styling	Tailwind CSS	Rapid, consistent UI development
Backend API	Node.js + Express	RESTful endpoints for schedule CRUD
Business Logic	Spring Boot	Constraint validation and schedule optimization
Scheduling Engine	Schedulum (custom npm package)	Core constraint-solving algorithm
Database	MongoDB	Flexible schema for varying constraint types
Build Tool	Vite	Fast development and optimized production builds
Key Features
One-click schedule generation — Automatically resolves shift conflicts, availability gaps, and labor law constraints
Priority-based shift assignment — Seniority, skill requirements, and employee preferences weighted algorithmically
Real-time conflict detection — Visual alerts when a generated schedule violates hard constraints (e.g., overtime limits)
Employee self-service portal — Staff can submit availability, request swaps, and view schedules
Labor cost analytics — Projected payroll costs based on scheduled hours and wage rates
Export to PDF/CSV — Shareable schedules for posting or payroll integration
Architecture Decisions
Why split the backend between Node.js and Spring Boot?
This was a deliberate architectural choice based on separation of concerns:
Table
Service	Responsibility	Rationale
Node.js/Express	API gateway, authentication, schedule CRUD	Rapid development, rich npm ecosystem, JSON-native
Spring Boot	Constraint validation, schedule optimization, business rules	Strong typing, complex domain logic benefits from Java's type safety, enterprise-grade transaction handling
The Node.js layer handles HTTP concerns and data persistence. When a schedule needs to be generated, it delegates to the Spring Boot service via internal REST calls. This microservice-lite approach kept each codebase focused and testable.
Why MongoDB over SQL for schedule data?
Schedule constraints vary wildly between businesses—some need union rules, others need skill matrices, others need labor law compliance. A rigid SQL schema would require constant migrations. MongoDB's document model lets each business define its own constraint schema without database changes.
Challenges & Solutions
Challenge: Race conditions in concurrent schedule edits
Multiple managers editing the same schedule simultaneously caused overwrites and data loss.
Solution: Implemented optimistic locking using version numbers on schedule documents. Each edit includes the expected version; if the version has changed, the server rejects the update and prompts the user to refresh. This eliminated overwrites without the complexity of real-time WebSockets.
Challenge: Slow schedule generation blocking the UI
The constraint solver could take 2-3 seconds for large teams, freezing the React dashboard.
Solution: Moved schedule generation to an asynchronous job queue. The frontend polls for completion status. This improved perceived performance from 2-3 seconds of frozen UI to < 200ms acknowledgment with a progress indicator.
Challenge: Complex constraint rules becoming unmaintainable
Early versions hardcoded constraint types (time, day, person). Adding new constraint types required backend changes.
Solution: Refactored to a plugin-based constraint architecture. Each constraint type implements a standard interface (validate(), score(), apply()). New constraints are registered at runtime. This reduced adding a new constraint type from ~50 lines of scattered changes to a single 15-line class.
How to Run
Prerequisites
Node.js 18+
Java 17+
MongoDB 6+
Backend (Spring Boot)
bash
cd shifter-backend
./mvnw spring-boot:run
API Layer (Node.js)
bash
cd shifter-api
npm install
npm run dev
Frontend (React)
bash
cd shifter-frontend
npm install
npm run dev
The application will be available at http://localhost:5173.
What I Learned
Distributed system complexity: Splitting a single app into two backend services introduced network failure modes I hadn't considered. I learned to design for partial failures—graceful degradation when the Spring Boot service is unavailable.
Frontend performance: React's default rendering is fast until it isn't. I profiled with React DevTools and learned to memoize expensive components, cutting re-render time by 60%.
Real-world constraint modeling: Academic scheduling problems assume perfect information. In practice, managers submit conflicting constraints constantly. Building a system that handles ambiguity gracefully is harder than building one that assumes consistency.
Links
GitHub Repository
Schedulum npm Package
License: MIT
