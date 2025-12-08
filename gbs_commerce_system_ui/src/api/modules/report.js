import http from '../request';

export const getSalesOverview = () => {
  return http('/report/sales/overview', { method: 'GET' });
};

export const getSalesTrend = (params = {}) => {
  return http('/report/sales/trend', { method: 'GET', params });
};

export const getInventoryWarnings = () => {
  return http('/report/inventory/warning', { method: 'GET' });
};
