package com.jobsight.company.mcp;

import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.settings.*;
import tools.jackson.databind.ObjectMapper;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.function.Function;

/** Restricted CIMD resolver: never fetch user-selected hosts, redirects, or JWKS. */
final class ChatGptClients implements RegisteredClientRepository {
    private record Cached(RegisteredClient client, Instant expires) {}
    private final RegisteredClientRepository manual;
    private final Function<String, Map<String, Object>> fetch;
    private final Map<String, Cached> cache = new LinkedHashMap<>(128, .75f, true);
    private final Clock clock;

    ChatGptClients(RegisteredClient manual, ObjectMapper mapper) {
        this(manual, url -> load(url, mapper), Clock.systemUTC());
    }
    ChatGptClients(RegisteredClient manual, Function<String, Map<String, Object>> fetch, Clock clock) {
        this.manual = new InMemoryRegisteredClientRepository(manual); this.fetch = fetch; this.clock = clock;
    }
    @Override public void save(RegisteredClient client) { manual.save(client); }
    @Override public RegisteredClient findById(String id) {
        var client = manual.findById(id);
        return client != null ? client : resolve(id);
    }
    @Override public RegisteredClient findByClientId(String id) {
        var client = manual.findByClientId(id);
        return client != null ? client : resolve(id);
    }
    static boolean allowed(String id) {
        return id != null && id.matches("https://chatgpt\\.com/oauth/(?:[A-Za-z0-9_-]{1,128}/)?client\\.json");
    }
    // ponytail: serialized cache misses, 128 clients; use bounded concurrent loading if traffic requires it.
    private synchronized RegisteredClient resolve(String id) {
        if (!allowed(id)) return null;
        Cached existing = cache.get(id);
        if (existing != null && existing.expires().isAfter(clock.instant())) return existing.client();
        cache.remove(id);
        try {
            Map<String, Object> doc = fetch.apply(id);
            if (!id.equals(doc.get("client_id"))) return null;
            Object plural = doc.get("token_endpoint_auth_methods_supported");
            boolean publicClient = plural instanceof List<?> methods ? methods.contains("none")
                    : "none".equals(doc.get("token_endpoint_auth_method"));
            Object grants = doc.getOrDefault("grant_types", List.of("authorization_code"));
            Object responses = doc.getOrDefault("response_types", List.of("code"));
            // Metadata describes client capabilities, not grants we must issue (refresh remains unsupported).
            if (!publicClient || !(grants instanceof List<?> supportedGrants) || !supportedGrants.contains("authorization_code")
                    || !(responses instanceof List<?> supportedResponses) || !supportedResponses.contains("code")) return null;
            String prefix = "https://chatgpt.com/oauth/";
            String callback = id.equals(prefix + "client.json") ? "https://chatgpt.com/connector_platform_oauth_redirect"
                    : "https://chatgpt.com/connector/oauth/" + id.substring(prefix.length(), id.length() - "/client.json".length());
            if (!List.of(callback).equals(doc.get("redirect_uris"))) return null;
            var client = RegisteredClient.withId(id).clientId(id).clientName("ChatGPT · JobSight")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri(callback)
                    .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(true).build())
                    .tokenSettings(TokenSettings.builder().accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                            .accessTokenTimeToLive(Duration.ofHours(1)).authorizationCodeTimeToLive(Duration.ofMinutes(2)).build());
            McpOAuthConfig.SCOPES.forEach(client::scope);
            var registered = client.build();
            if (cache.size() >= 128) cache.remove(cache.keySet().iterator().next());
            cache.put(id, new Cached(registered, clock.instant().plusSeconds(600)));
            return registered;
        } catch (RuntimeException e) { return null; } // fail closed; never serve expired metadata after a fetch failure
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> load(String url, ObjectMapper mapper) {
        if (!allowed(url)) throw new IllegalArgumentException("Unsupported CIMD URL");
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() != 200 || connection.getContentLengthLong() > 65536)
                throw new IllegalArgumentException("Invalid CIMD response");
            String type = connection.getContentType();
            if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("application/json"))
                throw new IllegalArgumentException("CIMD must be JSON");
            try (var input = connection.getInputStream()) {
                byte[] data = input.readNBytes(65537);
                if (data.length > 65536) throw new IllegalArgumentException("CIMD too large");
                return mapper.readValue(new String(data, StandardCharsets.UTF_8), Map.class);
            }
        } catch (Exception e) { throw new IllegalArgumentException("CIMD unavailable", e); }
        finally { if (connection != null) connection.disconnect(); }
    }
}
