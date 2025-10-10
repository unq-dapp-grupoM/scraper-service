package com.dapp.scraper_service.model.dto;

import java.util.List;
import lombok.Data;

@Data
public class TeamDTO {
    private String name;
    private List<TeamPlayerDTO> squad;
}
