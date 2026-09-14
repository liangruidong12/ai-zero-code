package com.wait.aizerocode.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.wait.aizerocode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.wait.aizerocode.model.dto.chathistory.ChatHistorySaveRequest;
import com.wait.aizerocode.model.entity.ChatHistory;
import com.wait.aizerocode.model.entity.User;
import com.wait.aizerocode.model.vo.ChatHistoryVO;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 对话历史 服务层。
 *
 * @author waitwait
 */
public interface ChatHistoryService extends IService<ChatHistory> {



    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory,int maxCount);

    /**
     * 保存一条对话消息（用户消息或 AI 消息）
     *
     * @param chatHistorySaveRequest 保存请求
     * @param
     * @return 对话历史 id
     */
    long saveMessage(ChatHistorySaveRequest chatHistorySaveRequest, User loginUser);

    /**
     * 级联删除应用下的所有对话历史
     *
     * @param appId 应用 id
     * @return 是否删除成功
     */
    boolean removeByAppId(Long appId);

    /**
     * 获取对话历史封装类
     *
     * @param chatHistory 对话历史
     * @return 对话历史封装类
     */
    ChatHistoryVO getChatHistoryVo(ChatHistory chatHistory);

    /**
     * 获取对话历史封装列表
     *
     * @param chatHistoryList 对话历史列表
     * @return 对话历史封装列表
     */
    List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList);

    /**
     * 获取查询条件
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 分页查询应用的对话历史（仅应用创建者和管理员可见，游标分页，每次最多 20 条）
     *
     * @param chatHistoryQueryRequest 查询请求
     * @param
     * @return 对话历史分页列表
     */
    Page<ChatHistoryVO> listChatHistoryVOByPage(ChatHistoryQueryRequest chatHistoryQueryRequest, User loginUser);

    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页列表
     */
    Page<ChatHistory> adminListChatHistoryByPage(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
