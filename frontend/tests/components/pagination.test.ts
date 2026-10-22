import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import { createI18n } from "vue-i18n";
import { ElButton } from "element-plus";
import PaginationBar from "../../src/shared/ui/PaginationBar.vue";
import en from "../../src/shared/i18n/en";
describe("pagination interactions", () => {
  it("disables impossible navigation and emits the requested page", async () => {
    const wrapper = mount(PaginationBar, {
      props: { current: 1, total: 21 },
      global: {
        plugins: [
          createI18n({ legacy: false, locale: "en", messages: { en } }),
        ],
        components: { ElButton },
      },
    });
    const buttons = wrapper.findAll("button");
    expect(buttons[0]?.attributes("disabled")).toBeDefined();
    await buttons[1]?.trigger("click");
    expect(wrapper.emitted("change")).toEqual([[2]]);
    await wrapper.setProps({ current: 3 });
    expect(wrapper.findAll("button")[1]?.attributes("disabled")).toBeDefined();
  });
});
