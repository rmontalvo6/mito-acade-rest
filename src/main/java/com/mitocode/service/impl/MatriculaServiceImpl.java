package com.mitocode.service.impl;

import com.mitocode.exception.DuplicateException;
import com.mitocode.exception.ModelNotFoundException;
import com.mitocode.model.Curso;
import com.mitocode.model.DetalleMatricula;
import com.mitocode.model.Estudiante;
import com.mitocode.model.Matricula;
import com.mitocode.repo.ICursoRepo;
import com.mitocode.repo.IEstudianteRepo;
import com.mitocode.repo.IGenericRepo;
import com.mitocode.repo.IMatriculaRepo;
import com.mitocode.service.IMatriculaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatriculaServiceImpl extends CRUDImpl<Matricula, Integer> implements IMatriculaService {

    private final IMatriculaRepo repo;
    private final IEstudianteRepo repoEstudiante;
    private final ICursoRepo repoCurso;

    @Override
    protected IGenericRepo<Matricula, Integer> getRepo() {
        return repo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Matricula> listar() throws Exception {
        // Consulta con JOIN FETCH: evita una consulta por matrícula al mapear las relaciones
        return repo.obtenerMatriculasConRelaciones();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> obtenerCursosConEstudiantes() throws Exception {
        List<Matricula> matriculas = repo.obtenerMatriculasConRelaciones();

        return matriculas.stream()
                .flatMap(m -> m.getDetalleMatricula().stream()
                        .map(det -> Map.entry(
                                det.getCurso().getNombre(),
                                m.getEstudiante().getNombres() + " " + m.getEstudiante().getApellidos()
                        ))
                ).distinct()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> obtenerEstudiantesConCursosActivos() throws Exception {
        // Espejo de obtenerCursosConEstudiantes, con la clave y el valor invertidos: la consulta
        // ya filtró los cursos activos, aquí solo se agrupa estudiante -> sus cursos
        List<Matricula> matriculas = repo.obtenerMatriculasConCursosActivos();

        return matriculas.stream()
                .flatMap(m -> m.getDetalleMatricula().stream()
                        .map(det -> Map.entry(
                                m.getEstudiante().getNombres() + " " + m.getEstudiante().getApellidos(),
                                det.getCurso().getNombre()
                        ))
                ).distinct()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    @Override
    @Transactional
    public Matricula registrar(Matricula matricula) throws Exception {
        matricula.setId(null);

        Integer idEstudiante = obtenerIdEstudiante(matricula);
        if (!repoEstudiante.existsById(idEstudiante)) {
            throw new ModelNotFoundException("Estudiante no encontrado con ID: " + idEstudiante);
        }

        Set<Integer> idsCursos = obtenerIdsCursosSinRepetir(matricula.getDetalleMatricula());
        obtenerCursosExistentes(idsCursos);

        for (Integer idCurso : idsCursos) {
            if (repo.existsByEstudianteAndCurso(idEstudiante, idCurso)) {
                throw new DuplicateException("El estudiante ya se encuentra matriculado en el curso con ID: " + idCurso);
            }
        }

        // Asignamos la relación bidireccional
        for (DetalleMatricula det : matricula.getDetalleMatricula()) {
            det.setMatricula(matricula);
        }

        return repo.save(matricula);
    }

    @Override
    @Transactional
    public Matricula modificar(Integer id, Matricula matricula) throws Exception {
        // El id manda es el de la ruta: se carga la matrícula gestionada (404 si no existe)
        Matricula matriculaDb = leerPorId(id);

        Integer idEstudiante = obtenerIdEstudiante(matricula);
        Estudiante estudiante = repoEstudiante.findById(idEstudiante)
                .orElseThrow(() -> new ModelNotFoundException("Estudiante no encontrado con ID: " + idEstudiante));

        Set<Integer> idsCursos = obtenerIdsCursosSinRepetir(matricula.getDetalleMatricula());
        Map<Integer, Curso> cursos = obtenerCursosExistentes(idsCursos);

        // Las mismas reglas que al registrar,
        for (Integer idCurso : idsCursos) {
            if (repo.existsByEstudianteAndCursoEnOtraMatricula(idEstudiante, idCurso, id)) {
                throw new DuplicateException("El estudiante ya se encuentra matriculado en el curso con ID: "
                        + idCurso + " en otra  matrícula.");
            }
        }

        matriculaDb.setFechaMatricula(matricula.getFechaMatricula());
        matriculaDb.setEstado(matricula.getEstado());
        matriculaDb.setEstudiante(estudiante);

        // Se vacía la colección existente en le asigna otra lista,
        // Hibernate pierde la referencia que necesita para el orphanRemoval y falla.
        List<DetalleMatricula> detalles = matriculaDb.getDetalleMatricula();
        detalles.clear();
        // flush intermedio: fuerza los DELETEotro modo
        // chocarían con UK_detalle_matricula_curso al repetirse un curso que ya estaba.
        repo.flush();

        for (DetalleMatricula det : matricula.getDetalleMatricula()) {
            DetalleMatricula nuevo = new DetalleMatricula();
            nuevo.setMatricula(matriculaDb);
            nuevo.setCurso(cursos.get(det.getCurso().getId()));
            nuevo.setAula(det.getAula());
            detalles.add(nuevo);
        }

        return repo.save(matriculaDb);
    }

    private Integer obtenerIdEstudiante(Matricula matricula) {
        Integer idEstudiante = matricula.getEstudiante() == null ? null : matricula.getEstudiante().getId();
        if (idEstudiante == null) {
            throw new IllegalArgumentException("El ID del estudiante es obligatorio en la matrícula.");
        }
        return idEstudiante;
    }

    /** Recoge los ids de curso del detalle y rechaza el mismo curso repetido dentro de la misma matrícula. */
    private Set<Integer> obtenerIdsCursosSinRepetir(List<DetalleMatricula> detalles) {
        Set<Integer> idsCursos = new LinkedHashSet<>();

        for (DetalleMatricula det : detalles) {
            Integer idCurso = det.getCurso() == null ? null : det.getCurso().getId();
            if (idCurso == null) {
                throw new IllegalArgumentException("El ID del curso es obligatorio en cada detalle de la matrícula.");
            }
            if (!idsCursos.add(idCurso)) {
                throw new DuplicateException("El curso con ID: " + idCurso + " está repetido en la misma matrícula.");
            }
        }

        return idsCursos;
    }

    /** Una sola consulta para todos los cursos, en lugar de un existsById por detalle. */
    private Map<Integer, Curso> obtenerCursosExistentes(Set<Integer> idsCursos) {
        Map<Integer, Curso> encontrados = repoCurso.findAllById(idsCursos).stream()
                .collect(Collectors.toMap(Curso::getId, curso -> curso));

        for (Integer idCurso : idsCursos) {
            if (!encontrados.containsKey(idCurso)) {
                throw new ModelNotFoundException("Curso no encontrado con ID: " + idCurso);
            }
        }

        return encontrados;
    }
}
