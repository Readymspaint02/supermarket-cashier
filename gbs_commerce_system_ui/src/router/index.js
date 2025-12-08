import { createRouter, createWebHistory } from 'vue-router';
import { staticRouter, errorRouter } from './modules/staticRouter';
import { initDynamicRouter } from './modules/dynamicRouter';

const router = createRouter({
  history: createWebHistory(),
  routes: [...staticRouter, ...errorRouter],
});

/**
 * @description 路由拦截 beforeEach
 * */
router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token');
  // 判断是访问登陆页，有 Token 就在当前页面，没有 Token 重置路由到登陆页
  if (to.path === '/login') {
    if (token) return next(from.fullPath);
    return next();
  }

  // 判断是否有 Token，没有重定向到 login 页面
  if (!token) return next({ path: '/login', replace: true });
  // 确保动态路由已加载
  const authMenuList = JSON.parse(localStorage.getItem('authMenuList') || '[]');
  if (authMenuList.length > 0) {
    // 检查是否已经添加了动态路由
    const dynamicRouteAdded = router
      .getRoutes()
      .some(route => authMenuList.some(menu => menu.path === route.path));

    if (!dynamicRouteAdded) {
      await initDynamicRouter();
      return next({ ...to, replace: true });
    }
  } else {
    await initDynamicRouter();
    return next({ ...to, replace: true });
  }
  // 正常访问页面
  next();
});

/**
 * @description 路由跳转错误
 * */
router.onError(error => {
  console.warn('路由错误', error.message);
});

export default router;
