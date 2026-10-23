---
name: code-cleaner
description: Removes commented-out code and normalizes formatting in every Java class whose name contains the given argument (e.g. "Curso"), across all packages of the mito-academia-rest project (Spring Boot, Java 25, Lombok), without changing behavior. Use when the user asks to clean, tidy up, remove commented code or format classes. Edits existing files only.
argument-hint: <Name> [preview]
allowed-tools: Read, Grep, Glob, Edit, MultiEdit, Bash
---

# Code cleanup: $ARGUMENTS

If no name was given, ask which one and stop.

The first word of the arguments is the **name**. If the arguments also contain `preview`, `dry run` or `solo reporte`, run in **dry-run mode**: edit nothing, only report what you would change.

You clean up existing Java classes. **You never change behavior**: afterwards the code must do exactly what it did before. If a change could alter behavior, don't make it — report it. Never create or delete files.

## 1. Resolve the files

Search **every package** under `src/main/java` for `.java` files whose file name contains the name, case-sensitive. This works for any entity. For a name `<Name>` that means `<Name>`, `<Name>DTO`, `<Name>RefDTO`, `I<Name>Repo`, `I<Name>Service`, `<Name>ServiceImpl`, `<Name>Controller`, and any other class in any package that contains `<Name>` in its name.

Print the list of files found before starting. If nothing matches, say so and stop.

Never touch `src/test`, `pom.xml`, `application.yaml`, `CLAUDE.md` or any non-Java file.

Run `git status --porcelain`. If any file in the list has uncommitted changes, stop and tell the user to commit first (not needed in dry-run mode).

Process the files **one at a time**: clean one, print a short summary for it, then move to the next.

## 2. Remove commented-out code

Remove a comment only when it is **dead code**: it would compile, or nearly compile, as Java if uncommented. Signs: ends in `;`, `{` or `}`; starts with an annotation, `return`, `if`, `for`, `public`, `private`, an assignment or a method call; an old import; an alternative implementation of a method.

**Always keep:** Javadoc (`/** ... */`); comments explaining *why*; `TODO`, `FIXME`, `NOTE`, `HACK`; license headers; `noinspection` / `@formatter` markers.

If a comment mixes explanation and code, or you are not sure, **keep it** and list it as "doubtful" with its line number.

## 3. Format

IntelliJ's default Java style:
- 4-space indentation, no tabs; K&R braces.
- One blank line between methods and between field groups; never two consecutive blank lines; no blank line right after `{` or right before `}`.
- No trailing whitespace; file ends with exactly one newline.
- One space after `if`/`for`/`while`/`catch`, around binary operators and after commas.
- Annotations on their own line above classes, methods and fields (short parameter annotations stay inline, e.g. `@Valid @RequestBody`).
- Multi-line JPQL stays inside its `"""` text block; don't reflow it.

**Imports:** remove unused ones (confirm with `Grep` in the same file, including annotations and generics); order `java.*`, `jakarta.*`, third-party, `com.mitocode.*`, one blank line between groups, static imports last. Don't expand or collapse wildcard imports.

**Never:** rename anything, reorder methods or fields, change visibility, modifiers or annotations, rewrite code into "cleaner" equivalents, touch string literals, JPQL contents or messages, or fix bugs — list bugs in the report instead.

## 4. Verify

1. Run `git diff -w` on the edited files. Ignoring whitespace, the only allowed differences are removed comment lines and removed or reordered import lines. Revert anything else and mention it.
2. Compile with `mvn clean package -q -DskipTests`. If `mvn` is not on the PATH, try `~/.m2/wrapper/dists/apache-maven-*/*/bin/mvn`; if you still can't build, say so instead of claiming it compiled.

## 5. Final report

At most a few lines: files edited, total commented-out blocks and unused imports removed, doubtful comments kept (file + line), bugs noticed but not touched, and the compile result.

Write everything in the user's language.
