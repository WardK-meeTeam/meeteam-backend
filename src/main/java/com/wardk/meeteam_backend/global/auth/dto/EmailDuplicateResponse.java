package com.wardk.meeteam_backend.global.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailDuplicateResponse {

    Boolean exists;
    String message;
}
