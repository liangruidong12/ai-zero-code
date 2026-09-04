import router from './router'
import { useLoginUserStore } from './stores/loginUser.ts'
import { message } from 'ant-design-vue'

// 不需要登录即可访问的公开页面
const whitePathList = ['/user/login', '/user/register']

// 标记是否已初始化获取过登录用户
let hasFetchLoginUser = false

/**
 * 全局路由权限校验
 * - 公开页面（登录/注册）：直接放行
 * - 已登录用户访问登录页：跳回首页
 * - 需要权限的页面：未登录跳登录页，无权限跳首页
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  const loginUser = loginUserStore.loginUser

  // 首次进入时尝试获取登录用户信息
  if (!hasFetchLoginUser) {
    hasFetchLoginUser = true
    await loginUserStore.fetchLoginUser()
  }

  const isLoggedIn = !!loginUser.id

  // 公开页面直接放行
  if (whitePathList.includes(to.path)) {
    // 已登录用户访问登录/注册页，跳回首页
    if (isLoggedIn) {
      next('/')
      return
    }
    next()
    return
  }

  // 管理员页面：需要登录 + 管理员角色
  if (to.path.startsWith('/admin')) {
    if (!isLoggedIn) {
      message.warning('请先登录')
      next(`/user/login?redirect=${encodeURIComponent(to.fullPath)}`)
      return
    }
    if (loginUser.userRole !== 'admin') {
      message.error('无权限访问')
      next('/')
      return
    }
  }

  next()
})
