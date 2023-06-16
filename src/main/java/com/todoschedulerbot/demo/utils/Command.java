package com.todoschedulerbot.demo.utils;

import lombok.Getter;

@Getter
public enum Command {
    DELETE_ACCOUNT("/account_delete"),
    DISPLAY_ALL_TODOS("/todo_display_all"),
    ADD_DESCRIPTION("/todo_add_description"),
    ADD_TODO("/todo_add"),
    DELETE_TODO("/todo_delete_one"),
    DELETE_ALL_TODOS("/todo_delete_all");
    private final String commandName;

    Command(String commandName) {
        this.commandName = commandName;
    }
}
