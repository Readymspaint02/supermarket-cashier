import http from '../request';

export const getMemberPage = (data) => {
  return http('/system/member/page', { method: 'POST', data });
};

export const getMemberList = () => {
  return http('/system/member/list', { method: 'GET' });
};

export const getMemberDetail = (id) => {
  return http(`/system/member/${id}`, { method: 'GET' });
};

export const getMemberByMemberId = (memberId) => {
  return http(`/system/member/byMemberId/${memberId}`, { method: 'GET' });
};

export const createMember = (data) => {
  return http('/system/member/add', { method: 'POST', data });
};

export const updateMember = (id, data) => {
  return http(`/system/member/update/${id}`, { method: 'PUT', data });
};

export const deleteMember = (id) => {
  return http(`/system/member/delete/${id}`, { method: 'DELETE' });
};
