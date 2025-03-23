# 🍲 SavorAI Recipe Service

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-brightgreen?style=flat-square&logo=springboot)
![OpenAI](https://img.shields.io/badge/OpenAI-Powered-blue?style=flat-square&logo=openai)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)

**AI-powered recipe generator and management service built with Spring Boot**

</div>

---

## 📋 Table of Contents
- [Overview](#-overview)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [API Endpoints](#-api-endpoints)
- [Getting Started](#-getting-started)
- [Security](#-security)

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

---

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 17 | Core programming language |
| Spring Boot 3.2.5 | Application framework |
| Spring AI | OpenAI integration |
| Spring Security | Authentication with JWT |
| Spring Data JPA | Database access layer |
| MySQL | Relational database |
| Cloudinary | Cloud image storage |
| Gradle | Build automation |

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
- Cloudinary account

### Configuration

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/savorai-recipe-service.git
   cd savorai-recipe-service
   ```

2. Configure environment variables in `application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/recipedb
       username: your_username
       password: your_password
       
   openai:
     api-key: your_openai_api_key
     
   cloudinary:
     cloud-name: your_cloud_name
     api-key: your_api_key
     api-secret: your_api_secret
     
   jwt:
     secret: your_jwt_secret
   ```

### Running the Application

```bash
./gradlew bootRun
```

### API Documentation

Access the Swagger UI to explore and test the API:
```
http://localhost:8082/swagger-ui.html
```

---

## 🔒 Security

The service implements JWT token-based authentication. All API endpoints (except Swagger documentation) require a valid JWT token in the Authorization header.

Authentication format:
```
Authorization: Bearer your_jwt_token
```

---

<div align="center">
  <p>Made with ❤️ by the SavorAI Team</p>
</div> 