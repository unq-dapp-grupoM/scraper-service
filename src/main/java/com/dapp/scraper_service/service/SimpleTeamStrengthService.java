package com.dapp.scraper_service.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SimpleTeamStrengthService {

    private final Map<String, Double> teamFactors = new HashMap<>();

    public SimpleTeamStrengthService() {
        // Inicializar con equipos conocidos
        initializeTeamFactors();
    }

    private void initializeTeamFactors() {
        // Equipos FUERTES (factor < 1.0)
        teamFactors.put("real madrid", 0.6);
        teamFactors.put("barcelona", 0.6);
        teamFactors.put("manchester city", 0.6);
        teamFactors.put("bayern munich", 0.6);
        teamFactors.put("psg", 0.7);
        teamFactors.put("juventus", 0.7);
        teamFactors.put("boca juniors", 0.7);
        teamFactors.put("river plate", 0.7);

        // Equipos DÉBILES (factor > 1.0)
        teamFactors.put("aldosivi", 1.3);
        teamFactors.put("patronato", 1.3);
        teamFactors.put("sarmiento", 1.2);
        teamFactors.put("norwich", 1.3);
        teamFactors.put("getafe", 1.2);
    }

    public double getOpponentFactor(String opponent) {
        if (opponent == null)
            return 1.0;

        String key = opponent.toLowerCase();

        // Buscar coincidencia exacta
        if (teamFactors.containsKey(key)) {
            return teamFactors.get(key);
        }

        // Buscar por contenido
        for (String team : teamFactors.keySet()) {
            if (key.contains(team) || team.contains(key)) {
                return teamFactors.get(team);
            }
        }

        return 1.0; // Neutral si no se encuentra
    }
}