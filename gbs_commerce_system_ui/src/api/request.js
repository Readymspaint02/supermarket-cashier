import axios from 'axios';
import router from '@/router/index';
import { ElMessage } from 'element-plus';

const http = axios.create({
    baseURL: '/api',
});
// 添加请求拦截器
http.interceptors.request.use(function (config) {
    // 在发送请求之前做些什么
    // 优先使用配置中的token，否则从localStorage获取
    const token = config.headers.Authorization || localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = 'Bearer ' + token;
    }
    return config;
}, function (error) {
    // 对请求错误做些什么
    return Promise.reject(error);
});

// 添加响应拦截器
http.interceptors.response.use(function (response) {
    const { data } = response;

    // 登录失效
    if (data.code == 401) {
        localStorage.removeItem('token');
        router.replace("/login");
        ElMessage.error(data.msg);
        return Promise.reject(data);
    }
    // 全局错误信息拦截（防止下载文件的时候返回数据流，没有 code 直接报错）
    if (data.code && data.code !== 200) {
        ElMessage.error(data.msg);
        return Promise.reject(data);
    }
    // 成功请求（在页面上除非特殊情况，否则不用处理失败逻辑）
    // console.log('响应成功', data);
    return data;
}, function (error) {
    // 对响应错误做点什么
    const { data } = error.response;
    if (data && data.code == 403) {
        localStorage.removeItem('token');
        localStorage.removeItem('authMenuList');
        localStorage.removeItem('userInfo');
        ElMessage.error(data.msg);
    }
    return Promise.reject(error);
});
export default http;