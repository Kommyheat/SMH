package org.example.dto;

import lombok.Getter;

@Getter
public class LinkCodeResponse {

    private final String linkCode;

    public LinkCodeResponse(String linkCode) {
        this.linkCode = linkCode;
    }
}
