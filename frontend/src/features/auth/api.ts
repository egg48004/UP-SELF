import { request } from "../../shared/http/client";
import type { Channel } from "../../shared/auth/session";
import type { Identity, Login } from "./types";
const prefix = (c: Channel) => (c === "student" ? "/client" : "");
export const authApi = {
  login: (channel: Channel, username: string, password: string) =>
    request<Login>(channel === "student" ? "/client/login" : "/empLogin", {
      method: "POST",
      data: { username, password },
    }),
  info: (c: Channel) => request<Identity>(prefix(c) + "/getInfo"),
  logout: (c: Channel) => request(prefix(c) + "/logout", { method: "POST" }),
  password: (c: Channel, oldPassword: string, newPassword: string) =>
    request(
      c === "student"
        ? "/client/profile/password"
        : "/system/user/profile/updatePwd",
      { method: "PUT", data: { oldPassword, newPassword } },
    ),
};
