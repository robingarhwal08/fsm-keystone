import api from "../api/axiosConfig";
export const login = (data) => api.post("/auth/login", data);
export const signup = (data) => api.post("/auth/signup", data);
export const getMyProfile = () => api.get("/users/me");
export const updateProfile = (data) => api.put("/users/me", data);
