package com.mitocode.exception;

import java.time.LocalDateTime;

public record CustomErrorResponse(LocalDateTime timestamp, String message, String details) {}
