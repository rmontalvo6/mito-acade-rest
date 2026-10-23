package com.mitocode.repo;

import com.mitocode.model.Matricula;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IMatriculaRepo extends IGenericRepo<Matricula, Integer> {

    @Query("""
            SELECT DISTINCT m FROM Matricula m
            LEFT JOIN FETCH m.estudiante e
            LEFT JOIN FETCH m.detalleMatricula d
            LEFT JOIN FETCH d.curso c
            """)
    List<Matricula> obtenerMatriculasConRelaciones();

    // El WHERE actúa sobre el JOIN FETCH, así que cada matrícula llega con su detalle YA filtrado
    // a cursos activos: es justo lo que necesita el reporte y por eso el Stream solo da forma.
    // Por lo mismo, esta consulta es solo de lectura: la colección viene parcialmente cargada a
    // propósito y no debe usarse en un camino que escriba (borraría el detalle no traído).
    @Query("""
            SELECT DISTINCT m FROM Matricula m
            JOIN FETCH m.estudiante e
            JOIN FETCH m.detalleMatricula d
            JOIN FETCH d.curso c
            WHERE c.estado = true
            """)
    List<Matricula> obtenerMatriculasConCursosActivos();

    @Query("SELECT COUNT(m) > 0 FROM Matricula m JOIN m.detalleMatricula d WHERE m.estudiante.id = :idEstudiante AND d.curso.id = :idCurso")
    boolean existsByEstudianteAndCurso(@Param("idEstudiante") Integer idEstudiante, @Param("idCurso") Integer idCurso);

    @Query("""
            SELECT COUNT(m) > 0 FROM Matricula m
            JOIN m.detalleMatricula d
            WHERE m.estudiante.id = :idEstudiante
              AND d.curso.id = :idCurso
              AND m.id <> :idMatricula
            """)
    boolean existsByEstudianteAndCursoEnOtraMatricula(@Param("idEstudiante") Integer idEstudiante,
                                                      @Param("idCurso") Integer idCurso,
                                                      @Param("idMatricula") Integer idMatricula);

    boolean existsByEstudianteId(Integer idEstudiante);

    @Query("SELECT COUNT(d) > 0 FROM DetalleMatricula d WHERE d.curso.id = :idCurso")
    boolean existsDetalleByCursoId(@Param("idCurso") Integer idCurso);
}
