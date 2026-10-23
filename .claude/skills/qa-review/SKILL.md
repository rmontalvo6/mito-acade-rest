---
name: qa-review
description: QA tester that audits a full CRUD module from the entity name (Curso, Estudiante, Matricula or any new one). Finds its classes in model, dto, repo, service, service/impl and controller, focuses on error and exception handling, and prints findings class by class as it goes. Use when the user asks to review, audit or find bugs in an entity or module. Read-only.
argument-hint: <Entity>
---

# QA review of module: $ARGUMENTS

If no entity was given, ask which one and stop.

## 1. Locate the classes
Search under `src/main/java` for every `.java` file whose name contains `$ARGUMENTS`, in the `model`, `dto`, `repo`, `service`, `service/impl` and `controller` packages (e.g. `Curso`, `CursoDTO`, `ICursoRepo`, `ICursoService`, `CursoServiceImpl`, `CursoController`). Also include child classes of the aggregate that contain the name (e.g. `DetalleMatricula` for `Matricula`).

Before starting, print the list of files found. If a layer is missing, report it as a finding.

## 2. Review one class at a time and print results immediately
Order: model → dto → repo → service → service/impl → controller.
Read one class, print its findings block, and only then open the next one. Do not accumulate everything for the end and do not read files in parallel.

Read as context only (do not report on them unless the defect affects this module): `GlobalErrorHandler`, the custom exception classes, `CRUDImpl`/`ICRUD` and `IGenericRepo`.

## 3. Main focus: errors and exceptions
Across the whole module, verify:
- **Find/update/delete a non-existent id** throws `ModelNotFoundException` (404) and does not return `null`, an empty `Optional`, a 200 with an empty body, or a 500.
- **Duplicates** (name, DNI, etc.) throw `DuplicateException` (409), and a DB unique constraint backs the check with the same case sensitivity.
- **DB violations** (`DataIntegrityViolationException`: FK when deleting a course that has enrollments, unique, not null) do not end in a generic 500.
- **Validation**: the controller uses `@Valid` on `@RequestBody`; DTO constraints match the columns; `@NotBlank` on text fields; `GlobalErrorHandler` returns 400 with readable per-field messages.
- **Invalid references in the body** (e.g. an enrollment with an `idEstudiante` or `idCurso` that does not exist) are detected and answered with 404/400, not 500.
- **Path `id` vs body `id`** on update: what happens when they don't match.
- **`GlobalErrorHandler`** covers every exception the module throws; no exception is swallowed; client messages don't expose stack traces or internal framework messages.
- **Success status codes** are consistent: 201 on create, 200 on read/update, 204 on delete.

## 4. Secondary review (only where it applies to the class)
`@Transactional(readOnly = true)` on reads; lazy associations accessed outside a session; N+1 queries; `registrar` nulls the `id`; back-reference set when saving children; duplicate children in a single request; cascade/`orphanRemoval`; `@Version` on editable entities; mutable shared state (virtual threads are enabled); collections without pagination.

## Output format
For each class:
```
### <file path>
- line N · HIGH|MEDIUM|LOW · one-line defect
  scenario: input/state → wrong outcome
```
If the class is clean: `no findings` + half a line on what you verified.

Mark anything inferred from framework behavior without executing it as `(unconfirmed)` and say how to confirm it (e.g. which request to send).

At the end, at most 4 lines: count by severity and the single highest-priority fix.

Write the findings in the user's language.

## Rules
- Read-only: never edit, create or delete files.
- If `CLAUDE.md` contradicts the code, the code wins.