package com.dapp.scraper_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "players") // Es una buena práctica nombrar las tablas en plural
@Data
@NoArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String currentTeam;
    private String shirtNumber;
    private String age;
    private String height;
    private String nationality;
    private String positions;

    // Un jugador puede tener muchas estadísticas de partidos.
    // cascade = CascadeType.ALL: Si guardamos/eliminamos un jugador, también se
    // guardan/eliminan sus estadísticas.
    // orphanRemoval = true: Si quitamos una estadística de la lista, se elimina de
    // la BD.
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlayerMatchStats> matchStats = new ArrayList<>();

}