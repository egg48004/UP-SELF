import { data } from "../../shared/http/client";
export type TextSize = "standard" | "large" | "extraLarge";
export type Contrast = "standard" | "highContrast";
export interface ReadingSettings {
  size: TextSize;
  contrast: Contrast;
  motion: boolean;
}
export interface PlatformDefaults extends ReadingSettings {
  version: number;
}
export const accessibilityApi = {
  read: () => data<PlatformDefaults>("/accessibility/defaults"),
  save: (body: PlatformDefaults) =>
    data<PlatformDefaults>("/accessibility/defaults", {
      method: "PUT",
      data: body,
    }),
};
