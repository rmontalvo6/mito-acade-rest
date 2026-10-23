package com.mitocode.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetalleMatriculaDTO {

    @NotNull(message = "El curso en el detalle de matrícula es obligatorio")
    @Valid
    private CursoRefDTO curso;

    @NotEmpty(message = "El aula no puede estar vacía")
    @Size(max = 50, message = "El aula no puede exceder los 50 caracteres")
    private String aula;
}
