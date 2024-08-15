package com.bts.tes_coding.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotEmpty(message = "{username.required}")
    private String username;

    @NotEmpty(message = "{email.required}")
    private String email;

    @NotEmpty(message = "{password.required}")
    @Size(min = 6, message = "{password.length}")
    private String password;
}
