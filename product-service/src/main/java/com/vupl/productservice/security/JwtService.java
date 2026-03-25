package com.vupl.productservice.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;
@Service
public class JwtService {
    @Value("${jwt.secret}") private String secretKey;
    public String extractUsername(String token) { return extractClaim(token, Claims::getSubject); }
    public boolean isTokenValid(String token) {
        try { return !extractClaim(token, Claims::getExpiration).before(new Date()); }
        catch (Exception e) { return false; }
    }
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(token).getPayload());
    }
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }
    private SecretKey getSignKey() { return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)); }
}
