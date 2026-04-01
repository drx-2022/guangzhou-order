package com.example.guangzhouorder.dto.chat;

import com.example.guangzhouorder.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private Long messageId;

    private String senderEmail;

    private String senderName;

    private String senderRole;

    private Long conversationId;

    private String messageType;

    private String content;

    private String mediaUrl;

    private LocalDateTime createdAt;

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .senderEmail(message.getSender().getEmail())
                .senderName(message.getSender().getName())
                .senderRole(message.getSender().getRole())
                .conversationId(message.getConversation().getConversationId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .mediaUrl(message.getMediaUrl())
                .createdAt(message.getCreatedAt())
                .build();
    }
}

