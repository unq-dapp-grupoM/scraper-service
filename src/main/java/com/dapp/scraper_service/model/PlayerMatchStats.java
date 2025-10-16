package com.dapp.scraper_service.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "player_match_stats")
@Data
@NoArgsConstructor
public class PlayerMatchStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String opponent;
    private String score;
    private String date;
    private String position;
    private String minsPlayed;
    private String goals;
    private String assists;
    private String yellowCards;
    private String redCards;
    private String shots;
    private String passSuccess;
    private String aerialsWon;
    private String rating;

    // Muchas estadísticas de partido pertenecen a un solo jugador.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false) // Esta será la columna de clave foránea
    @JsonIgnore // Evita problemas de serialización infinita
    private Player player;

}