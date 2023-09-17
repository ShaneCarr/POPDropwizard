package com.example;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;

public class ClientApp {
    
    public static void main(String[] args) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");

        // this would be azure key store or somethign along these lines in an authorization library. 
        ks.load(new FileInputStream("client-keystore.jks"), "password".toCharArray());
        PrivateKey clientPrivateKey = (PrivateKey) ks.getKey("clientkey", "password".toCharArray());

        String jwt = Jwts.builder()
                .setSubject("Client")
                .signWith(clientPrivateKey, SignatureAlgorithm.RS256) // sign the entire jwt. 
                .compact();

        URL url = new URL("http://localhost:8080/helloworld");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + jwt);

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String responseMessage = new String(conn.getInputStream().readAllBytes());
            System.out.println("Server Response: " + responseMessage);
        } else {
            System.out.println("Failed to get a response from the server.");
        }
    }
}
