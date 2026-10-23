package com.mitocode.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "matricula", schema = "academia")
public class Matricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private LocalDateTime fechaMatricula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false, foreignKey = @ForeignKey(name = "FK_matricula_estudiante"))
    private Estudiante estudiante;

    @Column(nullable = false)
    private Boolean estado;

    @OneToMany(mappedBy = "matricula", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleMatricula> detalleMatricula;

    public void setDetalleMatricula(List<DetalleMatricula> detalleMatricula) {
        this.detalleMatricula = detalleMatricula;
        if (detalleMatricula != null) {
            for (DetalleMatricula det : detalleMatricula) {
                det.setMatricula(this);
            }
        }
    }
}
