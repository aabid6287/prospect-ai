# ProspectAI – AI-Powered RHP Analysis Platform

**ProspectAI** is an AI-powered financial research platform that helps users understand and analyze IPO Red Herring Prospectus (RHP) documents using Google Gemini and Retrieval-Augmented Generation (RAG).

Users can upload RHP PDF documents, ask questions about their contents, explore financial information, and understand company-specific risk factors through AI-generated, document-grounded responses.

---

## Features

- **User Authentication:** Secure registration and login using Spring Security and JWT.
- **RHP Document Upload:** Upload and manage IPO prospectus PDF documents.
- **PDF Processing:** Extract text and metadata using Spring AI PDF Document Reader.
- **Intelligent Chunking:** Split extracted content into smaller, searchable text chunks.
- **AI-Powered Embeddings:** Generate vector embeddings using the configured embedding model.
- **Vector Search:** Store and retrieve document embeddings using Pinecone.
- **AI Document Chat:** Ask questions about uploaded RHP documents using Google Gemini.
- **RAG Architecture:** Generate context-aware answers using relevant document content.
- **Source Citations:** Reference supporting document content and page information where available.
- **Financial Analysis:** Extract and understand relevant financial information.
- **Risk Analysis:** Identify and summarize company-specific risk factors from prospectus documents.
- **Chat History:** Maintain conversations related to document analysis.
- **Responsive UI:** Access the platform through a modern React-based interface.

---

## Technology Stack

### Backend

- Java 21
- Spring Boot
- Spring AI
- Spring Security
- JWT Authentication
- Spring Data JPA
- Maven

### AI & RAG

- Google Gemini – AI Chat Generation
- Gemini Embeddings – if configured
- Retrieval-Augmented Generation (RAG)
- Spring AI PDF Document Reader

### Database & Storage

- MySQL – Relational Database
- Pinecone – Vector Database

### Frontend

- React.js
- Vite
- Tailwind CSS
- JavaScript
- Axios (if used in the API layer)

### Development Tools

- IntelliJ IDEA
- VS Code
- Postman
- Git & GitHub

---

## System Architecture

```text
                 User
                   |
                   v
             React Frontend
                   |
                   v
           Spring Boot Backend
                   |
          +--------+--------+
          |                 |
          v                 v
    JWT Authentication   MySQL Database
          |
          v
      Document Upload
          |
          v
   Spring AI PDF Reader
          |
          v
     Text Extraction
          |
          v
     Text Chunking
          |
          v
    Gemini Embeddings
          |
          v
      Pinecone
          |
          v
     User Question
          |
          v
   Relevant Chunk Retrieval
          |
          v
      Gemini Chat
          |
          v
   Contextual AI Response
          |
          v
   Answer with Citations
```

---

## How It Works

### 1. User Authentication

Users register and log in securely. JWT authentication protects private application endpoints.

### 2. RHP PDF Upload

Authenticated users upload their IPO prospectus PDF documents.

### 3. PDF Extraction

Spring AI PDF Document Reader extracts document content and available metadata.

### 4. Text Chunking

The extracted content is divided into smaller chunks to improve retrieval accuracy.

### 5. Embedding Generation

The configured embedding model converts text chunks into numerical vector representations.

### 6. Vector Storage

The generated embeddings and associated metadata are stored in Pinecone.

### 7. Question Answering

When a user asks a question, ProspectAI retrieves relevant chunks from the selected document.

### 8. AI Response Generation

Google Gemini uses the retrieved context to generate relevant answers, with source references where available.

---

## Project Structure

```text
ProspectAI/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       ├── application-dev.properties
│   │   │       └── application-prod.properties
│   │   └── test/
│   ├── pom.xml
│   ├── .gitignore
│   └── .env.example
│
├── frontend/
│   ├── public/
│   ├── src/
│   ├── package.json
│   ├── .gitignore
│   └── .env.example
│
├── docs/
│
├── .gitignore
└── README.md
```

*The structure above is illustrative. Adjust it to match the actual repository.*

---

## Installation and Setup

### Prerequisites

Make sure you have installed:

- Java 21
- Maven
- Node.js and npm
- MySQL
- Git
- Google Gemini API key
- Pinecone account and API key

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/ProspectAI.git

cd ProspectAI
```

### 2. Backend Setup

Navigate to the backend directory:

```bash
cd backend
```

Configure the required environment variables.

Example:

```env
SPRING_PROFILES_ACTIVE=dev

DB_URL=jdbc:mysql://localhost:3306/prospectai
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

GEMINI_API_KEY=your_gemini_api_key
PINECONE_API_KEY=your_pinecone_api_key

JWT_SECRET=your_secure_jwt_secret
```

Add any additional variables required by your actual application configuration.

Create the MySQL database:

```sql
CREATE DATABASE prospectai;
```

Build the application:

```bash
mvn clean install
```

Run the backend:

```bash
mvn spring-boot:run
```

### 3. Frontend Setup

Open another terminal:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Configure the frontend environment variables in `.env` according to your actual Vite configuration.

Example:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Start the frontend:

```bash
npm run dev
```

Open the local URL displayed by Vite in your browser.

---

## Environment Variables

| Variable                 | Description                   |
| ------------------------ | ----------------------------- |
| `SPRING_PROFILES_ACTIVE` | Spring Boot active profile    |
| `DB_URL`                 | MySQL database connection URL |
| `DB_USERNAME`            | MySQL username                |
| `DB_PASSWORD`            | MySQL password                |
| `GEMINI_API_KEY`         | Google Gemini API key         |
| `PINECONE_API_KEY`       | Pinecone API key              |
| `JWT_SECRET`             | JWT signing secret            |
| `VITE_API_BASE_URL`      | Backend API URL for frontend  |

The actual required variables depend on your configuration. Keep all credentials private and never commit `.env` files.

---

## AI Integration

ProspectAI uses Google Gemini through Spring AI for AI-powered document analysis.

The RAG pipeline combines:

- PDF document extraction
- Text chunking
- Embedding generation
- Pinecone vector search
- Gemini-based response generation

This architecture allows the AI to answer questions using retrieved information from the selected RHP document rather than relying only on general model knowledge.

---

## Future Enhancements

- Advanced IPO comparison.
- Automated financial ratio analysis.
- Company valuation insights.
- IPO subscription and market data integration.
- Exportable financial research reports.
- Improved document visualization.
- Multi-document analysis.

---

## Disclaimer

ProspectAI is an AI-powered research and information platform. Its analysis is based on uploaded documents and AI-generated interpretations.

AI-generated information may contain errors or omissions. Users should independently verify important financial information using official documents and consult qualified financial professionals before making investment decisions.

---
<div align="center">

⭐ If you find this project interesting, consider giving it a star!

**Made By Md Aabid ❤️**

</div>
