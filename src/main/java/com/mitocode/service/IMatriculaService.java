package com.mitocode.service;

import com.mitocode.model.Matricula;

import java.util.List;
import java.util.Map;

public interface IMatriculaService extends ICRUD<Matricula, Integer> {
    Map<String, List<String>> obtenerCursosConEstudiantes() throws Exception;

    Map<String, List<String>> obtenerEstudiantesConCursosActivos() throws Exception;
}
