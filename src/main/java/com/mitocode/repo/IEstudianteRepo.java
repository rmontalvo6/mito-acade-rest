package com.mitocode.repo;

import com.mitocode.model.Estudiante;

public interface IEstudianteRepo extends IGenericRepo<Estudiante, Integer> {
    boolean existsByDni(String dni);
}
