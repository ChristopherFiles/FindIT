package com.findit.campus.dto;

import jakarta.validation.constraints.Size;

public class CommentForm {
    @Size(max = 1200)
    private String message;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
