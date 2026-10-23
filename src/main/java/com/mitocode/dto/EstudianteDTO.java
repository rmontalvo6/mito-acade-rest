package com.mitocode.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EstudianteDTO {

    private Integer id;

    @NotEmpty(message = "Los nombres no pueden estar vacíos")
    @Size(min = 2, max = 70, message = "Los nombres deben tener entre 2 y 70 caracteres")
    private String nombres;

    @NotEmpty(message = "Los apellidos no pueden estar vacíos")
    @Size(min = 2, max = 70, message = "Los apellidos deben tener entre 2 y 70 caracteres")
    private String apellidos;

    @NotEmpty(message = "El DNI no puede estar vacío")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos")
    private String dni;

    @NotNull(message = "La edad es obligatoria")
    @Min(value = 1, message = "La edad mínima permitida es 1")
    private Integer edad;
}
