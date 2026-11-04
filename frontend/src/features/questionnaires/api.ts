import { data, request, table } from "../../shared/http/client";
import type {
  Questionnaire,
  StudentQuestionnaire,
  QuestionnaireInput,
} from "./types";
const base = "/questionnaires";
export const questionnaireApi = {
  visible: (current: number) =>
    table<StudentQuestionnaire>("/client/questionnaires", {
      params: { current, pageSize: 10 },
    }),
  manage: (current: number) =>
    table<Questionnaire>(base + "/manage", {
      params: { current, pageSize: 10 },
    }),
  create: (body: QuestionnaireInput) =>
    data<number>(base, { method: "POST", data: body }),
  update: (id: number, body: QuestionnaireInput, version: number) =>
    request(base + "/" + id, { method: "PUT", data: { ...body, version } }),
  visibility: (id: number, visible: boolean, version: number) =>
    request(base + "/" + id + "/visibility", {
      method: "PUT",
      data: { visible, version },
    }),
  remove: (id: number, version: number) =>
    request(base + "/" + id, { method: "DELETE", params: { version } }),
};
