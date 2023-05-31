package com.todoschedulerbot.demo.Bot;

import com.todoschedulerbot.demo.Entities.ChatStatusEntity;
import com.todoschedulerbot.demo.Repositories.ChatStatusRepository;
import com.todoschedulerbot.demo.configs.BotConfig;
import com.todoschedulerbot.demo.Requests.Requests;
import com.todoschedulerbot.demo.utils.Commands;
import com.todoschedulerbot.demo.utils.CurrentCommand;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.URISyntaxException;

//todo can I make this class controller and then process exceptions using class with @ExceptionHandler?
// (might be wrong with the name)
@Component
@RequiredArgsConstructor
@Getter
@Slf4j
public class Bot extends TelegramLongPollingBot {
    private final BotConfig botConfig;
    private final ChatStatusRepository chatStatusRepository;
    private ChatStatusEntity chatStatusEntity;
    private final String[] commandsAvailable = new String[]{Commands.TODO_ADD, Commands.TODO_DISPLAY_ALL, Commands.TODO_DELETE_ONE, Commands.TODO_DELETE_ALL};

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
            //todo chatId on Telegram is and Integer. Is there any sense to change datatype to Integer
            // to "optimize" program and user less memory or is it better to stick to long?
            long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();
            processMessageFromUser(update, chatId, text);
        }
    }

    private void processMessageFromUser(Update update, long chatId, String text) {
        boolean chatStatusRegistered = chatStatusRepository.findById(chatId).isPresent();
        registerChatStatusIfNotRegistered(update, chatId, chatStatusRegistered);

        String currentChatStatus = chatStatusEntity.getCurrentCommand();
        if (currentChatStatus.equals(CurrentCommand.ADD_TODO.name())) {
            processAddTodoCommand(chatId, text);
        } else if (currentChatStatus.equals(CurrentCommand.DELETE_TODO.name())) {
            processDeleteTodoCommand(chatId, text);
        } else {
            readCommand(chatId, text);
        }
    }

    private void processDeleteTodoCommand(long chatId, String todoTitle) {
        chatStatusRepository.updateChatStatusById(CurrentCommand.COMMAND.name(), chatId);
        try {
            Requests.deleteTodoRequest(chatId, todoTitle);
            sendTextMessage(chatId, "Todo deleted.");
        } catch (URISyntaxException | IllegalArgumentException | InterruptedException | IOException e) {
            log.error(e.getMessage());
            sendTextMessage(chatId, "Could not delete todo.");
        }
    }

    private void processAddTodoCommand(long chatId, String text) {
        chatStatusRepository.updateChatStatusById(CurrentCommand.COMMAND.name(), chatId);
        try {
            Requests.addTodoRequest(chatId, text);
            sendTextMessage(chatId, "Todo created.");
        } catch (URISyntaxException | InterruptedException | IOException e) {
            log.error(e.getMessage());
            sendTextMessage(chatId, "Could not create new todo.");
        }
    }

    private void registerChatStatusIfNotRegistered(Update update, long chatId, boolean chatStatusRegistered) {
        if (chatStatusRegistered) {
            chatStatusEntity = chatStatusRepository.findById(chatId).get();
        } else {
            saveNewChatStatusEntityInDb(chatId);
            registerNewUserInDb(update, chatId);
        }
    }

    private void saveNewChatStatusEntityInDb(long chatId) {
        chatStatusEntity = new ChatStatusEntity(chatId, CurrentCommand.COMMAND.name());
        chatStatusRepository.save(chatStatusEntity);
    }

    private void registerNewUserInDb(Update update, long chatId) {
        String username = update.getMessage().getChat().getFirstName();
        startCommandReceived(chatId, username);
        Requests.registerRequest(chatId, username);
    }

    private void readCommand(long chatId, String text) {
        try {
            switch (text) {
                case "/delete_account":
                    chatStatusRepository.updateChatStatusById(CurrentCommand.DELETE_ACCOUNT.name(), chatId);
                    executeDeleteCommand(chatId);
                    chatStatusRepository.updateChatStatusById(CurrentCommand.COMMAND.name(), chatId);
                    break;
                case Commands.TODO_DISPLAY_ALL:
                    chatStatusRepository.updateChatStatusById(CurrentCommand.DISPLAY_ALL_TODOS.name(), chatId);
                    if (Requests.displayAllTodosRequest(chatId).equals("")) { // If there aren't any todos
                        sendTextMessage(chatId, "You don't have any todos.");
                    } else {
                        sendTextMessage(chatId, "Your todos: \n" + Requests.displayAllTodosRequest(chatId));
                    }
                    chatStatusRepository.updateChatStatusById(CurrentCommand.COMMAND.name(), chatId);
                    break;
                case Commands.TODO_DELETE_ALL:
                    chatStatusRepository.updateChatStatusById(CurrentCommand.DELETE_ALL_TODOS.name(), chatId);
                    if (Requests.displayAllTodosRequest(chatId).equals("")) { // If there aren't any todos
                        sendTextMessage(chatId, "You don't have any todos.");
                    } else {
                        Requests.deleteAllTodosRequest(chatId);
                        sendTextMessage(chatId, "Todos deleted.");
                    }
                    chatStatusRepository.updateChatStatusById(CurrentCommand.COMMAND.name(), chatId);
                    break;
                case Commands.TODO_ADD:
                    chatStatusRepository.updateChatStatusById(CurrentCommand.ADD_TODO.name(), chatId);
                    sendTextMessage(chatId, "Name your new todo.");
                    break;
                case Commands.TODO_DELETE_ONE:
                    chatStatusRepository.updateChatStatusById(CurrentCommand.DELETE_TODO.name(), chatId);
                    sendTextMessage(chatId, "Which todo would you like to delete?\n" +
                            Requests.displayAllTodosRequest(chatId) + "\n" +
                            "Type only its name.");
                    break;
                default:
                    if (!text.equals("/start")) {
                        chatStatusRepository.updateChatStatusById(CurrentCommand.COMMAND.name(), chatId);
                        sendTextMessage(chatId, "Sorry, command was not recognized.");
                    }
            }
        } catch (URISyntaxException | InterruptedException | IOException e) {
            log.error(e.getMessage());
            sendTextMessage(chatId, "Something went wrong.");
        }
    }

    private void executeDeleteCommand(long chatId) throws URISyntaxException, IOException, InterruptedException {
        Requests.deleteUserRequest(chatId);
        chatStatusRepository.deleteById(chatId);
        sendTextMessage(chatId, "User deleted.");
    }

    private void startCommandReceived(long chatId, String firstName) {
        String commands = listCommandsAvailable();
        String answer = "Hi, " + firstName + ", nice to meet you! " +
                "Type one of the following commands:\n" + commands;

        sendTextMessage(chatId, answer);
    }

    private String listCommandsAvailable() {
        StringBuilder builder = new StringBuilder();
        for (String command :
                commandsAvailable) {
            builder.append("⚡ ").append(command).append("\n");
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


