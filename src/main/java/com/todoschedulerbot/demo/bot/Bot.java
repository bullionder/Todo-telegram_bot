package com.todoschedulerbot.demo.bot;

import com.todoschedulerbot.demo.entities.ChatStatusEntity;
import com.todoschedulerbot.demo.repositories.ChatStatusRepository;
import com.todoschedulerbot.demo.configs.BotConfig;
import com.todoschedulerbot.demo.utils.Command;
import io.swagger.client.api.TodoControllerApi;
import io.swagger.client.api.UserControllerApi;
import io.swagger.client.model.TodoDto;
import io.swagger.client.model.UserDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.URISyntaxException;

@Component
@RequiredArgsConstructor
@Getter
@Slf4j
public class Bot extends TelegramLongPollingBot {
    @Qualifier("io.swagger.client.api.UserControllerApi")
    @Autowired
    private UserControllerApi userControllerApi;
    @Qualifier("io.swagger.client.api.TodoControllerApi")
    @Autowired
    private TodoControllerApi todoControllerApi;
    private final BotConfig botConfig;
    private final ChatStatusRepository chatStatusRepository;
    private ChatStatusEntity chatStatusEntity;
    private TodoDto todoDto = new TodoDto();

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            processMessageFromUser(update, chatId, text);
        }
    }

    private void processMessageFromUser(Update update, long chatId, String text) {
        boolean chatStatusRegistered = chatStatusRepository.findById(chatId).isPresent();
        registerChatStatusIfNotRegistered(update, chatId, chatStatusRegistered);

        String currentChatStatus = chatStatusEntity.getCurrentCommand();
        if (currentChatStatus.equals(Command.ADD_TODO.name())) {
            todoDto.setTodoTitle(text);
            sendTextMessage(chatId, "Add description to your new todo.");
            chatStatusRepository.updateChatStatusById(Command.ADD_DESCRIPTION.name(), chatId);
        } else if (currentChatStatus.equals(Command.ADD_DESCRIPTION.name())) {
            todoDto.setTodoDescription(text);
            todoControllerApi.createTodoUsingPOST(chatId, todoDto);
            todoDto.setTodoDescription(null);
            todoDto.setTodoTitle(null);
            sendTextMessage(chatId, "Todo created.");
            //todo is it ok to set null values to description and title?
            chatStatusRepository.updateChatStatusById("", chatId);
        } else if (currentChatStatus.equals(Command.DELETE_TODO.name())) {
            try {
                todoControllerApi.deleteTodoUsingDELETE(Long.valueOf(text));
                sendTextMessage(chatId, "Todo deleted.");
                chatStatusRepository.updateChatStatusById("", chatId);
            } catch (Exception e) {
                sendTextMessage(chatId, String.format("Couldn't delete todo with id [%s].", text) +
                        "\nType its id again (e.g 3).");
            }
        } else {
            readCommand(chatId, text);
        }
    }

    private void registerChatStatusIfNotRegistered(Update update, long chatId, boolean chatStatusRegistered) {
        if (chatStatusRegistered && chatStatusRepository.findById(chatId).isPresent()) {
            chatStatusEntity = chatStatusRepository.findById(chatId).get();
        } else {
            saveNewChatStatusEntityInDb(chatId);
            registerNewUserInDb(update, chatId);
        }
    }

    private void saveNewChatStatusEntityInDb(long chatId) {
        chatStatusEntity = new ChatStatusEntity(chatId, "");
        chatStatusRepository.save(chatStatusEntity);
    }

    private void registerNewUserInDb(Update update, long chatId) {
        String username = update.getMessage().getChat().getFirstName();
        userControllerApi.registerUserUsingPOST(new UserDto()
                .userId(chatId)
                .username(username));
        startCommandReceived(chatId, username);
    }

    private void readCommand(long chatId, String text) {
        try {
            switch (text) {
                case "/account_delete":
                    chatStatusRepository.updateChatStatusById(Command.DELETE_ACCOUNT.name(), chatId);
                    executeDeleteCommand(chatId);
                    chatStatusRepository.updateChatStatusById("", chatId);
                    break;
                case "/todo_display_all":
                    chatStatusRepository.updateChatStatusById(Command.DISPLAY_ALL_TODOS.name(), chatId);
                    sendTextMessage(chatId, "Your todos: \n" + userControllerApi.getTodosUsingGET(chatId));
                    chatStatusRepository.updateChatStatusById("", chatId);
                    break;
                case "/todo_delete_all":
                    chatStatusRepository.updateChatStatusById(Command.DELETE_ALL_TODOS.name(), chatId);
                    todoControllerApi.deleteAllTodosUsingDELETE(chatId);
                    sendTextMessage(chatId, "Todos deleted.");
                    chatStatusRepository.updateChatStatusById("", chatId);
                    break;
                case "/todo_add":
                    chatStatusRepository.updateChatStatusById(Command.ADD_TODO.name(), chatId);
                    sendTextMessage(chatId, "Name your new todo.");
                    break;
                case "/todo_delete_one":
                    chatStatusRepository.updateChatStatusById(Command.DELETE_TODO.name(), chatId);
                    sendTextMessage(chatId, "Which todo would you like to delete?\n" +
                            userControllerApi.getTodosUsingGET(chatId) + "\n" +
                            "Type its id (e.g. 3).");
                    break;
                default:
                    if (!text.equals("/start")) {
                        chatStatusRepository.updateChatStatusById("", chatId);
                        sendTextMessage(chatId, "Sorry, command was not recognized.");
                    }
            }
        } catch (RestClientException | URISyntaxException | InterruptedException | IOException e) {
            log.error(e.getMessage());
            sendTextMessage(chatId, "Something went wrong.");
        }
    }

    private void executeDeleteCommand(long chatId) throws URISyntaxException, IOException, InterruptedException {
        try {
            userControllerApi.deleteUserUsingDELETE(chatId);
            log.info("User [" + chatId + "] deleted.");
            sendTextMessage(chatId, "User deleted.");
        } catch (RestClientException e) {
            throw new RuntimeException(e);
        }
    }

    private void startCommandReceived(long chatId, String firstName) {
        String commands = listCommandsAvailable();
        String answer = "Hi, " + firstName + ", nice to meet you! " +
                "Type one of the following commands:\n" + commands;

        sendTextMessage(chatId, answer);
    }

    private String listCommandsAvailable() {
        StringBuilder builder = new StringBuilder();
        for (Command command : Command.values()) {
            builder.append("⚡ ").append(command.getCommandName()).append("\n");
        }
        return builder.toString().trim();
    }

    private void sendTextMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}


