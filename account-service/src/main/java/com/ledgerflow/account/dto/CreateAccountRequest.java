package com.ledgerflow.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(
        @NotBlank String ownerId,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency) {
}
