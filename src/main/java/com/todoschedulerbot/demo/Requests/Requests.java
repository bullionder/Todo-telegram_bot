package com.todoschedulerbot.demo.Requests;

import com.google.gson.Gson;
import io.swagger.client.model.UserEntity;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
public class Requests {
    private static final String REGISTER = "/register";
    private static final String DELETE = "/delete";
    private static final String DELETE_ALL = "/delete-all";
    private static final String CREATE_TODO = "/create-todo";

    private static final String GET_TODOS = "/get-todos";
    private static final String GET_TODO = "/get-todo";


    private static final String LOCALHOST_USERS = "http://localhost:8080/users";
    private static final String LOCALHOST_TODOS = "http://localhost:8080/todos";

    private static HttpClient httpClient;

    public static void registerRequest(long chatId, String username) {
        try {
            //todo isn't the name confusing? It returns String, not Json object
            String jsonRequest = createJsonWithIdAndUsername(chatId, username);
            HttpRequest postRequest = buildRegisterPostHttpRequest(jsonRequest);
            httpClient = HttpClient.newHttpClient();
            HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
            log.info(String.valueOf(postResponse));
        } catch (URISyntaxException | InterruptedException | IOException e) {
            log.error("Could not create new user.");
            throw new RuntimeException(e);
        }
    }

    public static String displayAllTodosRequest(long chatId) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest getRequest = buildGetHttpRequest(LOCALHOST_USERS + GET_TODOS + "/" + chatId);
        httpClient = HttpClient.newHttpClient();
        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
        //todo should I log postResponse here or is it better to log them all in main app with the db
        return getResponse.body();
    }

    public static void addTodoRequest(long chatId, String todoTitle) throws URISyntaxException, IOException, InterruptedException {
        String jsonRequest = createJsonWithTodo(todoTitle);
        HttpRequest postRequest = buildCreateTodoHttpRequest(chatId, jsonRequest);
        httpClient = HttpClient.newHttpClient();
        HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
        log.info(String.valueOf(postResponse));
    }

    public static void deleteUserRequest(long chatId) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest deleteRequest = buildDeleteUserHttpRequest(chatId);
        httpClient = HttpClient.newHttpClient();
        HttpResponse<String> postResponse = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
        log.info(String.valueOf(postResponse));
        log.error("Could not delete user.");
    }

    public static void deleteTodoRequest(long chatId, String todoTitle) throws URISyntaxException, IllegalArgumentException, InterruptedException, IOException {
        String jsonRequest = createJsonWithTodo(todoTitle);
        HttpRequest postRequest = buildDeleteTodoHttpRequest(chatId, jsonRequest);
        httpClient = HttpClient.newHttpClient();
        HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
        if (postResponse.statusCode() == 400) throw new IllegalArgumentException();
        log.info(String.valueOf(postResponse));
    }

    public static void deleteAllTodosRequest(long chatId) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest deleteRequest = buildDeleteTodosHttpRequest(chatId);
        httpClient = HttpClient.newHttpClient();
        HttpResponse<String> deleteResponse = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
        log.info(String.valueOf(deleteResponse));
    }

    private static HttpRequest buildDeleteTodosHttpRequest(long chatId) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(LOCALHOST_TODOS + DELETE_ALL + "/" + chatId))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();
    }

    private static HttpRequest buildDeleteUserHttpRequest(long chatId) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(LOCALHOST_USERS + DELETE + "/" + chatId))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();
    }

    private static HttpRequest buildDeleteTodoHttpRequest(long chatId, String jsonRequest) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(LOCALHOST_TODOS + DELETE + "/" + chatId))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();
    }

    private static HttpRequest buildCreateTodoHttpRequest(long chatId, String jsonRequest) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(LOCALHOST_TODOS + CREATE_TODO + "/" + chatId))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();
    }

    private static HttpRequest buildRegisterPostHttpRequest(String jsonRequest) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(LOCALHOST_USERS + REGISTER))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();
    }

    private static String createJsonWithTodo(String todoTitle) {
        Transcript transcript = new Transcript();
        transcript.setTodoTitle(todoTitle);
        transcript.setCompleted(false);
        Gson gson = new Gson();
        return gson.toJson(transcript);
    }

    private static String createJsonWithIdAndUsername(long chatId, String username) {
        Transcript transcript = new Transcript();
        transcript.setUserId(chatId);
        transcript.setUsername(username);
        Gson gson = new Gson();
        return gson.toJson(transcript);
    }

    private static HttpRequest buildGetHttpRequest(String path) throws URISyntaxException {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(new URI(path))
                .header("Content-Type", "application/json")
                .GET().build();
        return getRequest;
    }
}
