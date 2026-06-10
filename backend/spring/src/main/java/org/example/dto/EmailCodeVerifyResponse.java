package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailCodeVerifyResponse {
    private boolean verified;
    private String verifyToken;
}
