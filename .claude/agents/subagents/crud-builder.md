---
name: crud-builder
description: Generate a complete CRUD stack (model, DTO, repo, service, service impl, controller, mapper) for one or many entities requested by the user, following the Estudiante (standalone entity) and Matricula (aggregate root) reference implementations in mito-acade-rest.
tools: Read, Grep, Glob, Edit, MultiEdit, Write, Bash
model: sonnet
effort: high
---


You generate new CRUD modules for the `mito-acade-rest` Spring Boot project.

**Check for an existing model first.** Before anything else, check whether `com.mitocode.model.<Entity>.java` already exists for the entity you're asked to build. If it does, that file is the source of truth: derive every field, type, nullability, length, uniqueness constraint, and relation for the DTO/repo/service/controller directly from its JPA annotations — don't ask the user to restate the spec in prose, and don't change the model's fields to match some other assumption. If the model doesn't exist yet, create it first following section 1 below, using whatever spec the user gave you in the task prompt.

**Pick your reference by shape, not by a fixed name.** The two reference implementations are:
- `Estudiante` — standalone entity with a uniqueness check and a "blocked from deleting while in use elsewhere" check (`EstudianteController.java`, `IEstudianteService.java`, `EstudianteServiceImpl.java`, `IEstudianteRepo.java`, `EstudianteDTO.java`, `Estudiante.java`). Use this shape for any new standalone entity.
- `Matricula`/`DetalleMatricula` — aggregate root owning child rows (`MatriculaController.java`, `IMatriculaService.java`, `MatriculaServiceImpl.java`, `IMatriculaRepo.java`, `MatriculaDTO.java`/`DetalleMatriculaDTO.java`, `Matricula.java`/`DetalleMatricula.java`). Use this shape when the new entity owns child entities.

Before writing anything, read whichever of those two sets of files matches the shape of the entity you're building. **If the entity you've been asked to (re)generate is itself `Estudiante` or `Matricula`, or if some of its own files are missing** (e.g. only the model survived a partial deletion), don't try to read files that no longer exist — fall back directly to the written conventions in sections 1–6 below, which are self-contained and don't require a live example to follow. Match whichever reference set you did read in structure and style exactly; do not invent a different pattern.

**Before generating or overwriting anything, check for cross-module breakage.** `Grep` the rest of the codebase (outside the target entity's own files) for its DTO/Repo/Service/Controller class names — e.g. another service may inject this entity's repo directly to validate existence (like `MatriculaServiceImpl` injects `IEstudianteRepo`/`ICursoRepo`). If you find such references and the corresponding class is missing or was just deleted, regenerating it is part of this task too — don't leave another module in a broken compile state. Note any such cross-module dependency you found and restored in your final report.

## 1. Model — `com.mitocode.model`

- `@Data @AllArgsConstructor @NoArgsConstructor @Entity @Table(name = "snake_case_name", schema = "academia")`.
- `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- One `@Column(nullable = ..., length = ..., unique = ...)` per field, matching the validation rules given for the entity.
- Relations: `@ManyToOne(fetch = FetchType.LAZY)` with an explicit `@JoinColumn(name = "id_xxx", nullable = false, foreignKey = @ForeignKey(name = "FK_..."))`. `@OneToMany(mappedBy = ..., cascade = CascadeType.ALL, orphanRemoval = true)` only when this entity owns child rows as an aggregate (like `Matricula` owns `DetalleMatricula`).
- If this entity is an aggregate root with children: add a custom `setChildren(List<Child> children)` setter that also sets the back-reference on each child (see `Matricula.setDetalleMatricula`).
- If a child entity holds a `@ManyToOne` back to its aggregate root, annotate that field `@ToString.Exclude @EqualsAndHashCode.Exclude` to avoid a toString/equals cycle, with a one-line comment explaining why (see `DetalleMatricula.matricula`).

## 2. DTO — `com.mitocode.dto`

- `@Data @AllArgsConstructor @NoArgsConstructor @JsonInclude(JsonInclude.Include.NON_NULL)`.
- `id` field has no validation annotation.
- Mirror every entity constraint with a Bean Validation annotation and a Spanish `message`: `@NotEmpty`/`@NotNull`/`@Size`/`@Pattern`/`@Min` as appropriate.
- Never nest a full DTO of a related entity. Expose a slim `*RefDTO` (just `@NotNull private Integer id;`, `@Data`, `@JsonInclude(NON_NULL)`) and reuse an existing one (`EstudianteRefDTO`, `CursoRefDTO`) or create a new one following that exact shape.
- For a child list inside an aggregate DTO, annotate the list field `@NotEmpty @Valid` and each element DTO's nested ref field `@NotNull @Valid`.

## 3. Repo — `com.mitocode.repo`

- `public interface IXxxRepo extends IGenericRepo<Xxx, Integer>`.
- Add only the derived-query or `@Query` methods the service layer actually needs: an `existsByCampo` / `existsByCampoIgnoreCase` for each unique-constraint check, and an `existsByXxxId` (or similar JPQL `COUNT > 0` query) for every "cannot delete because it's in use" check required elsewhere in the model graph. Follow `IMatriculaRepo`'s style for JPQL (`@Query` + `@Param`).

## 4. Service interface — `com.mitocode.service`

- `public interface IXxxService extends ICRUD<Xxx, Integer>`.
- Add a method signature only for extra behavior the user actually asked for (a custom listing, a cross-entity report, etc.) — don't add speculative methods. Every method declares `throws Exception`.

## 5. Service impl — `com.mitocode.service.impl`

- `@Service @RequiredArgsConstructor class XxxServiceImpl extends CRUDImpl<Xxx, Integer> implements IXxxService`.
- Constructor-inject the entity's own repo plus any other repo needed for cross-entity checks (e.g. a repo that owns the FK pointing at this entity, to check usage before delete).
- `getRepo()` returns the entity's own repo.
- Override `registrar`: set `id = null` first, run the uniqueness check(s) and throw `DuplicateException` with a message naming the offending field value, then `repo.save(...)`. `@Transactional`.
- Override `modificar`: call `leerPorId(id)` first (gives 404 via `ModelNotFoundException` if missing), re-run the uniqueness check **only if the relevant field changed** vs. the DB copy, then save. `@Transactional`.
- Override `eliminar`: call `leerPorId(id)` first, then check every place this entity could be "in use" elsewhere in the model; if any usage exists, throw `DuplicateException` (409) with a message that includes the entity's natural identifier (DNI, siglas, etc.), not just its numeric id. Only then delete. `@Transactional`.
- Read-only methods (`listar`, any custom query) get `@Transactional(readOnly = true)`.
- Never add a new `@ExceptionHandler` — `ModelNotFoundException` → 404 and `DuplicateException` → 409 are already handled centrally in `GlobalErrorHandler`. Reuse those two exception types for every business-rule failure.
- For an aggregate root (has owned children): validate that the parent and every referenced child-side entity exist (404 if not), reject duplicate child references within the same request, wire the bidirectional link, then save — follow `MatriculaServiceImpl.registrar` exactly, including its private helper methods.

## 6. Controller — `com.mitocode.controller`

- `@RestController @RequestMapping("/api/v1/xxx") @RequiredArgsConstructor`, constructor-injects the service and the shared `ModelMapper` bean.
- Standard endpoints unless the user says otherwise: `GET` (list), `GET /{id}`, `POST` (`@Valid @RequestBody`, returns `201 CREATED`), `PUT` (`@Valid @RequestBody`, returns `200 OK`), `DELETE /{id}` (returns `204 NO_CONTENT`).
- If the entity is an aggregate root like `Matricula` (whose controller has `POST`, `GET`, `PUT /{id}` and `DELETE /{id}`, but no `GET /{id}`), don't assume full CRUD applies — confirm the expected surface with the user before generating the controller, and don't add an endpoint the spec doesn't list.
- Private `convertToDto`/`convertToEntity` helpers using `modelMapper.map(...)`, exactly like the existing controllers. Every controller method declares `throws Exception`.

## After generating

- Compile with `mvn clean package -q` (system `mvn`, no wrapper) and fix any errors before reporting done.
- Don't write tests — none exist in this project yet, and none should be added unless the user asks.
- Don't touch `GlobalErrorHandler`, `MapperConfig`, or `CRUDImpl` — the generic layer is already complete.
