import { createApp } from "vue";
import { createPinia } from "pinia";
import { ElButton, ElDialog, ElConfigProvider } from "element-plus";
import "element-plus/theme-chalk/base.css";
import "element-plus/theme-chalk/el-button.css";
import "element-plus/theme-chalk/el-dialog.css";
import "element-plus/theme-chalk/el-overlay.css";
import "element-plus/theme-chalk/el-message-box.css";
import App from "./App.vue";
import { router } from "./app/router";
import { i18n } from "./shared/i18n";
import { onUnauthorized } from "./shared/http/client";
import { useAuthStore } from "./features/auth/store";
import "./shared/styles/base.css";
const app = createApp(App);
app.use(createPinia());
app.use(i18n);
app.component("ElButton", ElButton);
app.component("ElDialog", ElDialog);
app.component("ElConfigProvider", ElConfigProvider);
app.use(router);
let redirecting = false;
onUnauthorized(() => {
  useAuthStore().clear();
  if (!redirecting) {
    redirecting = true;
    void router.replace("/login").finally(() => {
      redirecting = false;
    });
  }
});
document.addEventListener(
  "invalid",
  (e) => {
    const field = e.target;
    if (
      field instanceof HTMLInputElement ||
      field instanceof HTMLTextAreaElement ||
      field instanceof HTMLSelectElement
    )
      field.setCustomValidity(i18n.global.t(field.validity.valueMissing ? "required" : "invalidValue"));
  },
  true,
);
document.addEventListener("input", (e) => {
  const field = e.target;
  if (
    field instanceof HTMLInputElement ||
    field instanceof HTMLTextAreaElement ||
    field instanceof HTMLSelectElement
  )
    field.setCustomValidity("");
});
document.addEventListener("change", (e) => {
  const field = e.target;
  if (
    field instanceof HTMLInputElement ||
    field instanceof HTMLTextAreaElement ||
    field instanceof HTMLSelectElement
  )
    field.setCustomValidity("");
});
app.mount("#app");
