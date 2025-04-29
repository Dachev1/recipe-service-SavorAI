# SavorAI - Recipe Service

## Related Repositories
- [SavorAI](https://github.com/Dachev1/SavorAI) - Frontend application
- [user-service-SavorAI](https://github.com/Dachev1/user-service-SavorAI) - User authentication and management service 

## Overview

The Recipe Service is a core microservice in the SavorAI platform, responsible for recipe management, AI-powered recipe generation, and related operations. Built with Spring Boot and Java, this service provides robust API endpoints for creating, retrieving, updating, and searching recipes.

## Features

- **Recipe Management**: Complete CRUD operations for recipes
- **AI-Powered Recipe Generation**: Leveraging Spring AI and OpenAI integration to create unique recipes
- **Recipe Search**: Advanced search capabilities with filtering options
- **Favorites Management**: Allow users to mark recipes as favorites
- **Comments System**: Support for user comments on recipes
- **Image Processing**: Handle recipe image uploads and storage
- **Security**: JWT-based authentication and authorization
- **Documentation**: Comprehensive API documentation with Springdoc OpenAPI

## Tech Stack

- **Framework**: Spring Boot 3.4.2
- **Language**: Java 17
- **Database**: MySQL
- **ORM**: Spring Data JPA
- **Security**: Spring Security with JWT
- **API Documentation**: Springdoc OpenAPI
- **AI Integration**: Spring AI with OpenAI
- **Cloud Storage**: Cloudinary for image storage
- **Client Communication**: Spring Cloud OpenFeign
- **Build Tool**: Gradle
- **Testing**: JUnit, Spring Boot Test

## Architecture

The Recipe Service follows a layered architecture approach:

```
dev.idachev.recipeservice/
├── config/       # Configuration classes for Spring components
├── exception/    # Custom exceptions and error handling
├── infrastructure/ # Infrastructure concerns (caching, events)
├── model/        # Domain entities and business objects
├── repository/   # Data access layer with JPA repositories
├── service/      # Business logic services
├── user/         # User-related functionality
├── util/         # Utility classes and helper functions
├── web/          # REST controllers, DTOs, and request/response mapping
└── Application.java # Main application class
```

## API Endpoints

The service exposes the following main API endpoints:

- **Recipe Management**:
  - `GET /api/recipes` - List recipes with pagination and filtering
  - `GET /api/recipes/{id}` - Get recipe by ID
  - `POST /api/recipes` - Create new recipe
  - `PUT /api/recipes/{id}` - Update existing recipe
  - `DELETE /api/recipes/{id}` - Delete recipe

- **AI Recipe Generation**:
  - `POST /api/recipes/generate` - Generate recipe using AI

- **Favorites**:
  - `GET /api/favorites` - Get user's favorite recipes
  - `POST /api/favorites/{recipeId}` - Add recipe to favorites
  - `DELETE /api/favorites/{recipeId}` - Remove recipe from favorites

- **Comments**:
  - `GET /api/recipes/{recipeId}/comments` - Get comments for a recipe
  - `POST /api/recipes/{recipeId}/comments` - Add comment to a recipe
  - `PUT /api/comments/{commentId}` - Update a comment
  - `DELETE /api/comments/{commentId}` - Delete a comment

## Security

The service uses OAuth2 Resource Server for JWT validation. Authentication is handled by the User Service, and this service validates the JWT tokens. The following security features are implemented:

- Token-based authentication
- Role-based access control
- Method-level security
- CORS configuration
- Request validation

## Getting Started

### Prerequisites

- JDK 17+
- MySQL 8.0+
- Gradle 8.0+
- OpenAI API key (for AI recipe generation)
- Cloudinary account (for image storage)

### Configuration

Create an `.env.properties` file in the `src/main/resources` directory with the following variables:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/savorai_recipes
spring.datasource.username=root
spring.datasource.password=yourpassword

# JWT
jwt.public.key=classpath:keys/public_key.pem

# OpenAI
spring.ai.openai.api-key=your-openai-api-key
spring.ai.openai.model=gpt-4-1106-preview

# Cloudinary
cloudinary.cloud-name=your-cloud-name
cloudinary.api-key=your-api-key
cloudinary.api-secret=your-api-secret

# User Service
user-service.url=http://localhost:8081
```

### Building and Running

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/recipe-service-SavorAI.git
   cd recipe-service-SavorAI
   ```

2. Build the application:
   ```bash
   ./gradlew build
   ```

3. Run the application:
   ```bash
   ./gradlew bootRun
   ```

4. Access the OpenAPI documentation at `http://localhost:8082/swagger-ui.html`

## Testing

Run the tests with the following command:

```bash
./gradlew test
```

The service includes unit tests, integration tests, and controller tests to ensure reliability.

## Deployment

The service can be deployed as a standalone Spring Boot application, as a Docker container, or in a Kubernetes cluster.

### Docker Deployment

1. Build the Docker image:
   ```bash
   ./gradlew bootBuildImage --imageName=savorai/recipe-service
   ```

2. Run the Docker container:
   ```bash
   docker run -p 8082:8082 savorai/recipe-service
   ```

### Kubernetes Deployment

1. Create a Kubernetes deployment file (`recipe-service-deployment.yaml`):
   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: recipe-service
   spec:
     replicas: 2
     selector:
       matchLabels:
         app: recipe-service
     template:
       metadata:
         labels:
           app: recipe-service
       spec:
         containers:
         - name: recipe-service
           image: savorai/recipe-service:latest
           ports:
           - containerPort: 8082
           env:
           - name: SPRING_PROFILES_ACTIVE
             value: "prod"
   ```

2. Apply the deployment:
   ```bash
   kubectl apply -f recipe-service-deployment.yaml
   ```

## Performance Considerations

- The service implements caching for frequently accessed data
- Database query optimization with indexed fields
- Connection pooling for database efficiency
- Paginated APIs for large data sets
- Asynchronous processing for AI recipe generation

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m 'Add some feature'`
4. Push to the branch: `git push origin feature/your-feature-name`
5. Open a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
