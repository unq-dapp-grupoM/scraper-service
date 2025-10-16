package com.dapp.scraper_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "team_players")
@Data
@NoArgsConstructor
public class TeamPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String age;
    private String position;
    private String height;
    private String weight;
    private String apps;
    private String minsPlayed;
    private String goals;
    private String assists;
    private String yellowCards;
    private String redCards;
    private String shotsPerGame;
    private String passSuccess;
    private String aerialsWonPerGame;
    private String manOfTheMatch;
    private String rating;

    // Muchos jugadores de plantilla pertenecen a un solo equipo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false) // Clave foránea a la tabla teams
    @JsonIgnore // Evita bucles infinitos al convertir a JSON
    private Team team;

}
