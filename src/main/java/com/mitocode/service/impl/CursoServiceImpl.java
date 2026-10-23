package com.mitocode.service.impl;

import com.mitocode.exception.DuplicateException;
import com.mitocode.model.Curso;
import com.mitocode.repo.ICursoRepo;
import com.mitocode.repo.IGenericRepo;
import com.mitocode.repo.IMatriculaRepo;
import com.mitocode.service.ICursoService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CursoServiceImpl extends CRUDImpl<Curso, Integer> implements ICursoService {

    // Espacios no separables: NBSP, figure space y narrow NBSP. Character.isWhitespace() los
    // excluye a propósito, así que hay que convertirlos antes de recortar
    private static final Pattern ESPACIOS_NO_SEPARABLES = Pattern.compile("[\\u00A0\\u2007\\u202F]");

    // SQLState de PostgreSQL. Discriminar por aquí es más estable que por el nombre de la
    // restricción: ddl-auto bautiza las claves foráneas con un hash que puede cambiar
    private static final String SQLSTATE_UNIQUE_VIOLATION = "23505";
    private static final String SQLSTATE_FOREIGN_KEY_VIOLATION = "23503";

    private final ICursoRepo repo;
    private final IMatriculaRepo repoMatricula;

    @Override
    protected IGenericRepo<Curso, Integer> getRepo() {
        return repo;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Curso> listarPagina(Pageable pageable) throws Exception {
        return repo.findAll(pageable);
    }

    @Override
    @Transactional
    public Curso registrar(Curso curso) throws Exception {
        curso.setId(null);
        normalizar(curso);

        if (repo.existsBySiglas(curso.getSiglas())) {
            throw new DuplicateException("El curso con siglas '" + curso.getSiglas() + "' ya se encuentra registrado.");
        }

        try {
            Curso guardado = repo.save(curso);
            // flush dentro del try: el exists de arriba no es atómico, así que si otra petición
            // insertó las mismas siglas en medio, la restricción uk_curso_siglas salta aquí y la
            // traducimos al mismo 409 en lugar de dejar el mensaje genérico de integridad
            repo.flush();
            return guardado;
        } catch (DataIntegrityViolationException ex) {
            if (esViolacionDe(ex, SQLSTATE_UNIQUE_VIOLATION)) {
                throw new DuplicateException("El curso con siglas '" + curso.getSiglas()
                        + "' ya se encuentra registrado.");
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public Curso modificar(Integer id, Curso curso) throws Exception {
        Curso cursoDb = leerPorId(id);
        normalizar(curso);

        if (repo.existsBySiglasAndIdNot(curso.getSiglas(), id)) {
            throw new DuplicateException("Las siglas '" + curso.getSiglas() + "' ya están asignadas a otro curso.");
        }

        // Se copian los campos sobre la instancia ya gestionada en lugar de guardar la que
        // llegó del DTO: así el id de la ruta sigue siendo el único que manda.
        // Nota: sin @Version, dos PUT concurrentes sobre el mismo curso se pisan (el último gana).
        cursoDb.setNombre(curso.getNombre());
        cursoDb.setSiglas(curso.getSiglas());
        cursoDb.setEstado(curso.getEstado());

        try {
            Curso guardado = repo.save(cursoDb);
            // flush dentro del try, igual que en registrar: adelanta el UPDATE para traducir una
            // violación de uk_curso_siglas al 409 específico. Sin esto estallaría al hacer commit,
            // ya fuera del método, y el cliente recibiría el 409 genérico del GlobalErrorHandler
            repo.flush();
            return guardado;
        } catch (DataIntegrityViolationException ex) {
            if (esViolacionDe(ex, SQLSTATE_UNIQUE_VIOLATION)) {
                throw new DuplicateException("Las siglas '" + curso.getSiglas()
                        + "' ya están asignadas a otro curso.");
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public void eliminar(Integer id) throws Exception {
        Curso curso = leerPorId(id);

        if (repoMatricula.existsDetalleByCursoId(id)) {
            throw new DuplicateException("No se puede eliminar el curso con siglas '" + curso.getSiglas()
                    + "' porque está siendo usado en matrículas registradas.");
        }

        try {
            repo.delete(curso);
            // flush dentro del try: fuerza el DELETE ahora para poder traducir una violación de FK
            // creada entre el exists y el borrado, en lugar de que salga como error genérico
            repo.flush();
        } catch (DataIntegrityViolationException ex) {
            if (esViolacionDe(ex, SQLSTATE_FOREIGN_KEY_VIOLATION)) {
                throw new DuplicateException("No se puede eliminar el curso con siglas '" + curso.getSiglas()
                        + "' porque está siendo usado en matrículas registradas.");
            }
            throw ex;
        }
    }

    /**
     * Comprueba si la violación de integridad es del tipo que el llamador acaba de descartar con
     * un exists. Solo entonces se traduce al 409 específico: disfrazar de duplicado un not-null o
     * una FK ajena daría un mensaje falso al cliente. Lo demás se re-lanza para que lo trate el
     * GlobalErrorHandler.
     */
    private boolean esViolacionDe(DataIntegrityViolationException ex, String sqlState) {
        return ex.getMostSpecificCause() instanceof SQLException causa
                && sqlState.equals(causa.getSQLState());
    }

    /**
     * Recorta espacios y pasa las siglas a mayúsculas, para que el chequeo de unicidad no se
     * pueda burlar con " MAT101" ni con "mat101". Los espacios no separables se sustituyen por
     * un espacio normal ANTES del strip(), porque Character.isWhitespace() -en el que se apoyan
     * tanto strip() como isBlank()- los excluye a propósito: sin esta conversión, unas siglas
     * con un NBSP delante no las recortaba nadie y convivían con su versión limpia sin que
     * existsBySiglas ni la restriccion uk_curso_siglas vieran el duplicado.
     */
    private void normalizar(Curso curso) {
        curso.setNombre(normalizarTexto(curso.getNombre()));

        String siglas = normalizarTexto(curso.getSiglas());
        curso.setSiglas(siglas != null ? siglas.toUpperCase(Locale.ROOT) : null);

        validar(curso);
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        return ESPACIOS_NO_SEPARABLES.matcher(valor).replaceAll(" ").strip();
    }

    /**
     * Red de seguridad para las llamadas que no pasan por el @Valid del controller, donde las
     * restricciones del DTO no intervienen. Un texto hecho solo de espacios no separables queda
     * vacío al normalizar y la columna lo aceptaría, porque es not-null y no not-empty; un estado
     * nulo sí violaría el not-null, pero llegaría al flush y saldría como un 409 de integridad en
     * lugar del 400 que corresponde a un dato de entrada inválido. IllegalArgumentException los
     * traduce a 400 en el GlobalErrorHandler.
     */
    private void validar(Curso curso) {
        if (curso.getNombre() == null || curso.getNombre().isEmpty()) {
            throw new IllegalArgumentException("El nombre del curso no puede estar vacío.");
        }
        if (curso.getSiglas() == null || curso.getSiglas().isEmpty()) {
            throw new IllegalArgumentException("Las siglas del curso no pueden estar vacías.");
        }
        if (curso.getEstado() == null) {
            throw new IllegalArgumentException("El estado del curso es obligatorio.");
        }
    }
}
