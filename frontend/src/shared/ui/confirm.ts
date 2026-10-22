import { ElMessageBox } from "element-plus";
import { i18n } from "../i18n";
export async function confirmAction(message: string) {
  try {
    await ElMessageBox.confirm(message, i18n.global.t("confirmTitle"), {
      confirmButtonText: i18n.global.t("confirm"),
      cancelButtonText: i18n.global.t("cancel"),
      type: "warning",
      closeOnClickModal: false,
    });
    return true;
  } catch {
    return false;
  }
}
