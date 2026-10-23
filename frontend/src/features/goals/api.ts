import { data, request, table } from "../../shared/http/client";
import type { GoalFilter, GoalInput, PersonalGoal } from "./types";

const base = "/client/goals";
export const goalApi = {
  list: (current: number, status: GoalFilter) =>
    table<PersonalGoal>(base, { params: { current, pageSize: 10, status } }),
  create: (body: GoalInput) =>
    data<number>(base, { method: "POST", data: body }),
  edit: (id: number, body: GoalInput, version: number) =>
    request(`${base}/${id}`, { method: "PUT", data: { ...body, version } }),
  complete: (id: number, completed: boolean, version: number) =>
    request(`${base}/${id}/completion`, {
      method: "PUT",
      data: { completed, version },
    }),
  remove: (id: number, version: number) =>
    request(`${base}/${id}`, { method: "DELETE", params: { version } }),
};
