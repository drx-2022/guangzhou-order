package com.example.guangzhouorder.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    @NotNull(message = "Conversation ID cannot be null")
    private Long conversationId;

    @Builder.Default
    private String messageType = "TEXT";

    private String content;

    private String mediaUrl;
}

