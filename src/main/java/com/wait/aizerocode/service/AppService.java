package com.wait.aizerocode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.wait.aizerocode.model.dto.app.AppAddRequest;
import com.wait.aizerocode.model.dto.app.AppQueryRequest;
import com.wait.aizerocode.model.dto.app.AppUpdateRequest;
import com.wait.aizerocode.model.entity.App;
import com.wait.aizerocode.model.entity.User;
import com.wait.aizerocode.model.vo.AppVO;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author waitwait
 */
public interface AppService extends IService<App> {



    String deployApp(Long appId,User loginUser);

    /**
     * 创建应用
     *
     * @param appAddRequest 创建请求
     * @param request       请求
     * @return 应用 id
     */
    long addApp(AppAddRequest appAddRequest, HttpServletRequest request);

    /**
     * 用户修改自己的应用（仅应用名称）
     *
     * @param appUpdateRequest 更新请求
     * @param request          请求
     * @return 是否更新成功
     */
    boolean updateApp(AppUpdateRequest appUpdateRequest, HttpServletRequest request);

    /**
     * 用户删除自己的应用
     *
     * @param id      应用 id
     * @param request 请求
     * @return 是否删除成功
     */
    boolean deleteApp(Long id, HttpServletRequest request);

    /**
     * 管理员删除任意应用（级联删除对话历史）
     *
     * @param id 应用 id
     * @return 是否删除成功
     */
    boolean adminDeleteApp(Long id);

    /**
     * 获取应用封装类
     *
     * @param app 应用
     * @return 应用封装类
     */
    AppVO getAppVo(App app);

    /**
     * 获取应用封装列表
     *
     * @param appList 应用列表
     * @return 应用封装列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 获取查询条件
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);


    Flux<String> chatToGenCode(Long appId, String message, User login);

}
