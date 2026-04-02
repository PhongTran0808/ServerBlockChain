package com.cuutrominhbach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class GitHubStorageService {

    private static final String GITHUB_API_URL = "https://api.github.com";
    private static final String GITHUB_TOKEN = "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";
    private static final String REPO_OWNER = "xinloihuy";
    private static final String REPO_NAME = "git_test";
    private static final String BRANCH = "main";
    private static final String FOLDER = "blockchain_img";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID().toString() + fileExtension;
        String filePath = FOLDER + "/" + fileName;

        byte[] fileContent = file.getBytes();
        String encodedContent = Base64.getEncoder().encodeToString(fileContent);

        try {
            // Get SHA of existing file (if any) for update
            String currentSha = getFileSha(filePath);

            // Create request body
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("message", "Upload blockchain image: " + fileName);
            requestBody.put("branch", BRANCH);
            requestBody.put("content", encodedContent);
            if (currentSha != null) {
                requestBody.put("sha", currentSha);
            }

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            String url = GITHUB_API_URL + "/repos/" + REPO_OWNER + "/" + REPO_NAME + "/contents/" + filePath;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + GITHUB_TOKEN)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("Content-Type", "application/json")
                    .method("PUT", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                // Return the raw GitHub URL
                return "https://raw.githubusercontent.com/" + REPO_OWNER + "/" + REPO_NAME + "/" + BRANCH + "/" + filePath;
            } else {
                throw new RuntimeException("Failed to upload file to GitHub. Status: " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upload interrupted", e);
        }
    }

    private String getFileSha(String filePath) {
        try {
            String url = GITHUB_API_URL + "/repos/" + REPO_OWNER + "/" + REPO_NAME + "/contents/" + filePath;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + GITHUB_TOKEN)
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseData = objectMapper.readValue(response.body(), Map.class);
                return (String) responseData.get("sha");
            }
            return null;
        } catch (Exception e) {
            return null; // File doesn't exist yet
        }
    }
}
