package com.wardk.meeteam_backend.global.loginRegister;

import lombok.Data;

@Data
public class UploadFile {

    private String originalFileName;

    private String storeFileName;

    public UploadFile(String originalFileName, String storeFileName) {
        this.originalFileName = originalFileName;
        this.storeFileName = storeFileName;
    }
}
