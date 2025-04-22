package dev.idachev.recipeservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Macros {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(precision = 10, scale = 2)
    private BigDecimal calories;

    @Column(precision = 10, scale = 2)
    private BigDecimal proteinGrams;

    @Column(precision = 10, scale = 2)
    private BigDecimal carbsGrams;

    @Column(precision = 10, scale = 2)
    private BigDecimal fatGrams;
} 