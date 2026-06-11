package com.back.catchmate.orchestration.oauth.dto.response;

public record AuthorizeRedirect(String url, String state) {}
