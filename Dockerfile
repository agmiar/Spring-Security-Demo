# ETAPA 1: Construcción (Build)
# Se usa JDK para compilar
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR .

# Copiar archivos de configuración de Maven
COPY ./pom.xml .
COPY ./.mvn ./.mvn
COPY ./mvnw .

# Descargar dependencias
RUN ./mvnw dependency:go-offline

# Copiar código fuente y construir el JAR
COPY ./src ./src
RUN ./mvnw clean package -DskipTests

# ETAPA 2: Ejecución (Runtime)
# En la imagen final solo queda el JAR sobre un JRE ligero
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar solo el JAR compilado de la etapa anterior (build)
# TODO: Verificar que el nombre del JAR en /root/target/ sea el correcto
COPY --from=build /target/SpringSecurityApp-0.0.1-SNAPSHOT.jar app.jar

# Informar el puerto (Metadata)
EXPOSE 8080

# Levantar la aplicación cuando el contenedor inicie
ENTRYPOINT ["java","-jar","app.jar"]