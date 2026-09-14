package com.wait.aizerocode.controller;

import com.mybatisflex.core.paginate.Page;
import com.wait.aizerocode.annotation.AuthCheck;
import com.wait.aizerocode.common.BaseResponse;
import com.wait.aizerocode.common.ResultUtils;
import com.wait.aizerocode.constant.UserConstant;
import com.wait.aizerocode.exception.ErrorCode;
import com.wait.aizerocode.exception.ThrowUtils;
import com.wait.aizerocode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.wait.aizerocode.model.dto.chathistory.ChatHistorySaveRequest;
import com.wait.aizerocode.model.entity.ChatHistory;
import com.wait.aizerocode.model.entity.User;
import com.wait.aizerocode.model.vo.ChatHistoryVO;
import com.wait.aizerocode.service.ChatHistoryService;
import com.wait.aizerocode.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话历史 控制层。
 *
 * @author waitwait
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private UserService userService;

    // ==================== 用户端接口 ====================

    /**
     * 保存一条对话消息（用户消息或 AI 消息）
     * 支持保存用户发送的消息、AI 成功回复的消息、AI 回复失败的错误信息
     *
     * @param chatHistorySaveRequest 保存请求
     * @param request                请求
     * @return 对话历史 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChatHistory(@RequestBody ChatHistorySaveRequest chatHistorySaveRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(chatHistorySaveRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getUser(request);
        long chatHistoryId = chatHistoryService.saveMessage(chatHistorySaveRequest, loginUser);
        return ResultUtils.success(chatHistoryId);
    }

    /**
     * 分页查询应用的对话历史（游标分页，每次最多 20 条，仅应用创建者和管理员可见）
     * 前端首次加载：不传 lastCreateTime，获取最新 10 条
     * 向前加载更多：传 lastCreateTime = 已加载最旧消息的 createTime，获取更早的记录
     *
     * @param chatHistoryQueryRequest 查询请求
     * @param request                 请求
     * @return 对话历史分页列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<ChatHistoryVO>> listChatHistoryVOByPage(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getUser(request);
        Page<ChatHistoryVO> chatHistoryVOPage = chatHistoryService.listChatHistoryVOByPage(chatHistoryQueryRequest, loginUser);
        return ResultUtils.success(chatHistoryVOPage);
    }

    // ==================== 管理员端接口 ====================

    /**
     * 管理员分页查询所有应用的对话历史（按时间降序，便于内容监管）
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页列表
     */
    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> adminListChatHistoryByPage(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<ChatHistory> chatHistoryPage = chatHistoryService.adminListChatHistoryByPage(chatHistoryQueryRequest);
        return ResultUtils.success(chatHistoryPage);
    }

}
