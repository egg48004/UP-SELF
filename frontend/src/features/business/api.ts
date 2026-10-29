import { data, request, table } from "../../shared/http/client";
import type { BusinessIdea, IdeaInput, CostInput, CostResult } from "./types";
const base = "/client/business";
export const ideaApi = {
  list: (current: number) =>
    table<BusinessIdea>(base + "/ideas", { params: { current, pageSize: 10 } }),
  detail: (id: number) => data<BusinessIdea>(base + "/ideas/" + id),
  create: (body: IdeaInput) =>
    data<number>(base + "/ideas", { method: "POST", data: body }),
  edit: (id: number, body: IdeaInput, version: number) =>
    request(base + "/ideas/" + id, {
      method: "PUT",
      data: { ...body, version },
    }),
};
export const costApi = {
  current: () => data<CostResult>(base + "/cost-plan"),
  calculate: (body: CostInput) =>
    data<CostResult>(base + "/cost-plan/calculate", {
      method: "POST",
      data: body,
    }),
  save: (body: CostInput, version: number) =>
    data<CostResult>(base + "/cost-plan", {
      method: "PUT",
      data: { ...body, version },
    }),
};
