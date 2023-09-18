package com.example;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Path("/helloworld")
public class HelloWorldResource {
    
    private final PublicKey clientPublicKey;
    // Resources are singletons in jax-rs
    final Set<String> usedNonces = Collections.synchronizedSet(new HashSet<>()); // synchronizedSet makes it thread-safe

    public HelloWorldResource() throws Exception {

        

        KeyStore ks = KeyStore.getInstance("JKS");
        // i'm loading the public key for the request here to make this focus on protocal 
        /*
         * Public Key Infrastructure (PKI): Public keys would typically be distributed via certificates signed
         *  by a trusted Certificate Authority (CA). 
         * Instead of the server needing to have every client's public key, it would instead trust a CA's public certificate. 
         * As long as a client provides a certificate signed by this trusted CA, the server would trust it.
         */

        ks.load(new FileInputStream("client-keystore.jks"), "password".toCharArray());
        clientPublicKey = ks.getCertificate("clientkey").getPublicKey();
    }

    @GET
    public Response getHelloWorld(@HeaderParam("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String jwt = authHeader.substring("Bearer ".length());

        try {
            /*
             * Sets the signing key for validation: setSigningKey(clientPublicKey) tells the parser which key to use
             *  when validating the signature of the JWT. In this case, it's set to the clientPublicKey, 
             * which is the public key corresponding to the private key the JWT was signed with.

                Parses and Validates the JWT: parseClaimsJws(jwt) is called to parse the JWT and validate its signature.
                If the JWT was not properly signed with the corresponding private key, or if it was tampered with in any way after being signed,
                 this method would throw an exception.


                 if this is a bad request it throws something that inherits from -> io.jsonwebtoken.JwtException
                 The reason you don't need a throws clause on your getHelloWorldApi method for this exception is 
                 that JwtException (and its subclasses) are unchecked exceptions, meaning they extend RuntimeException. 
                 In Java, methods are not required to declare unchecked exceptions in their throws clause,
                  though you might want to catch them and handle them appropriately, especially in a user-facing API.


                  NOTE
                  For a cleaner user experience and to prevent leaking details to the client, you might consider catching the 
                  JwtException and returning a generic "Authentication failed" or "Invalid request" message, depending on your specific use-case.

                I'm not doing this becasuse it's a test app and i'd like to see the error /learning purposes, but in prodcution i'd long more of these details and return a 401. 

             */
            Jws<Claims> jws = Jwts.parserBuilder().setSigningKey(clientPublicKey).build().parseClaimsJws(jwt);

            String nonce = jws.getBody().get("nonce", String.class);

            // Check if nonce is already used
            if (usedNonces.contains(nonce)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Replay detected!").build();
            }

            // Store the nonce (assuming a short validity period for the token, e.g., 5 minutes)
            usedNonces.add(nonce);

            return Response.ok("Hello, World!").build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid PoP token").build();
        }
    }
}
