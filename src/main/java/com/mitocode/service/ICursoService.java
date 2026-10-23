package com.mitocode.service;

import com.mitocode.model.Curso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICursoService extends ICRUD<Curso, Integer> {

    Page<Curso> listarPagina(Pageable pageable) throws Exception;
}
