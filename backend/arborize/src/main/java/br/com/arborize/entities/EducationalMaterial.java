package br.com.arborize.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "educational_material")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationalMaterial {

}
