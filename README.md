# Mito Academia REST

API REST para la gestión de una academia: estudiantes, cursos y matrículas.
Desarrollada con Spring Boot como proyecto del curso de MitoCode.

## Tecnologías

- Java 25
- Spring Boot [versión]
- Maven
- Lombok
- Jackson
- [Base de datos, por ejemplo PostgreSQL]

## Funcionalidades

- CRUD de estudiantes
- CRUD de cursos
- Registro de matrículas con su detalle de cursos
- [Otras: validaciones, manejo de excepciones, consultas especiales, etc.]

## Modelo de datos

| Entidad | Descripción |
|---|---|
| Estudiante | Datos del alumno |
| Curso | Cursos disponibles en la academia |
| Matricula | Inscripción de un estudiante |
| DetalleMatricula | Cursos incluidos en cada matrícula |

## Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| GET | /estudiantes | Lista todos los estudiantes |
| POST | /estudiantes | Registra un estudiante |
| GET | /cursos | Lista todos los cursos |
| POST | /matriculas | Registra una matrícula |
| [...] | [...] | [...] |

## Cómo ejecutarlo

1. Clona el repositorio:
   ```bash
   git clone https://github.com/rmontalvo6/mito-academia-rest.git
   ```
2. Crea la base de datos `[nombre_bd]`.
3. Configura usuario y contraseña en `src/main/resources/application.yaml`.
4. Ejecuta el proyecto:
   ```bash
   ./mvnw spring-boot:run
   ```
5. La API queda disponible en `http://localhost:8080`.

## Pruebas con Postman

En la carpeta [`postman/`](postman/) está la colección con todas las peticiones.
Impórtala en Postman con **Import** y ejecuta los requests.

## Autor
Renzo Montalvo - https://www.linkedin.com/in/renzo-alejandro-montalvo-quispe-1b57a4130/
