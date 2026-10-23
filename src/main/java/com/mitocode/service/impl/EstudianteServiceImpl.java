package com.mitocode.service.impl;

import com.mitocode.exception.DuplicateException;
import com.mitocode.model.Estudiante;
import com.mitocode.repo.IEstudianteRepo;
import com.mitocode.repo.IGenericRepo;
import com.mitocode.repo.IMatriculaRepo;
import com.mitocode.service.IEstudianteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstudianteServiceImpl extends CRUDImpl<Estudiante, Integer> implements IEstudianteService {

    private final IEstudianteRepo repoEstudiante;
    private final IMatriculaRepo repoMatricula;

    @Override
    protected IGenericRepo<Estudiante, Integer> getRepo() {
        return repoEstudiante;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Estudiante> listarEstudiantesOrdenadosPorEdadDesc() throws Exception {
        return repoEstudiante.findAll().stream()
                .sorted(Comparator.comparing(Estudiante::getEdad).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Estudiante registrar(Estudiante estudiante) throws Exception {
        estudiante.setId(null);
        if (repoEstudiante.existsByDni(estudiante.getDni())) {
            throw new DuplicateException("El DNI " + estudiante.getDni() + " ya se encuentra registrado para otro estudiante.");
        }

        return repoEstudiante.save(estudiante);
    }

    @Override
    @Transactional
    public Estudiante modificar(Integer id, Estudiante estudiante) throws Exception {
        Estudiante estudianteDb = leerPorId(id);
        // El id de la ruta manda sobre el del cuerpo: sin esto, un cuerpo sin id insertaría un estudiante nuevo
        estudiante.setId(estudianteDb.getId());

        if (!estudianteDb.getDni().equalsIgnoreCase(estudiante.getDni())) {
            if (repoEstudiante.existsByDni(estudiante.getDni())) {
                throw new DuplicateException("El DNI " + estudiante.getDni() + " ya se encuentra registrado para otro estudiante.");
            }
        }

        return repoEstudiante.save(estudiante);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) throws Exception {
        Estudiante estudiante = leerPorId(id);

        if (repoMatricula.existsByEstudianteId(id)) {
            throw new DuplicateException("No se puede eliminar el estudiante con DNI: " + estudiante.getDni()
                    + " porque cuenta con matrículas registradas.");
        }

        repoEstudiante.deleteById(id);
    }
}
