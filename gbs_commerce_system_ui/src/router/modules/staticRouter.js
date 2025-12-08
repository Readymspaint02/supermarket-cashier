/**
 * staticRouter (静态路由)
 */
export const staticRouter = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
  },
  {
    path: '/layout',
    name: 'layout',
    component: () => import('@/components/Layout.vue'),
    redirect: '/dashboard',
    children: [],
  },
];

/**
 * errorRouter (错误页面路由)
 */
export const errorRouter = [
  {
    path: '/404',
    name: '404',
    component: () => import('@/components/ErrorMessage/404.vue'),
    meta: {
      title: '404页面',
    },
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/components/ErrorMessage/404.vue'),
  },
];
