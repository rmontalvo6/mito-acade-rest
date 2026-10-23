package com.mitocode.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatriculaDTO {

    private Integer id;

    @NotNull(message = "La fecha de matrícula es obligatoria")
    private LocalDateTime fechaMatricula;

    @NotNull(message = "Los datos del estudiante son obligatorios")
    @Valid
    private EstudianteRefDTO estudiante;

    @NotNull(message = "El estado de la matrícula es obligatorio")
    private Boolean estado;

    @NotEmpty(message = "La matrícula debe contener al menos un detalle de curso")
    @Valid
    private List<DetalleMatriculaDTO> detalleMatricula;
}
