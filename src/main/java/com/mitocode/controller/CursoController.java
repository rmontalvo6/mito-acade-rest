package com.mitocode.controller;

import com.mitocode.dto.CursoDTO;
import com.mitocode.model.Curso;
import com.mitocode.service.ICursoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/curso")
@RequiredArgsConstructor
public class CursoController {

    private final ICursoService service;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<CursoDTO>> listar() throws Exception {
        List<CursoDTO> lista = service.listar().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return new ResponseEntity<>(lista, HttpStatus.OK);
    }

    // Alternativa paginada al listado completo, preferible cuando la tabla crece: el GET de
    // arriba materializa todos los cursos dos veces, como entidades y como DTOs.
    // ?page=0&size=20&sort=nombre,asc
    @GetMapping("/pageable")
    public ResponseEntity<Page<CursoDTO>> listarPaginado(Pageable pageable) throws Exception {
        Page<CursoDTO> pagina = service.listarPagina(pageable).map(this::convertToDto);
        return new ResponseEntity<>(pagina, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CursoDTO> listarPorId(@PathVariable("id") Integer id) throws Exception {
        Curso curso = service.leerPorId(id);
        return new ResponseEntity<>(convertToDto(curso), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<CursoDTO> registrar(@Valid @RequestBody CursoDTO dto) throws Exception {
        Curso curso = service.registrar(convertToEntity(dto));

        // Location con la URI canónica del recurso creado: /api/v1/curso/{id}
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(curso.getId())
                .toUri();

        return ResponseEntity.created(location).body(convertToDto(curso));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CursoDTO> modificar(@PathVariable Integer id, @Valid @RequestBody CursoDTO dto) throws Exception {
        Curso curso = service.modificar(id, convertToEntity(dto));
        return new ResponseEntity<>(convertToDto(curso), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Integer id) throws Exception {
        service.eliminar(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private Curso convertToEntity(CursoDTO dto) {
        return modelMapper.map(dto, Curso.class);
    }

    private CursoDTO convertToDto(Curso curso) {
        return modelMapper.map(curso, CursoDTO.class);
    }
}
