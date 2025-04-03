# 🍲 SavorAI Recipe Service

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.2-brightgreen?style=flat-square&logo=springboot)
![OpenAI](https://img.shields.io/badge/OpenAI-Powered-blue?style=flat-square&logo=openai)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)

**AI-powered recipe generator and management service built with Spring Boot**

</div>

---

## 📋 Table of Contents
- [Overview](#-overview)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [API Endpoints](#-api-endpoints)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [Security](#-security)
- [Contributing](#-contributing)

---

## 🔍 Overview

SavorAI is a robust backend service for recipe generation and management with OpenAI integration. The service allows users to generate custom recipes from ingredients, store favorite recipes, and manage their personal recipe collection with a modern API.

---

## ✨ Features

- 🤖 **AI-powered recipe generation** - Create unique recipes from a list of ingredients
- 🖼️ **Recipe image generation** - Automatically generate appetizing images for recipes
- 📝 **Complete recipe management** - Create, read, update, and delete your recipes
- ⭐ **Favorites functionality** - Save and organize your favorite recipes
- 🔍 **Advanced search capabilities** - Find recipes by keyword and filtering options
- 🔐 **JWT Authentication** - Secure user authentication and authorization
- 🌐 **Integration with User Service** - Seamless connection with user management service

---

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 17 | Core programming language |
| Spring Boot 3.4.2 | Application framework |
| Spring Data JPA | Database access layer |
| Spring Security | Authentication with JWT |
| Spring AI | OpenAI integration for recipe generation |
| Spring Cloud OpenFeign | Service-to-service communication |
| MySQL | Relational database |
| Cloudinary | Cloud image storage |
| Gradle | Build automation |
| Springdoc OpenAPI | API documentation |

---

## 📂 Project Structure

```
src/
├── main/
│   ├── java/dev/idachev/recipeservice/
│   │   ├── config/        # Application configuration
│   │   ├── exception/     # Custom exceptions
│   │   ├── infrastructure/# Infrastructure services
│   │   ├── mapper/        # Object mappers
│   │   ├── model/         # Domain models and DTOs
│   │   ├── repository/    # Data access repositories
│   │   ├── service/       # Business logic services
│   │   ├── user/          # User-related components
│   │   ├── util/          # Utility classes
│   │   ├── web/           # REST controllers
│   │   └── Application.java  # Entry point
│   └── resources/
│       └── application.yml   # Application configuration
└── test/                  # Unit and integration tests
```

---

## 🌐 API Endpoints

### Recipe Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/recipes` | Retrieve all recipes with pagination |
| `GET` | `/api/v1/recipes/{id}` | Get a specific recipe by ID |
| `POST` | `/api/v1/recipes` | Create a new recipe |
| `PUT` | `/api/v1/recipes/{id}` | Update an existing recipe |
| `DELETE` | `/api/v1/recipes/{id}` | Delete a recipe |

### AI Features

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/recipes/generate` | Generate a recipe from ingredients |

### Favorites

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/favorites` | Get user's favorite recipes |
| `POST` | `/api/v1/favorites/{recipeId}` | Add recipe to favorites |
| `DELETE` | `/api/v1/favorites/{recipeId}` | Remove recipe from favorites |

---

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- MySQL database
- OpenAI API key
- Cloudinary account (for image storage)

### Running the Application

1. Clone the repository:
   ```bash
   git clone https://github.com/idachev/savorai-recipe-service.git
   cd savorai-recipe-service
   ```

2. Set up environment variables:
   ```
   DB_USERNAME=your_db_username
   DB_PASSWORD=your_db_password
   OPENAI_API_KEY=your_openai_api_key
   JWT_SECRET=your_jwt_secret
   CLOUDINARY_CLOUD_NAME=your_cloud_name
   CLOUDINARY_API_KEY=your_api_key
   CLOUDINARY_API_SECRET=your_api_secret
   ```

3. Build and run the application:
   ```bash
   ./gradlew bootRun
   ```

4. The service will be available at:
   ```
   http://localhost:8082
   ```

---

## ⚙️ Configuration

The application can be configured using environment variables or by modifying the `application.yml` file:

```yaml
server:
  port: 8082

spring:
  application:
    name: recipe-service
  datasource:
    url: jdbc:mysql://localhost:3306/savorai_recipe?createDatabaseIfNotExist=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}
  jpa:
    hibernate:
      ddl-auto: update
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-3.5-turbo
          temperature: 0.7

jwt:
  secret: ${JWT_SECRET}

# Integration with user service
app:
  services:
    user-service:
      url: http://localhost:8081
```

### API Documentation

Access the Swagger UI to explore and test the API:
```
http://localhost:8082/swagger-ui/index.html
```

---

## 🔒 Security

The service implements JWT token-based authentication. All API endpoints (except Swagger documentation) require a valid JWT token in the Authorization header.

Authentication format:
```
Authorization: Bearer your_jwt_token
```

The service integrates with a separate User Service for authentication and user management.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

<div align="center">
  <p>Made with ❤️ by the SavorAI Team</p>
</div> 