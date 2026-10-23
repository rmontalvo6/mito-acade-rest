# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack and commands

Spring Boot 4.1.0, Java 25, Maven, Spring Data JPA, PostgreSQL, Lombok, ModelMapper. The project has no Maven wrapper and no tests yet, so use a system `mvn`:

- Run: `mvn spring-boot:run` (main class `com.mitocode.AcademiaApplication`, port **9595**)
- Build: `mvn clean package`
- Tests (none exist yet): `mvn test`, or a single test with `mvn test -Dtest=ClassName#method`

Runtime needs a local PostgreSQL database `academia` (user `postgres`, see `src/main/resources/application.yaml`). `ddl-auto: update` builds the tables. The entities use `schema = "academia"`, so the `academia` schema must exist in that database, or Hibernate will fail on startup. Virtual threads are enabled.

## Architecture

Layered REST API under `com.mitocode`, with a Spanish-language domain (Estudiante, Curso, Matricula and its DetalleMatricula). Endpoints live under `/api/v1/...`.

- **Generic CRUD base:** `IGenericRepo<T,ID>` → `ICRUD<T,ID>` → abstract `CRUDImpl<T,ID>`. A concrete service extends `CRUDImpl`, implements `getRepo()`, and overrides `registrar`/`modificar`/`eliminar` to add business rules. Every service method declares `throws Exception`, as do the controller methods. Errors are handled centrally rather than per call.
- **Controllers** take and return DTOs (`dto/`) and convert with the shared `ModelMapper` bean (`config/MapperConfig`) via private `convertToDto`/`convertToEntity` helpers. Services work only with entities. The `*RefDTO` classes are slim nested references used inside `MatriculaDTO`/`DetalleMatriculaDTO`.
- **Listings:** all three modules expose an unpaginated `GET` that returns the full table, and `Curso` additionally offers `GET /api/v1/curso/pageable` → `Page<CursoDTO>` (`?page=0&size=20&sort=nombre,asc`). The unpaginated listing was briefly removed from `Curso` and then **restored at the user's request**: an unbounded `findAll()` materializes the whole table twice, as entities and as DTOs, which is a known and accepted tradeoff here — don't remove that endpoint again without being asked. `Estudiante` also has `GET /api/v1/estudiante/ordenados-edad`, and `Matricula` has two reports: `GET /api/v1/matricula/cursos-estudiantes` (`Map<curso, List<estudiante>>`) and `GET /api/v1/matricula/estudiantes-cursos-activos` (`Map<estudiante, List<curso activo>>`, filtered to `Curso.estado = true`). A report that shows both sides of the enrolment belongs to `Matricula`, not to `Estudiante` or `Curso`.
- **Errors:** `GlobalErrorHandler` (`@RestControllerAdvice`) holds 11 handlers, all answering with `CustomErrorResponse`: `ModelNotFoundException` → 404; `DuplicateException` → 409; `MethodArgumentNotValidException` → 400 with one readable message per field; `MethodArgumentTypeMismatchException`, `HttpMessageNotReadableException` and `IllegalArgumentException` → 400; `PropertyReferenceException` → 400 (a `?sort=` naming a property that does not exist, reported with `getPropertyName()` so the domain class is not leaked); `HttpRequestMethodNotSupportedException` → 405; `DataIntegrityViolationException` → 409 as a safety net for constraints a service did not get to check; `ObjectOptimisticLockingFailureException` → 409, inactive on purpose because no entity carries `@Version` (the lost-update risk on two concurrent PUTs is an accepted tradeoff — do not add it); and any other `Exception` → 500, logged in full but answered with a neutral message. So `DuplicateException` is how a *service* signals a 409, including "cannot delete because it is in use", but it is not the only source of one.
- **Matricula aggregate:** `Matricula` (header) owns `DetalleMatricula` (lines) with `cascade = ALL, orphanRemoval = true`. The bidirectional link is set in `Matricula.setDetalleMatricula` and again in `MatriculaServiceImpl.registrar`. Registering checks that the student and each course exist (404) and rejects a student already enrolled in a course (409) through `IMatriculaRepo.existsByEstudianteAndCurso`. `GET /api/v1/matricula/cursos-estudiantes` builds a `Map<course name, List<student name>>` with Streams in `obtenerCursosConEstudiantes()`.

## Conventions for service rules

`.claude/agents/subagents/crud-builder.md` defines the conventions for CRUD modules, and existing services follow them:

- `registrar` always sets `id = null` first.
- `modificar` and `eliminar` call `leerPorId(id)` first so a missing resource gives 404.
- Deleting an `Estudiante` with matrículas throws `DuplicateException` (409) with the student's DNI in the message.
- Deleting a `Curso` that is used in matrícula details also returns 409. `CursoServiceImpl.eliminar` enforces this via `IMatriculaRepo.existsDetalleByCursoId`, mirroring the `Estudiante` check.
- Unique checks: `Estudiante.dni` (`existsByDni`) and `Curso.siglas` (`existsBySiglas`), each backed by a DB unique constraint. The two services reach the same outcome by different means, and neither is a bug: `EstudianteServiceImpl.modificar` queries only when the DNI actually changed (`!equalsIgnoreCase`), while `CursoServiceImpl.modificar` always queries with `existsBySiglasAndIdNot`, which excludes the row itself.
- `CursoServiceImpl` normalizes text before every uniqueness check: the non-breaking spaces U+00A0, U+2007 and U+202F become ordinary spaces, then `strip()`, then `siglas` go uppercase. `Character.isWhitespace()` excludes those three on purpose, so neither `strip()` nor `isBlank()` ever sees them — which is also why `CursoDTO` needs the `(?U)` flag in its `@Pattern`, so that `\S` follows the Unicode white-space property. Without both, `"<NBSP>MAT101"` outlived every check and coexisted with `MAT101` as a duplicate the unique constraint could not see.
- `CursoServiceImpl.validar` is the service-level net for callers that bypass the controller's `@Valid`: an empty `nombre`/`siglas` or a null `estado` throws `IllegalArgumentException` → 400, instead of reaching the flush as a not-null violation and surfacing as a 409.
- The `catch (DataIntegrityViolationException)` blocks in `CursoServiceImpl` translate to `DuplicateException` only when the PostgreSQL SQLState matches — 23505 (unique) in `registrar`/`modificar`, 23503 (foreign key) in `eliminar` — and rethrow anything else, so a not-null or an unrelated FK is never disguised as a duplicate. SQLState is used instead of the constraint name because `ddl-auto` names foreign keys with a hash that can change.

## Subagents and skills

Everything that exists under `.claude/` is these four files (plus `settings.local.json`):

- `.claude/agents/subagents/crud-builder.md` — scaffolds a brand-new entity's full CRUD stack (model, DTO, repo, service, impl, controller), following Estudiante as the standalone reference and Matricula as the aggregate-root one.
- `.claude/agents/subagents/query-builder.md` — adds a custom, non-CRUD query or report to a module that already exists, in one of four shapes: a filtered listing (no example in the project yet), a JOIN FETCH listing (`IMatriculaRepo.obtenerMatriculasConRelaciones`, or `obtenerMatriculasConCursosActivos` for the filtered variant), a business-rule `exists` check (`existsByEstudianteAndCurso`, `existsDetalleByCursoId`), or a Streams aggregation/ordering report (`MatriculaServiceImpl.obtenerCursosConEstudiantes`). It edits only the module's existing repo/service/impl/controller and **never creates a file** — its `tools` list has no `Write`, which enforces that mechanically. The one CRUD method it may override is `listar()`, to delegate to a JOIN FETCH query.
- `.claude/skills/qa-review/SKILL.md` — the read-only `/qa-review <Entidad>` skill. It audits one module class by class (model → dto → repo → service → impl → controller), focuses on error and exception handling, and reports each finding with its line, severity and failure scenario. It never edits, creates or deletes files.
- `.claude/skills/code-cleaner/SKILL.md` — the `/code-cleaner <Name> [preview]` skill. It removes commented-out code and normalizes formatting in every Java class whose name contains the argument, without changing behavior; it edits existing files only, and `preview` makes it report instead of edit.

The decision tree is: a new entity → `crud-builder`; a new query or report on an entity that already exists → `query-builder`; a feature that needs both → `crud-builder` first, then `query-builder` on the module it just created. There is no `qa-tester` and no `README` agent.
