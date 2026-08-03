import api from "../api/axiosConfig";

export const login = (data) =>api.post("/auth/login", data);
export const signup = (data) => api.post("/auth/signup", data);

export const dashboardSummary = () => api.get("/dashboard/summary");
export const getCustomers = () => api.get("/customers");
export const createCustomer = (data) => api.post("/customers", data);
export const getSites = () => api.get("/sites");
export const createSite = (data) => api.post("/sites", data);

export const getTechnicians = () => api.get("/users/technicians");
export const getParts = () => api.get("/parts");
export const createPart = (data) => api.post("/parts", data);
export const getWorkOrders = () => api.get("/work-orders");
export const createWorkOrder = (data) => api.post("/work-orders", data);
export const updateWorkOrderStatus = (id, data) => api.patch(`/work-orders/${id}/status`, data);
export const updateCustomer = (id, data) => api.put(`/customers/${id}`, data);
export const deleteCustomer = (id) =>
    api.delete(`/customers/${id}`);
export const updateSite = (id, data) =>
  api.put(`/sites/${id}`, data);

export const deleteSite = (id) =>
  api.delete(`/sites/${id}`);

 export const updateWorkOrder = (id, data) =>
   api.put(`/work-orders/${id}`, data);

 export const deleteWorkOrder = (id) =>
   api.delete(`/work-orders/${id}`);

 export const createTimeLog = (data) =>
     api.post("/time-logs", data);

 export const createPartUsage = (data) =>
     api.post("/part-usage", data);

 export const updatePart = (id, data) =>
     api.put(`/parts/${id}`, data);

 export const deletePart = (id) =>
     api.delete(`/parts/${id}`);

export const getUsers = () =>
    api.get("/users");

export const updateUser = (id, data) =>
    api.put(`/users/${id}`, data);

export const deleteUser = (id) =>
    api.delete(`/users/${id}`);

export const assignTechnician = (id, data) =>
    api.patch(`/work-orders/${id}/assign`, data);
