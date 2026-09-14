declare namespace API {
  type adminGetAppByIdParams = {
    // 后端 Long 统一序列化为字符串，避免 JS number 精度丢失
    id: string
  }

  type App = {
    id?: string
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type AppAddRequest = {
    initPrompt?: string
  }

  type AppAdminUpdateRequest = {
    id?: string
    appName?: string
    cover?: string
    priority?: number
  }

  type AppDeployRequest = {
    appId?: string
  }

  type AppQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    priority?: number
    userId?: string
  }

  type AppUpdateRequest = {
    id?: string
    appName?: string
  }

  type AppVO = {
    id?: string
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: string
    createTime?: string
    updateTime?: string
    user?: UserVO
  }

  type BaseResponseApp = {
    code?: number
    data?: App
    message?: string
  }

  type BaseResponseAppVO = {
    code?: number
    data?: AppVO
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    // 后端 Long 统一序列化为字符串，避免 JS number 精度丢失
    data?: string
    message?: string
  }

  type BaseResponsePageApp = {
    code?: number
    data?: PageApp
    message?: string
  }

  type BaseResponsePageAppVO = {
    code?: number
    data?: PageAppVO
    message?: string
  }

  type BaseResponsePageChatHistory = {
    code?: number
    data?: PageChatHistory
    message?: string
  }

  type BaseResponsePageChatHistoryVO = {
    code?: number
    data?: PageChatHistoryVO
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type ChatHistory = {
    id?: string
    message?: string
    messageType?: string
    appId?: string
    userId?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type ChatHistoryQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    message?: string
    messageType?: string
    appId?: string
    userId?: string
    // 游标：查询早于该创建时间的消息
    lastCreateTime?: string
  }

  type ChatHistorySaveRequest = {
    appId?: string
    message?: string
    messageType?: string
  }

  type ChatHistoryVO = {
    id?: string
    message?: string
    messageType?: string
    appId?: string
    userId?: string
    createTime?: string
    updateTime?: string
    user?: UserVO
  }

  type chatToGenCodeParams = {
    appId: string
    message: string
  }

  type DeleteRequest = {
    id?: string
  }

  type getAppVOByIdParams = {
    id: string
  }

  type getUserByIdParams = {
    id: string
  }

  type getUserVOByIdParams = {
    id: string
  }

  type LoginUserVO = {
    id?: string
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }

  type PageApp = {
    records?: App[]
    // mybatis-flex Page 中分页字段为 long，后端统一序列化为字符串
    pageNumber?: string
    pageSize?: string
    totalPage?: string
    totalRow?: string
    optimizeCountQuery?: boolean
  }

  type PageAppVO = {
    records?: AppVO[]
    pageNumber?: string
    pageSize?: string
    totalPage?: string
    totalRow?: string
    optimizeCountQuery?: boolean
  }

  type PageChatHistory = {
    records?: ChatHistory[]
    pageNumber?: string
    pageSize?: string
    totalPage?: string
    totalRow?: string
    optimizeCountQuery?: boolean
  }

  type PageChatHistoryVO = {
    records?: ChatHistoryVO[]
    pageNumber?: string
    pageSize?: string
    totalPage?: string
    totalRow?: string
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: string
    pageSize?: string
    totalPage?: string
    totalRow?: string
    optimizeCountQuery?: boolean
  }

  type ServerSentEventString = true

  type User = {
    id?: string
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }

  type UserUpdateRequest = {
    id?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserVO = {
    id?: string
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }
}
