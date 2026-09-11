package com.wait.aizerocode.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wait.aizerocode.model.entity.ChatHistory;
import com.wait.aizerocode.mapper.ChatHistoryMapper;
import com.wait.aizerocode.service.ChatHistoryService;
import org.springframework.stereotype.Service;

/**
 * 对话历史 服务层实现。
 *
 * @author waitwait
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

}
