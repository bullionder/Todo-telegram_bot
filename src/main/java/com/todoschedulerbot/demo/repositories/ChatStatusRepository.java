package com.todoschedulerbot.demo.repositories;

import com.todoschedulerbot.demo.entities.ChatStatusEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ChatStatusRepository extends CrudRepository<ChatStatusEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = "UPDATE `chat-status` SET current_command = ?1 WHERE id_chat = ?2", nativeQuery = true)
    void updateChatStatusById(String currentCommand, Long chatId);
}
