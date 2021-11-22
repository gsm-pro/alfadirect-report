package com.github.gsmpro.brokerage.gateway;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

@Component
public class InternetBrowsingGateway {
    public StringBuilder getWebPage(String url) {
        var page = new StringBuilder();
        try (var br = new BufferedReader(
                new InputStreamReader(
                        new URL(url).openConnection().getInputStream()))) {
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                page.append(inputLine);
            }
        } catch (Exception e) {
            return page;
        }
        return page;
    }
}