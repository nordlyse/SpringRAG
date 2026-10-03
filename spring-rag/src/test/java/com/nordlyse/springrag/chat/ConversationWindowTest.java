package com.nordlyse.springrag.chat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationWindowTest {

    @Test
    void windowKeepsOnlyTheNewestMessages() {
        ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(2).build();
        memory.add("session-1", new UserMessage("one"));
        memory.add("session-1", new UserMessage("two"));
        memory.add("session-1", new UserMessage("three"));

        assertThat(memory.get("session-1")).extracting(Message::getText).containsExactly("two", "three");
    }
}
