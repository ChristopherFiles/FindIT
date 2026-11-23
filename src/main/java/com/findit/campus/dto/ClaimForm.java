package com.findit.campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClaimForm {
    @NotBlank
    @Size(max = 1200)
    private String message;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
