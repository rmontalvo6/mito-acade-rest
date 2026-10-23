package com.mitocode.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EstudianteRefDTO {

    @NotNull(message = "El ID del estudiante es obligatorio")
    private Integer id;
}
