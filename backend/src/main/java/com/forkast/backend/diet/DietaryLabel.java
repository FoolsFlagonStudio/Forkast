package com.forkast.backend.diet;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name = "dietary_label")
public class DietaryLabel {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column (name = "name", nullable=false, unique=true)
    private String name;

    protected DietaryLabel() {}

    public DietaryLabel(String name) {
        this.name = name;
    }   

    public UUID getId() {return id;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
}
