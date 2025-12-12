package com.example.chatvideo.payload.response;

import lombok.Data;

@Data
public class JwtResponse {

    private String token;
    private String type = "Bearer";
    private long id;
    private String username;
    private String email;

    public JwtResponse(String accessToken, long id, String username, String email) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
    }
}
