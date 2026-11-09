package org.example.lab1;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public final class AuthController {    private final JwtEncoder encoder;
    private final Properties users = new Properties();
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);
    private final String dummyHash = bcrypt.encode(UUID.randomUUID().toString());

    public AuthController(JwtEncoder encoder) throws IOException {
        this.encoder = encoder;
        try (Reader reader = Files.newBufferedReader(Path.of(".lab-users"))) {
            users.load(reader);
        }
    }

    public record LoginRequest(String username, String password) {}

    @PostMapping(value = "/auth/login", consumes = "application/json")    public Map<String, String> login(@RequestBody LoginRequest request) {
        if (request.username() == null || request.password() == null
                || request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String hash = users.getProperty(request.username());
        boolean matches = bcrypt.matches(request.password(), hash == null ? dummyHash : hash);
        if (!matches || hash == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SecurityConfig.ISSUER)
                .subject(request.username())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
        return Map.of("access_token", token, "token_type", "Bearer");
    }
}