package com.todoschedulerbot.demo.Requests;

import lombok.Data;

@Data
public class Transcript {
    private String username;
    private Long userId;
    private boolean completed;
    private String todoTitle;
}
