import api from "../api/axiosConfig";
export const login = (data) => api.post("/auth/login", data);
export const signup = (data) => api.post("/auth/signup", data);
