package com.wait.aizerocode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wait.aizerocode.exception.BusinessException;
import com.wait.aizerocode.exception.ErrorCode;
import com.wait.aizerocode.exception.ThrowUtils;
import com.wait.aizerocode.mapper.ChatHistoryMapper;
import com.wait.aizerocode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.wait.aizerocode.model.dto.chathistory.ChatHistorySaveRequest;
import com.wait.aizerocode.model.entity.App;
import com.wait.aizerocode.model.entity.ChatHistory;
import com.wait.aizerocode.model.entity.User;
import com.wait.aizerocode.model.enums.ChatHistoryMessageTypeEnum;
import com.wait.aizerocode.model.enums.UserRoleEnum;
import com.wait.aizerocode.model.vo.ChatHistoryVO;
import com.wait.aizerocode.model.vo.UserVO;
import com.wait.aizerocode.service.AppService;
import com.wait.aizerocode.service.ChatHistoryService;
import com.wait.aizerocode.service.UserService;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话历史 服务层实现。
 *
 * @author waitwait
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Autowired
    private ChatHistoryMapper chatHistoryMapper;

    @Autowired
    private UserService userService;

    @Autowired
    @Lazy
    private AppService appService;


    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq(ChatHistory::getAppId, appId)
                .orderBy(ChatHistory::getCreateTime, false)
                .limit(1, maxCount);
        List<ChatHistory> historyList=this.list(wrapper);
        if(CollUtil.isEmpty(historyList)){
            return 0;
        }
        historyList=historyList.reversed();

        int loadedCount=0;
        chatMemory.clear();
        for(ChatHistory chatHistory:historyList){
            if(ChatHistoryMessageTypeEnum.USER.getValue().equals(chatHistory.getMessageType())){
                chatMemory.add(UserMessage.from(chatHistory.getMessage()));
                loadedCount++;
            } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(chatHistory.getMessageType())) {
                chatMemory.add(UserMessage.from(chatHistory.getMessage()));
                loadedCount++;
            }

        }

         log.info("成功为appId:{},加载了{}条对话",appId,loadedCount);
        return loadedCount;
    }

    @Override
    public long saveMessage(ChatHistorySaveRequest chatHistorySaveRequest, User loginUser) {
        if (chatHistorySaveRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long appId = chatHistorySaveRequest.getAppId();
        String message = chatHistorySaveRequest.getMessage();
        String messageType = chatHistorySaveRequest.getMessageType();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        // 校验消息类型
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "消息类型不合法");
        // 校验应用存在
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 非管理员需校验是否为应用创建者
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (!UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限操作此应用的对话历史");
        }
        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setAppId(appId);
        chatHistory.setMessage(message);
        chatHistory.setMessageType(messageType);
        chatHistory.setUserId(loginUser.getId());
        boolean result = this.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return chatHistory.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        QueryWrapper queryWrapper = QueryWrapper.create().eq("appId", appId);
        return this.remove(queryWrapper);
    }

    @Override
    public ChatHistoryVO getChatHistoryVo(ChatHistory chatHistory) {
        if (chatHistory == null) {
            return null;
        }
        ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
        BeanUtil.copyProperties(chatHistory, chatHistoryVO);
        // 填充用户信息
        Long userId = chatHistory.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            if (user != null) {
                UserVO userVO = userService.getUserVo(user);
                chatHistoryVO.setUser(userVO);
            }
        }
        return chatHistoryVO;
    }

    @Override
    public List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList) {
        if (CollUtil.isEmpty(chatHistoryList)) {
            return new ArrayList<>();
        }
        return chatHistoryList.stream().map(this::getChatHistoryVo).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        if (chatHistoryQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 游标分页：获取早于 lastCreateTime 的记录
        java.time.LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        if (lastCreateTime != null) {
            wrapper.lt("createTime", lastCreateTime);
        }
        wrapper.orderBy(sortField, "ascend".equals(sortOrder));
        return wrapper;
    }

    @Override
    public Page<ChatHistoryVO> listChatHistoryVOByPage(ChatHistoryQueryRequest chatHistoryQueryRequest, User loginUser) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = chatHistoryQueryRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 无效");
        // 校验应用存在
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 非管理员需校验是否为应用创建者
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (!UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限查看此应用的对话历史");
        }
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 每次最多 20 条
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多 20 条");
        // 默认按创建时间倒序（最新在前）
        if (chatHistoryQueryRequest.getSortField() == null) {
            chatHistoryQueryRequest.setSortField("createTime");
            chatHistoryQueryRequest.setSortOrder("descend");
        }
        Page<ChatHistory> chatHistoryPage = this.page(Page.of(pageNum, pageSize), getQueryWrapper(chatHistoryQueryRequest));
        Page<ChatHistoryVO> chatHistoryVOPage = new Page<>(pageNum, pageSize, chatHistoryPage.getTotalRow());
        List<ChatHistoryVO> chatHistoryVOList = getChatHistoryVOList(chatHistoryPage.getRecords());
        chatHistoryVOPage.setRecords(chatHistoryVOList);
        return chatHistoryVOPage;
    }

    @Override
    public Page<ChatHistory> adminListChatHistoryByPage(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 默认按创建时间倒序
        if (chatHistoryQueryRequest.getSortField() == null) {
            chatHistoryQueryRequest.setSortField("createTime");
            chatHistoryQueryRequest.setSortOrder("descend");
        }
        return this.page(Page.of(pageNum, pageSize), getQueryWrapper(chatHistoryQueryRequest));
    }

}
