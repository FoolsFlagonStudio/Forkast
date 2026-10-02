package com.forkast.backend.diet;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name = "dietary_labels")
public class DietaryLabel {

    // ---------- id ----------

    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---------- columns ----------

    @Column (name = "name", nullable=false, unique=true)
    private String name;

    // ---------- constructors ----------

    protected DietaryLabel() {}

    public DietaryLabel(String name) {
        this.name = name;
    }   

    // ---------- getters / setters ----------

    public UUID getId() {return id;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
}
