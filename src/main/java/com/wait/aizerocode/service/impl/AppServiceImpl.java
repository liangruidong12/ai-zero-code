package com.wait.aizerocode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wait.aizerocode.constant.AppConstant;
import com.wait.aizerocode.core.AiCodeGeneratorFacade;
import com.wait.aizerocode.core.builder.VueProjectBuilder;
import com.wait.aizerocode.core.handler.StreamHandlerExecutor;
import com.wait.aizerocode.exception.BusinessException;
import com.wait.aizerocode.exception.ErrorCode;
import com.wait.aizerocode.exception.ThrowUtils;
import com.wait.aizerocode.mapper.AppMapper;
import com.wait.aizerocode.model.dto.app.AppAddRequest;
import com.wait.aizerocode.model.dto.app.AppQueryRequest;
import com.wait.aizerocode.model.dto.app.AppUpdateRequest;
import com.wait.aizerocode.model.dto.chathistory.ChatHistorySaveRequest;
import com.wait.aizerocode.model.entity.App;
import com.wait.aizerocode.model.entity.User;
import com.wait.aizerocode.model.enums.ChatHistoryMessageTypeEnum;
import com.wait.aizerocode.model.enums.CodeGenTypeEnum;
import com.wait.aizerocode.model.vo.AppVO;
import com.wait.aizerocode.model.vo.UserVO;
import com.wait.aizerocode.service.AppService;
import com.wait.aizerocode.service.ChatHistoryService;
import com.wait.aizerocode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.StreamHandler;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author waitwait
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Autowired
    private AppMapper appMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Autowired
    @Lazy
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandler;

    @Resource
    private VueProjectBuilder  vueProjectBuilder;

    @Override
    public String deployApp(Long appId, User loginUser) {
        //1 参数校验
        ThrowUtils.throwIf(appId==null||appId<=0,ErrorCode.PARAMS_ERROR,"应用不存在或无效");
        ThrowUtils.throwIf(loginUser==null,ErrorCode.PARAMS_ERROR,"用户未登录");
        App app = this.getById(appId);
        ThrowUtils.throwIf(app==null,ErrorCode.PARAMS_ERROR,"应用不存在或无效");
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()),ErrorCode.PARAMS_ERROR,"应用不存在或无效");
        //2
        String deployKey = app.getDeployKey();
        if(StrUtil.isBlank(deployKey)){
            deployKey=RandomUtil.randomString(6);
        }

        // 5. 获取代码生成类型，获取原始代码生成路径（应用访问目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查路径是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码路径不存在，请先生成应用");
        }

        //7 vue项目处理
        if(codeGenType.equals(CodeGenTypeEnum.VUE_PROJECT.getValue())){
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess,ErrorCode.SYSTEM_ERROR,"vue代码构建失败");

            File fileDist=new File(sourceDirPath,"dist");
            ThrowUtils.throwIf(!fileDist.exists(),ErrorCode.SYSTEM_ERROR,"vue项目构建成功，但dist目录未生成");

            sourceDir=fileDist;
            log.info("项目构建成功，将部署dist目录:{}",sourceDir,fileDist.getAbsolutePath());


        }


        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败：" + e.getMessage());
        }
        // 9. 更新数据库
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");

        return String.format("%s/%s",AppConstant.CODE_DEPLOY_HOST,deployKey);

    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        //1 校验参数
        if (StrUtil.isBlank(message))
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提示词为空");
        if(appId==null||appId<0)
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"应用Id不能为空");
        //2 查询应用信息
        App app=this.getById(appId);
        if(!app.getUserId().equals(loginUser.getId()))
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无法访问该应用");
        ThrowUtils.throwIf(app.getCodeGenType()==null,ErrorCode.PARAMS_ERROR,"没有生成类型");



        //3调用ai接口
        String codeGenTypeValue = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeValue);

        ChatHistorySaveRequest chatHistorySaveRequest = new ChatHistorySaveRequest();

        chatHistorySaveRequest.setAppId(appId);
        chatHistorySaveRequest.setMessage(message);
        chatHistorySaveRequest.setMessageType(ChatHistoryMessageTypeEnum.USER.getValue());
        //保存首条用户消息。
        chatHistoryService.saveMessage(chatHistorySaveRequest,loginUser);


        Flux<String> fluxStream = aiCodeGeneratorFacade.generateAndSaveCode(message, codeGenTypeEnum, appId);

        return streamHandler.doExecute(fluxStream,chatHistoryService,appId,loginUser,codeGenTypeEnum);



//        return flux.map(chunk->{
//            aiResponseBuilder.append(chunk);
//            return chunk;
//        })
//                .doOnComplete(()-> {
//            String aiResponse=aiResponseBuilder.toString();
//            if(StrUtil.isNotBlank(aiResponse)){
//                chatHistorySaveRequest.setMessage(aiResponse);
//                chatHistoryService.saveMessage(chatHistorySaveRequest,loginUser);
//            }
//
//                })
//                .doOnError(error->{
//                    String errorMessage="AI回复失败"+error.getMessage();
//                    chatHistorySaveRequest.setMessage(errorMessage);
//                    chatHistorySaveRequest.setMessageType(ChatHistoryMessageTypeEnum.AI.getValue());
//                });

    }


    @Override
    public long addApp(AppAddRequest appAddRequest, HttpServletRequest request) {
        if (appAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String initPrompt = appAddRequest.getInitPrompt();
        if (StrUtil.isBlank(initPrompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "初始化提示词不能为空");
        }
        // 获取当前登录用户
        User loginUser = userService.getUser(request);
        // 创建应用
        App app = new App();
        app.setInitPrompt(initPrompt);
        app.setAppName("app_"+RandomUtil.randomString(10));
        app.setUserId(loginUser.getId());
        app.setCodeGenType(CodeGenTypeEnum.VUE_PROJECT.getValue());
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return app.getId();
    }

    @Override
    public boolean updateApp(AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getUser(request);
        App app = this.getById(appUpdateRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验是否为本人应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 仅更新应用名称
        App updateApp = new App();
        updateApp.setId(app.getId());
        updateApp.setAppName(appUpdateRequest.getAppName());
        boolean result = this.updateById(updateApp);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteApp(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getUser(request);
        App app = this.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验是否为本人应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 级联删除该应用的所有对话历史
        chatHistoryService.removeByAppId(id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminDeleteApp(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        App app = this.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 级联删除该应用的所有对话历史
        chatHistoryService.removeByAppId(id);
        return true;
    }

    @Override
    public AppVO getAppVo(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 填充用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            if (user != null) {
                UserVO userVO = userService.getUserVo(user);
                appVO.setUser(userVO);
            }
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        return appList.stream().map(this::getAppVo).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .eq("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

}
