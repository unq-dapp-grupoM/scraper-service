package com.dapp.scraper_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "players") // Es una buena práctica nombrar las tablas en plural
@Data
@NoArgsConstructor
@Getter
@Setter
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

    // A player can have multiple match stats.
    // cascade = CascadeType.ALL: If we save/delete a player, their stats are also
    // saved/delete.
    // orphanRemoval = true: If we remove a stat from the list, it is removed from
    // the database.
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlayerMatchStats> matchStats = new ArrayList<>();

}