# PhiloAgents Spring API Fork

This project contains a Spring Boot microservice "philoagents-java-api" integrated with SPRING AI framework that wants to enrich the **PhiloAgents** course by <b><a href="https://theneuralmaze.substack.com/">The Neural Maze</a></b> offering a Java version of the Python backend "philoagents-api" present in the original project.
The goal of this document is to describe how this backend API is done and how to use this backend instead of the original python one provided in the project mentioned above.

I kept the Python backend to demonstrate that it's exactly replaceable by the Java Backend without changing a single line of the frontend service or the long-memory collection structure. 

It provides RESTful APIs and a (non stomp) websocket endpoint for managing and interacting with philosophical agents based on Java technologies instead of the original python ones.
The service is designed for modularity and scalability, making it easy to integrate with other microservices in the ecosystem.

## Technologies Used

- **Spring Boot**: Rapid application development and REST API support.
- **Spring AI**: Integrates AI capabilities into Spring applications.
- **langgraph4j**: Enables graph-based language model workflows in Java. Visit: <b><a href="https://github.com/langgraph4j/langgraph4j">bsorrentino's repo</a></b> for more infos about this java porting.
- **Advanced RAG with Mongo Vector Search and embeddings**: Efficient vector-based search capabilities using MongoDB and embeddings from HuggingFace.
- **Maven**: Build and dependency management.
- **Docker**: Containerization for deployment. I'm using ubuntu/jre:17_edge image because of dependency from libstdc++ and glibc libraries for embeddings
- **Docker Compose**: Orchestration of multiple microservices.

# 🎯 Getting Started

## 1. Clone the Repository

Start by cloning the repository and navigating to the `philoagents-spring-api` project directory:

```bash
git clone https://github.com/pasqualerizzi/philoagents-course.git
cd philoagents-java-api
```

### 2. Installation

```bash
run mvn clean install
```

### 3. Environment configuration

Add .env file into philoagents-java-api folder with your api key using the property "SPRING_AI_OPENAI_API-KEY"


# 📁 Project Structure

The project follows a clean architecture structure commonly used in production Spring projects:

```bash
src
    main
    ├── java/spring/ai/philoagents/     # Main package directory
    │   ├── config/                     # Configuration settings
    │   ├── controllers/                # Presentation layer (REST controllers)
    │   ├── entities/                   # Domain entities
    │   ├── handlers/                   # Exception or event handlers
    │   ├── override/                   # MongoDBVectoreStore override to adapt to the original collection structure
    │   ├── repository/                 # Data access layer (repositories)
    │   ├── services/                   # Service layer (business logic)
    │   ├── tools/                      # Utility classes/tools
    │   ├── workflow/                   # Workflow/business process logic                    
    │   ├── Application.java            # Main application entry point
    │   resources/                      # Application resources (e.g., application.properties)
    test                                # Test classes
├── Dockerfile                          # API Docker image definition
└── pom.xml                             # Project dependencies
```

# 🏗️ Set Up Your Local Infrastructure 

We can use Docker to set up the local infrastructure (Game UI, Agent API (python or java), MongoDB, Prometheus, Graphana).

> [!WARNING]
> Before running the command below, ensure you do not have any processes running on ports `27017` (MongoDB), `8000` (Agent API) and `8080` (Game UI).

From the root `philoagents-course` directory, to start the Docker infrastructure with the Java backend, run:
```bash
make java-infrastructure-up
```

From the root `philoagents-course` directory, to stop the Docker infrastructure, run:
```bash
make infrastructure-stop
```

From the root `philoagents-course` directory, to build the Docker images (without running them), run:
```bash
make infrastructure-build
```


# ⚡️ Running the Code

After you have set up your environment and local infrastructure (through Docker), you are ready to run and test out the game simulation.
But before starting you need data. You need long-term-memory data. As mentioned in the original guide:

>First, from the root `philoagents-course` directory, populate the long term memory within your MongoDB instance (required for agentic RAG) with the following command:
>```bash
>make create-long-term-memory
>```

Once you completed the previous step, connect locally your MongoDBCompass client and recreate the VECTORSEARCH "vector_index" inside <b>philosopher_long_term_memory</b> collection adding all the fields you need to apply as filters (only philosopher_name for this purpose) otherwise you will receive an error saying that <i>Path 'philosopher_name' needs to be indexed as token", "code": 8</i>:

```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding",
      "numDimensions": 384,
      "similarity": "dotProduct"
    },
    {
      "type": "filter",
      "path": "philosopher_name"
    }
  ]
}
```

Now you're ready to use your Java backend.

Next, you can access the game by typing in your browser:
```
http://localhost:8080
```

## 🐝 API Documentation

Once running, the SWAGGER API documentation will be available at `http://localhost:8000/swagger-ui/index.html`.

## 🦜🕸️ Langgraph studio

You can play with the agent workflow visiting `http://localhost:8000/?instance=default` as described in <a href="https://github.com/langgraph4j/langgraph4j/tree/main/studio">this document</a>

---

Feel free to contribute or customize or suggest fixes (particularly about this file)!