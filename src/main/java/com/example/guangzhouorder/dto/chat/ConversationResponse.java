package com.example.guangzhouorder.dto.chat;

import com.example.guangzhouorder.entity.Conversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private Long conversationId;

    private String customerEmail;

    private String customerName;

    private LocalDateTime lastMessageAt;

    private long unreadCount;

    public static ConversationResponse from(Conversation conversation, long unreadCount) {
        return ConversationResponse.builder()
                .conversationId(conversation.getConversationId())
                .customerEmail(conversation.getCustomer().getEmail())
                .customerName(conversation.getCustomer().getName())
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(unreadCount)
                .build();
    }
}

