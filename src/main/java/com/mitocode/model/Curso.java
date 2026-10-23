package com.mitocode.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
// Solo el id entra en equals/hashCode: los demás campos son mutables y romperían
// la identidad del objeto dentro de un HashSet o un Map
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "curso", schema = "academia",
        uniqueConstraints = @UniqueConstraint(name = "uk_curso_siglas", columnNames = "siglas"))
public class Curso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    // La unicidad real la da la restricción uk_curso_siglas declarada en @Table (ddl-auto la
    // crea al arrancar). No hace falta un índice funcional: el servicio normaliza las siglas a
    // mayúsculas antes de guardar, así que un índice normal sobre la columna ya cubre los
    // choques de capitalización y además es el que usa existsBySiglas
    @Column(nullable = false, length = 10)
    private String siglas;

    @Column(nullable = false)
    private Boolean estado;
}
