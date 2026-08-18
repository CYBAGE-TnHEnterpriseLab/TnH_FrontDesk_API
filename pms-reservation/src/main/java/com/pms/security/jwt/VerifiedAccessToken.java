package com.pms.security.jwt;

import java.util.List;

public record VerifiedAccessToken(String username, List<String> roles) {
}
