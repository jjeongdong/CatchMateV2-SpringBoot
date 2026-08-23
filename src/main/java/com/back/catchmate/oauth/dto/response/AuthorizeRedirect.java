package com.back.catchmate.oauth.dto.response;

public record AuthorizeRedirect(String url, String state) {}
