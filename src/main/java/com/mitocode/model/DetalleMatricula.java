package com.mitocode.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "detalle_matricula", schema = "academia",
        uniqueConstraints = @UniqueConstraint(name = "UK_detalle_matricula_curso",
                columnNames = {"id_matricula", "id_curso"}))
public class DetalleMatricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_matricula", nullable = false, foreignKey = @ForeignKey(name = "FK_detalle_matricula"))
    private Matricula matricula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso", nullable = false, foreignKey = @ForeignKey(name = "FK_detalle_curso"))
    private Curso curso;

    @Column(nullable = false, length = 50)
    private String aula;
}
