package com.jobsight.company.mcp;

import java.util.Set;
import java.util.UUID;

/** An OAuth delegation, never a Google token or a browser session. */
public record McpPrincipal(UUID userId, Set<String> scopes) {}
