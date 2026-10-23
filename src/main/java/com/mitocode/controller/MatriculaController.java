package com.mitocode.controller;

import com.mitocode.dto.MatriculaDTO;
import com.mitocode.model.Matricula;
import com.mitocode.service.IMatriculaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/matricula")
@RequiredArgsConstructor
public class MatriculaController {

    private final IMatriculaService service;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<MatriculaDTO> registrar(@Valid @RequestBody MatriculaDTO dto) throws Exception {
        Matricula matricula = service.registrar(convertToEntity(dto));
        return new ResponseEntity<>(convertToDto(matricula), HttpStatus.CREATED);
    }

    @GetMapping("/cursos-estudiantes")
    public ResponseEntity<Map<String, List<String>>> listarCursosConEstudiantes() throws Exception {
        Map<String, List<String>> resultado = service.obtenerCursosConEstudiantes();
        return new ResponseEntity<>(resultado, HttpStatus.OK);
    }

    @GetMapping("/estudiantes-cursos-activos")
    public ResponseEntity<Map<String, List<String>>> listarEstudiantesConCursosActivos() throws Exception {
        Map<String, List<String>> resultado = service.obtenerEstudiantesConCursosActivos();
        return new ResponseEntity<>(resultado, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<MatriculaDTO>> listar() throws Exception {
        List<MatriculaDTO> lista = service.listar().stream().map(this::convertToDto).collect(Collectors.toList());
        return new ResponseEntity<>(lista, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Integer id) throws Exception {
        service.eliminar(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatriculaDTO> modificar(@PathVariable Integer id, @Valid @RequestBody MatriculaDTO dto) throws Exception {
        Matricula obj = service.modificar(id, convertToEntity(dto));
        return new ResponseEntity<>(convertToDto(obj), HttpStatus.OK);
    }

    private MatriculaDTO convertToDto(Matricula matricula) {
        return modelMapper.map(matricula, MatriculaDTO.class);
    }

    private Matricula convertToEntity(MatriculaDTO dto) {
        return modelMapper.map(dto, Matricula.class);
    }
}
