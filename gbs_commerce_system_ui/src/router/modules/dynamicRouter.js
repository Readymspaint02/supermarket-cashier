import router from "../index";
import { ElNotification } from "element-plus";
import { getAuthMenuList } from "@/api/modules/menu";

const modules = import.meta.glob("@/views/**/*.vue");

export const initDynamicRouter = async () => {
  try {
    await getAuthMenuList();
    let authMenuList = localStorage.getItem("authMenuList")
    if (authMenuList) {
      authMenuList = JSON.parse(authMenuList)
      console.log('菜单列表:', authMenuList)
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

    const convertToRoute = (item) => {
      const routeItem = {
        name: item.path.replace(/\//g, '_') || `route_${item.id}`,
        path: item.path,
        meta: {
          title: item.menuName,
          icon: item.icon
        }
      };

      if (item.component && typeof item.component === "string") {
        const compPath = item.component.startsWith('views/') 
          ? `/src/${item.component}.vue` 
          : `/src/views/${item.component}.vue`;
        routeItem.component = modules[compPath];
      }

      if (item.children && item.children.length > 0) {
        routeItem.children = item.children.map(convertToRoute);
      }

      return routeItem;
    };

    authMenuList.forEach(item => {
      const routeItem = convertToRoute(item);
      if (routeItem.component || (routeItem.children && routeItem.children.length > 0)) {
        router.addRoute("layout", routeItem);
      }
    });

    console.log('已注册路由:', router.getRoutes().map(r => r.path));
  } catch (error) {
    console.error('路由初始化失败:', error);
    localStorage.removeItem("token");
    router.replace('/login');
    return Promise.reject(error);
  }
};
