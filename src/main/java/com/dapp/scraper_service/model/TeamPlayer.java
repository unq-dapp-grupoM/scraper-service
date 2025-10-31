package com.dapp.scraper_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "team_players")
@Data
@NoArgsConstructor
@Getter
@Setter
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

    // Many squad players belong to a single team.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false) // Clave foránea a la tabla teams
    @JsonIgnore // Avoid infinite loops when converting to JSON
    private Team team;

}
