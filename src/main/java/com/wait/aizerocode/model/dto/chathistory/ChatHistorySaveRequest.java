package com.wait.aizerocode.model.dto.chathistory;

import lombok.Data;

import java.io.Serializable;

/**
 * 对话历史保存请求
 */
@Data
public class ChatHistorySaveRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型（user/ai）
     */
    private String messageType;

    private static final long serialVersionUID = 1L;
}
