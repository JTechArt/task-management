FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY pom.xml .
COPY core/pom.xml core/pom.xml
COPY desktop-app/pom.xml desktop-app/pom.xml
RUN mvn -B -pl desktop-app -am dependency:go-offline
COPY . .
RUN mvn -B -pl desktop-app -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/desktop-app/target/desktop-app-*.jar /app/app.jar
ENV APP_NAME=TaskManager
ENV DB_HOST=postgres
ENV DB_PORT=5432
ENV DB_NAME=taskmanager
ENV DB_USER=taskmanager
ENV DB_PASSWORD=taskmanager_local
ENTRYPOINT ["java","-jar","/app/app.jar"]

