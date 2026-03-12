package org.example.greenloginbe.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Integer id;
    private String username;
    private String displayName;
    private List<String> roles;

    public JwtResponse(String accessToken, Integer id, String username, String displayName, List<String> roles) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.roles = roles;
    }
}
