package com.jobsight.company.mcp;

import com.jobsight.company.common.ApiPaths;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Public documentation only; never includes account data or OAuth credentials. */
@RestController
public class PluginGuideController {
    public static final String SKILL_URI = "jobsight://guide/skill";
    private static final String ROOT = "jobsight-plugin/";
    private static final String SKILL = "skills/jobsight/SKILL.md";
    private final McpOAuthConfig oauth;
    private final ObjectMapper mapper;
    public PluginGuideController(McpOAuthConfig oauth, ObjectMapper mapper) { this.oauth = oauth; this.mapper = mapper; }
    public static String skillText() {
        try { return new ClassPathResource(ROOT + SKILL).getContentAsString(StandardCharsets.UTF_8); }
        catch (IOException e) { throw new IllegalStateException("Packaged JobSight skill is missing", e); }
    }
    @GetMapping(ApiPaths.PLUGIN_CONFIG)
    public Map<String, Object> config() {
        return Map.of("mcpUrl", oauth.resource(), "authorizationUrl", oauth.base() + ApiPaths.MCP_AUTHORIZE,
                "tokenUrl", oauth.base() + ApiPaths.MCP_TOKEN, "registrationMethod", "CIMD",
                "scopes", List.of(McpOAuthConfig.READ, McpOAuthConfig.WRITE));
    }
    @GetMapping(value = ApiPaths.PLUGIN_SKILL, produces = "text/markdown;charset=UTF-8")
    public ResponseEntity<String> skill() {
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=SKILL.md").body(skillText());
    }
    @GetMapping(value = ApiPaths.PLUGIN_DOWNLOAD, produces = "application/zip")
    public ResponseEntity<byte[]> download() throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            for (String path : List.of(".codex-plugin/plugin.json", ".mcp.json", SKILL)) {
                zip.putNextEntry(new ZipEntry(path));
                if (path.equals(".mcp.json")) {
                    zip.write(mapper.writeValueAsBytes(Map.of("mcpServers", Map.of("jobsight",
                            Map.of("type", "http", "url", oauth.resource())))));
                } else {
                    try (var input = new ClassPathResource(ROOT + path).getInputStream()) { input.transferTo(zip); }
                }
                zip.closeEntry();
            }
        }
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=jobsight-plugin.zip")
                .header("Cache-Control", "no-cache").body(bytes.toByteArray());
    }
}
