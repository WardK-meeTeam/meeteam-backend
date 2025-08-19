package com.wardk.meeteam_backend.global.auth;

import lombok.Getter;

@Getter
public class FileObject {

    private String originalFileName;

    private String storeFileName;

    public FileObject(String originalFileName, String storeFileName) {
        this.originalFileName = originalFileName;
        this.storeFileName = storeFileName;
    }
}
