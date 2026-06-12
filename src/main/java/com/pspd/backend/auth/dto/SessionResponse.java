package com.pspd.backend.auth.dto;

public record SessionResponse(
    String  sid,
    String  device,
    String  ip,
    String  createdAt,
    String  lastSeenAt,
    boolean current
) {}
