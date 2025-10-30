# Cinema Ticket Booking — Backend (Spring Boot + MySQL)

Production-ready REST API for managing cinemas and showtimes.

## Tech Stack
- Java 21
- Spring Boot 3.3.x
- Spring Data JPA / Hibernate
- MySQL (H2 for tests)
- JUnit 5, Mockito, MockMvc
- springdoc-openapi (Swagger UI)

## Features
- CRUD operations for showtimes and cinema relationships
- Search showtimes by:
    - Movie title
    - Cinema ID
    - Date & time window
- Business rules:
    - Start time must be before end time
    - No overlapping showtimes in the same screen
    - Ticket price must be non-negative
- Global exception handling for clean JSON errors
- Full testing: unit, repository, and integration

## API Endpoints

| Method | Endpoint | Description |
|---------|-----------|-------------|
| GET | `/api/showtimes/{id}` | Get showtime by ID |
| GET | `/api/showtimes/by-cinema/{cinemaId}` | Get all showtimes by cinema |
| GET | `/api/showtimes/search?q={title}` | Search by movie title |
| GET | `/api/showtimes/window?cinemaId=1&from=...&to=...` | Find by cinema and time window |
| POST | `/api/showtimes` | Create a new showtime |
| PUT | `/api/showtimes/{id}` | Update an existing showtime |
| DELETE | `/api/showtimes/{id}` | Delete a showtime |

## Run Locally

1. Edit:  
   `src/main/resources/application.properties`  
   and add your MySQL username + password.
2. Run this in terminal:
   ```bash
   mvn spring-boot:run
