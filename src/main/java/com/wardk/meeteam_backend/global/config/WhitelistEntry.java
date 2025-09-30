package com.wardk.meeteam_backend.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Getter
@Setter
public class WhitelistEntry {
    private String method;
    private String uri;

    public WhitelistEntry() {}

    public WhitelistEntry(String method, String uri) {
        this.method = method;
        this.uri = uri;
    }
}
