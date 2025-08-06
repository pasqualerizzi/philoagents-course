# Use an official OpenJDK runtime as a parent image
FROM ubuntu/jre:17_edge

# Set the working directory in the container
WORKDIR /philoagents-spring-api

# Copy the jar file into the container
COPY target/*.jar app.jar

# Expose port 8080
EXPOSE 8000

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]