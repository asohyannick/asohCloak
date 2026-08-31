FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:25-jre AS layers

WORKDIR /application
COPY --from=build /workspace/target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher

FROM eclipse-temurin:25-jre AS runtime

RUN groupadd --system asohcloak && useradd --system --gid asohcloak asohcloak
USER asohcloak

WORKDIR /application

COPY --from=layers --chown=asohcloak:asohcloak /application/application/dependencies/ ./
COPY --from=layers --chown=asohcloak:asohcloak /application/application/spring-boot-loader/ ./
COPY --from=layers --chown=asohcloak:asohcloak /application/application/snapshot-dependencies/ ./
COPY --from=layers --chown=asohcloak:asohcloak /application/application/application/ ./

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]