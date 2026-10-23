package com.mitocode.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CursoRefDTO {

    @NotNull(message = "El ID del curso es obligatorio")
    private Integer id;
}
