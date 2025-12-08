import http from "../request";

export const login = async data => {
    return http('/auth/login', { method: 'POST', data });
};