package com.mitocode.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
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
public class CursoDTO {

    // Se devuelve en las respuestas pero se ignora en el cuerpo de entrada:
    // el id de la ruta es el único que manda en el update
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer id;

    // El @Pattern rechaza los espacios en los extremos en lugar de dejar que el trim del
    // servicio los recorte después de validar, lo que permitía colar un " A " de 3 caracteres
    // que acababa guardado con 1 y burlaba el min.
    // El flag (?U) es imprescindible: sin él, \s es solo [ \t\n\x0B\f\r] y los espacios no
    // separables (U+00A0, U+2007, U+202F) cuentan como \S, así que un nombre formado solo por
    // ellos pasaba el @NotBlank y el @Size y se guardaba en blanco. Con (?U), \s equivale a
    // \p{IsWhite_Space}, que sí los incluye
    @NotBlank(message = "El nombre del curso no puede estar vacío")
    @Size(min = 2, max = 100, message = "El nombre del curso debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "(?U)^\\S(.*\\S)?$", message = "El nombre del curso no puede empezar ni terminar con espacios")
    private String nombre;

    @NotBlank(message = "Las siglas no pueden estar vacías")
    @Size(min = 2, max = 10, message = "Las siglas deben tener entre 2 y 10 caracteres")
    @Pattern(regexp = "(?U)^\\S(.*\\S)?$", message = "Las siglas no pueden empezar ni terminar con espacios")
    private String siglas;

    @NotNull(message = "El estado del curso es obligatorio")
    private Boolean estado;
}
