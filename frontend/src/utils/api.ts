import axios from "axios";
import { ApplicationUser } from "../utils/ApplicationUser";

// Create an axios instance for backend API calls.
// Important: withCredentials is enabled so browser session cookies (or other
// same-site cookies used by Spring Security) are sent with cross-origin
// requests. The backend must have CORS configured to allow credentials
// (Access-Control-Allow-Credentials: true) and the specific Origin.
const api = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 10000,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

export const fetchTrades = () => api.get("/trades");

export const fetchAllUsers = async () => {
  console.log("Fetching all users from the API");
  return await api.get("/users").then((res) => {
    return res;
  });
};

// Use the existing `ApplicationUser` interface for typing here. We accept a
// partial user payload when creating/updating from the UI, so `Partial<
// ApplicationUser>` is appropriate. This replaces a previous quick `any`
// annotation and keeps the signatures meaningful while still matching the
// shape being passed from pages such as `SignUp` and modals like
// `UserActionsModal`.
export const createUser = (user: Partial<ApplicationUser>) =>
  api.post("/users", user);

export const fetchUserProfiles = () => api.get("/userProfiles");

// Update uses the same `Partial<ApplicationUser>` shape. `id` can be a
// number or string depending on where callers obtain it (forms sometimes
// keep an id as a string). If desired later, tighten `id` to `number` only.
export const updateUser = (
  id: number | string,
  user: Partial<ApplicationUser>
) => api.put(`/users/${id}`, user);

export const authenticate = (user: string, pass: string) => {
  return api.post(`/login/${user}`, null, {
    params: {
      Authorization: pass,
    },
  });
};

export const getUserByLogin = (login: string) => {
  return api.get(`/users/loginId/${login}`);
};

// Dashboard endpoints
export const getDashboardMyTrades = (traderId: string) =>
  api.get(`/dashboard/my-trades`, { params: { traderId } });

export const getDashboardSummary = (traderId: string) =>
  api.get(`/dashboard/summary`, { params: { traderId } });

export const getDashboardDailySummary = (traderId: string) =>
  api.get(`/dashboard/daily-summary`, { params: { traderId } });

export const getTradesByBook = (bookId: number | string) =>
  api.get(`/dashboard/book/${bookId}/trades`);
export default api;
