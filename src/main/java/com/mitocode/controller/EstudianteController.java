package com.mitocode.controller;

import com.mitocode.dto.EstudianteDTO;
import com.mitocode.model.Estudiante;
import com.mitocode.service.IEstudianteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/estudiante")
@RequiredArgsConstructor
public class EstudianteController {

    private final IEstudianteService service;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<EstudianteDTO>> listar() throws Exception {
        List<EstudianteDTO> lista = service.listar().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return new ResponseEntity<>(lista, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstudianteDTO> leerPorId(@PathVariable("id") Integer id) throws Exception {
        Estudiante obj = service.leerPorId(id);
        return new ResponseEntity<>(convertToDto(obj), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<EstudianteDTO> registrar(@Valid @RequestBody EstudianteDTO dto) throws Exception {
        Estudiante obj = service.registrar(convertToEntity(dto));
        return new ResponseEntity<>(convertToDto(obj), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstudianteDTO> modificar(@PathVariable Integer id, @Valid @RequestBody EstudianteDTO dto) throws Exception {
        Estudiante obj = service.modificar(id, convertToEntity(dto));
        return new ResponseEntity<>(convertToDto(obj), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Integer id) throws Exception {
        service.eliminar(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/ordenados-edad")
    public ResponseEntity<List<EstudianteDTO>> listarOrdenadosPorEdadDesc() throws Exception {
        List<EstudianteDTO> lista = service.listarEstudiantesOrdenadosPorEdadDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return new ResponseEntity<>(lista, HttpStatus.OK);
    }

    private EstudianteDTO convertToDto(Estudiante estudiante) {
        return modelMapper.map(estudiante, EstudianteDTO.class);
    }

    private Estudiante convertToEntity(EstudianteDTO dto) {
        return modelMapper.map(dto, Estudiante.class);
    }
}
