package com.mitocode.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "estudiante", schema = "academia")
public class Estudiante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 70)
    private String nombres;

    @Column(nullable = false, length = 70)
    private String apellidos;

    @Column(nullable = false, length = 8, unique = true)
    private String dni;

    @Column(nullable = false)
    private Integer edad;
}
