package com.mitocode.repo;

import com.mitocode.model.Curso;

public interface ICursoRepo extends IGenericRepo<Curso, Integer> {
    // Sin IgnoreCase a propósito: el servicio normaliza las siglas a mayúsculas antes de
    // consultar, así que la igualdad simple basta y la consulta sí puede aprovechar el índice
    // uk_curso_siglas (un upper(siglas) = upper(?) obligaría a un seq scan)
    boolean existsBySiglas(String siglas);

    // Variante para el update: excluye la propia fila, así un PUT que no cambia las siglas
    // no se encuentra a sí mismo y no produce un 409 falso
    boolean existsBySiglasAndIdNot(String siglas, Integer id);
}
