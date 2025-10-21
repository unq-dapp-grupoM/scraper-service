package com.dapp.scraper_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String homeTeam;
    private String awayTeam;
    private String date;
    private String competition;

    // Muchos partidos pueden estar asociados a un equipo.
    // Usamos FetchType.LAZY para que el equipo no se cargue a menos que se necesite.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") // Esta será la clave foránea en la tabla 'matches'
    private Team team;

}