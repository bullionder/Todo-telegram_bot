package com.todoschedulerbot.demo.Entities;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "chat-status")
public class ChatStatusEntity {
    @Id
    private Long idChat;
    private String currentCommand;
}
