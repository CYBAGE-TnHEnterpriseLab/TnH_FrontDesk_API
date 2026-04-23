# PMS Tool - Property Wizard

This is a Property Management System (PMS) tool with a property wizard feature.

## Tech Stack
- Backend: Java Spring Boot
- Frontend: React (TypeScript)
- Database: PostgreSQL

## Prerequisites
- Java 17 or higher
- Maven 3.6+
- Node.js 16+
- npm or yarn
- PostgreSQL

## Setup

### Backend Setup
1. Navigate to `backend/` directory.
2. Update `src/main/resources/application.properties` with your PostgreSQL credentials.
3. Run `mvn spring-boot:run` to start the backend server on port 8080.

### Frontend Setup
1. Navigate to `frontend/` directory.
2. Run `npm install` to install dependencies.
3. Run `npm start` to start the development server on port 3000.

### Database
- Install PostgreSQL and create a database named `pms_db`.
- Create a user `pms_user` with password `pms123` and grant privileges on `pms_db`.
- Alternatively, update `backend/src/main/resources/application.properties` with your PostgreSQL username and password.
- The application uses JPA with `ddl-auto=update`, so tables will be created automatically.
- If needed, run the SQL script in `backend/src/main/resources/schema.sql` manually in PostgreSQL.

## API Endpoints
- GET /api/properties: Get all properties
- GET /api/properties/{id}: Get property by ID
- POST /api/properties: Create a new property (JSON body with property details)
- PUT /api/properties/{id}: Update property by ID
- DELETE /api/properties/{id}: Delete property by ID

### Property JSON Structure
```json
{
  "propertyName": "Property Name",
  "email": "property@example.com",
  "address": "Property Address",
  "contact": "+1234567890",
  "timezone": "America/New_York",
  "checkinTime": "14:00",
  "checkoutTime": "11:00",
  "propertyType": "Hotel",
  "price": 150.0,
  "bedrooms": 2,
  "bathrooms": 1,
  "area": 800.0,
  "description": "Description",
  "status": "Available"
}
```

## Development
- Backend runs on port 8080
- Frontend runs on port 3000

Note: Ensure all prerequisites are installed before running the project.