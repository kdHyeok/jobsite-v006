package com.jobsight.company.mcp;

import com.jobsight.company.common.ApiPaths;
import com.jobsight.company.user.AppUserRepository;
import com.jobsight.company.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.*;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.*;

@Configuration
public class McpOAuthConfig {
    public static final String READ = "jobsight.read", WRITE = "jobsight.write", ADMIN = "jobsight.admin";
    public static final List<String> SCOPES = List.of(READ, WRITE, ADMIN);
    private final String base;

    public McpOAuthConfig(@Value("${app.public-base-url}") String base) {
        URI uri = URI.create(base);
        if (uri.getHost() == null || uri.getRawQuery() != null || uri.getFragment() != null
                || uri.getUserInfo() != null || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))
                || !("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme())
                    && Set.of("127.0.0.1", "localhost", "[::1]").contains(uri.getHost())))) {
            throw new IllegalArgumentException("PUBLIC_BASE_URL must be an HTTPS origin (HTTP loopback for development)");
        }
        this.base = base.replaceAll("/$", "");
    }

    public String base() { return base; }
    public String resource() { return base + ApiPaths.MCP; }
    public String challenge() {
        return "Bearer resource_metadata=\"" + base + ApiPaths.MCP_METADATA + "\", scope=\"" + READ + "\"";
    }

    @Bean
    RegisteredClientRepository mcpClients(
            @Value("${app.mcp.redirect-uris:https://chatgpt.com/connector_platform_oauth_redirect}") String redirects,
            tools.jackson.databind.ObjectMapper mapper) {
        var client = RegisteredClient.withId("jobsight-plugin")
                .clientId("jobsight-plugin").clientName("JobSight plugin")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(true).build())
                .tokenSettings(TokenSettings.builder().accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .accessTokenTimeToLive(Duration.ofHours(1)).authorizationCodeTimeToLive(Duration.ofMinutes(2)).build());
        SCOPES.forEach(client::scope);
        for (String redirect : redirects.split(",")) {
            URI uri = URI.create(redirect.trim());
            if (uri.getHost() == null || uri.getFragment() != null || uri.getUserInfo() != null
                    || !("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme())
                    && Set.of("127.0.0.1", "localhost", "[::1]").contains(uri.getHost())))) {
                throw new IllegalArgumentException("MCP redirect URI must be HTTPS or HTTP loopback");
            }
            client.redirectUri(uri.toString());
        }
        return new ChatGptClients(client.build(), mapper);
    }

    @Bean
    OAuth2AuthorizationService mcpAuthorizations() {
        // ponytail: single replica; use JDBC authorization/consent stores for restart persistence or scaling.
        return new InMemoryOAuth2AuthorizationService();
    }

    @Bean
    OAuth2AuthorizationConsentService mcpConsents() { return new InMemoryOAuth2AuthorizationConsentService(); }

    @Bean
    AuthorizationServerSettings mcpAuthorizationSettings() {
        return AuthorizationServerSettings.builder().issuer(base)
                .authorizationEndpoint(ApiPaths.MCP_AUTHORIZE).tokenEndpoint(ApiPaths.MCP_TOKEN)
                .tokenRevocationEndpoint(ApiPaths.MCP_REVOKE).tokenIntrospectionEndpoint(ApiPaths.MCP_INTROSPECT).build();
    }

    @Bean
    OAuth2TokenGenerator<?> mcpTokenGenerator() {
        var access = new OAuth2AccessTokenGenerator();
        access.setAccessTokenCustomizer(context -> context.getClaims().audience(List.of(resource())));
        return access;
    }

    @Bean @Order(1)
    SecurityFilterChain mcpAuthorizationChain(HttpSecurity http) throws Exception {
        var server = new OAuth2AuthorizationServerConfigurer();
        http.securityMatcher(server.getEndpointsMatcher())
                .with(server, config -> config.authorizationServerMetadataEndpoint(metadata ->
                        metadata.authorizationServerMetadataCustomizer(builder -> builder
                                .claim("client_id_metadata_document_supported", true)
                                .tokenEndpointAuthenticationMethods(methods -> { methods.clear(); methods.add("none"); }))))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(
                        (request, response, error) -> response.sendRedirect(ApiPaths.OAUTH_AUTHORIZATION)))
                .addFilterAfter(new OncePerRequestFilter() {
                    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                               FilterChain chain) throws ServletException, IOException {
                        String path = request.getRequestURI();
                        // Consent POST uses Spring's stored request, not an untrusted resource parameter.
                        boolean initial = path.equals(ApiPaths.MCP_AUTHORIZE) && "GET".equals(request.getMethod());
                        if (initial || path.equals(ApiPaths.MCP_TOKEN)) {
                            String[] resources = request.getParameterValues("resource");
                            if (resources == null || resources.length != 1 || !resource().equals(resources[0])) {
                                response.setStatus(400); response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"invalid_target\"}"); return;
                            }
                            if (initial && !"S256".equals(request.getParameter("code_challenge_method"))) {
                                response.setStatus(400); response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"invalid_request\"}"); return;
                            }
                        }
                        chain.doFilter(request, response);
                    }
                }, SecurityContextHolderFilter.class);
        return http.build();
    }

    @Bean @Order(2)
    SecurityFilterChain mcpResourceChain(HttpSecurity http, OAuth2AuthorizationService authorizations,
                                        AppUserRepository users) throws Exception {
        http.securityMatcher(ApiPaths.MCP, ApiPaths.MCP_METADATA, ApiPaths.MCP_METADATA + ApiPaths.MCP,
                        ApiPaths.PLUGIN_CONFIG, ApiPaths.PLUGIN_SKILL, ApiPaths.PLUGIN_DOWNLOAD)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth.requestMatchers(ApiPaths.MCP_METADATA,
                        ApiPaths.MCP_METADATA + ApiPaths.MCP, ApiPaths.PLUGIN_CONFIG,
                        ApiPaths.PLUGIN_SKILL, ApiPaths.PLUGIN_DOWNLOAD).permitAll().anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, error) -> {
                    response.setHeader("WWW-Authenticate", challenge()); response.setStatus(401);
                }))
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                               FilterChain chain) throws ServletException, IOException {
                        String origin = request.getHeader("Origin");
                        if (origin != null && !origin.equals(base)) { response.setStatus(403); return; }
                        String header = request.getHeader("Authorization");
                        if (header != null && header.startsWith("Bearer ")) {
                            var authorization = authorizations.findByToken(header.substring(7), OAuth2TokenType.ACCESS_TOKEN);
                            if (authorization != null && authorization.getAccessToken() != null
                                    && authorization.getAccessToken().isActive()) {
                                var claims = authorization.getAccessToken().getClaims();
                                if (claims != null && claims.get("aud") instanceof Collection<?> audience
                                        && audience.contains(resource())) {
                                    var user = users.findById(UUID.fromString(authorization.getPrincipalName())).orElse(null);
                                    if (user != null && user.getStatus() == UserStatus.ACTIVE) {
                                        var principal = new McpPrincipal(user.getId(), authorization.getAuthorizedScopes());
                                        SecurityContextHolder.getContext().setAuthentication(
                                                new UsernamePasswordAuthenticationToken(principal, null,
                                                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
                                    }
                                }
                            }
                        }
                        chain.doFilter(request, response);
                    }
                }, AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
