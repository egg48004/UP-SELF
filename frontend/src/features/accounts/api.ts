import { data, request, table } from "../../shared/http/client";
import type { User } from "../auth/types";
export interface Candidate {
  id: number;
  userName: string;
  nickName: string;
}
export interface AccountBody {
  userName: string;
  nickName: string;
  password: string;
  roleId: number;
}
export const accountApi = {
  list: (current: number, userName: string) =>
    table<User>("/system/user/list", {
      params: { current, pageSize: 10, userName },
    }),
  candidates: (role: "student" | "mentor", current: number, keyword: string) =>
    table<Candidate>("/system/user/candidates", {
      params: { role, current, pageSize: 10, keyword },
    }),
  create: (body: AccountBody) =>
    data<number>("/system/user", { method: "POST", data: body }),
  update: (user: User) =>
    request("/system/user", {
      method: "PUT",
      data: {
        id: user.id,
        nickName: user.nickName,
        roleId: user.roleId,
        status: user.status,
      },
    }),
  reset: (id: number, password: string) =>
    request("/system/user/resetPwd", { method: "PUT", data: { id, password } }),
  resetTotp: (id: number) =>
    request("/system/user/" + id + "/resetTotp", { method: "PUT" }),
};
