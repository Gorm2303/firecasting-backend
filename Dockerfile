# ---------- Build (JDK 23) ----------
FROM maven:3.9-eclipse-temurin-23 AS build
WORKDIR /src
# Copy entire repo so Maven sees parent + all modules
COPY . .

# Run the SAME command your IDE does, but pointing at the firecasting pom
# (equivalent to: cd firecasting && mvn -DskipTests package)
RUN mvn -q -DskipTests -f firecasting/pom.xml package

# ---------- Run (JRE 23) ----------
FROM eclipse-temurin:23-jre
WORKDIR /app
# Copy the Spring Boot jar from application module (wildcard for future versions)
COPY --from=build /src/firecasting/application/target/application-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-XX:+UseContainerSupport","-jar","/app/app.jar"]
