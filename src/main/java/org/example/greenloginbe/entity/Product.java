package org.example.greenloginbe.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "products", schema = "db_green")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 50)
    @NotNull
    @Column(name = "sku", nullable = false, length = 50, unique = true)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "category_id")
    private Category category;

    @Size(max = 150)
    @NotNull
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Size(max = 50)
    @NotNull
    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    @NotNull
    @ColumnDefault("0.00")
    @Column(name = "default_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal defaultPrice;

    @NotNull
    @ColumnDefault("0.00")
    @Column(name = "stock_quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal stockQuantity;

    @NotNull
    @ColumnDefault("0.00")
    @Column(name = "min_stock_level", nullable = false, precision = 15, scale = 2)
    private BigDecimal minStockLevel;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Boolean isActive;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;


}