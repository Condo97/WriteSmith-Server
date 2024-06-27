package com.writesmith.google.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.Constants;
import httpson.Httpson;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

public class GoogleSearcher {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Item {

            private String title;
            private String link;

            public Item() {

            }

            public Item(String title, String link) {
                this.title = title;
                this.link = link;
            }

            public String getTitle() {
                return title;
            }

            public String getLink() {
                return link;
            }

        }

        private List<Item> items;

        public Response() {

        }

        public Response(List<Item> items) {
            this.items = items;
        }

        public List<Item> getItems() {
            return items;
        }

    }

    public static Response search(String key, String searchEngineID, String query) throws URISyntaxException, IOException, InterruptedException {
        // Null check for key, searchEngineID, and null and empty check for query returning null TODO: Should this throw an exception? Probably.. also I need to do exception handling for this program in general
        if (key == null || searchEngineID == null || query == null || query.isEmpty())
            return null;

        // Build URI
        URI uri = new URI(Constants.Google_Search_Base_URL + "?" + "key=" + key + "&" + "cx=" + searchEngineID + "&" + "q=" + URLEncoder.encode(query, "UTF-8"));

        // Create HttpClient
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(Constants.GOOGLE_TIMEOUT_MINUTES)).build();

        // Do request
        JsonNode response = Httpson.sendGET(
                httpClient,
                uri
        );

        // Return response mapped ot GoogleSearcher Response
        return new ObjectMapper().treeToValue(response, Response.class);
    }

}
