import router from "../index";
import { ElNotification } from "element-plus";
import { getAuthMenuList } from "@/api/modules/menu";

// 引入 views 文件夹下所有 vue 文件
const modules = import.meta.glob("@/views/**/*.vue");

/**
 * @description 初始化动态路由
 */
export const initDynamicRouter = async () => {
  try {
    await getAuthMenuList();
    let authMenuList = localStorage.getItem("authMenuList")
    // 2.判断当前用户有没有菜单权限
    if (authMenuList) {
      authMenuList = JSON.parse(authMenuList)
      console.log(authMenuList)
      if (!authMenuList.length) {
        ElNotification({
          title: "无权限访问",
          message: "当前账号无任何菜单权限，请联系系统管理员！",
          type: "warning",
          duration: 3000
        });
        localStorage.removeItem("token");
        router.replace('/login');
        return Promise.reject("No permission");
      }
    }

    // 3.添加动态路由
    // 3.添加动态路由（包含子路由）
    const convertToRoute = (item) => {
      // 转换后端字段到前端路由需要的字段
      const routeItem = {
        ...item,
        name: item.menuName,
        path: item.path,
      };

      // 处理组件
      if (routeItem.component && typeof routeItem.component == "string") {
        routeItem.component = modules["/src/views/" + routeItem.component + ".vue"];
      }

      // 递归处理子路由
      if (routeItem.children && routeItem.children.length > 0) {
        routeItem.children = routeItem.children.map(convertToRoute);
      }

      return routeItem;
    };

    authMenuList.forEach(item => {
      const routeItem = convertToRoute(item);
      router.addRoute("layout", routeItem);
    });

    // 获取已添加的路由
    console.log(router.getRoutes());
  } catch (error) {
    // 当按钮 || 菜单请求出错时，重定向到登陆页
    localStorage.removeItem("token");
    router.replace('/login');
    return Promise.reject(error);
  }
};