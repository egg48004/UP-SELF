import { data } from "../../shared/http/client";
import type { Badge } from "./types";
export const badgeApi = {
  mine: () => data<Badge[]>("/client/learning/badges"),
};
