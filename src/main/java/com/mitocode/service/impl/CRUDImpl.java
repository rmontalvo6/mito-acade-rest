package com.mitocode.service.impl;

import com.mitocode.exception.ModelNotFoundException;
import com.mitocode.repo.IGenericRepo;
import com.mitocode.service.ICRUD;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public abstract class CRUDImpl<T,ID> implements ICRUD<T,ID> {

    protected abstract IGenericRepo<T, ID> getRepo();

    @Override
    @Transactional
    public T registrar(T t) throws Exception {
        return getRepo().save(t);
    }

    @Override
    @Transactional
    public T modificar(ID id,T t) throws Exception {
        leerPorId(id);
        return getRepo().save(t);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> listar() throws Exception {
        return getRepo().findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public T leerPorId(ID id) throws Exception {
        return getRepo().findById(id).orElseThrow(() -> new ModelNotFoundException("ID no encontrado: " + id));
    }

    @Override
    @Transactional
    public void eliminar(ID id) throws Exception {
        T entidad = getRepo().findById(id)
                .orElseThrow(() -> new ModelNotFoundException("ID no encontrado para eliminar: " + id));
        getRepo().delete(entidad);
    }


}