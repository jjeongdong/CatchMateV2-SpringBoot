package com.back.catchmate.oauth.application.dto.response;

public record AuthorizeRedirect(String url, String state) {}
