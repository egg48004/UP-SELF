import { beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises, mount } from "@vue/test-utils";
import { createI18n } from "vue-i18n";
import { ElButton } from "element-plus";
import KnowledgeAnswerForm from "../../src/features/knowledge/components/KnowledgeAnswerForm.vue";
import KnowledgeQuestionEditor from "../../src/features/knowledge/components/KnowledgeQuestionEditor.vue";
import MoodHistory from "../../src/features/mood/components/MoodHistory.vue";
import type { KnowledgeQuestion } from "../../src/features/knowledge/types";
import en from "../../src/shared/i18n/en";
const mocks = vi.hoisted(() => ({ answer: vi.fn(), update: vi.fn() }));
vi.mock("../../src/features/knowledge/api", () => ({ knowledgeApi: mocks }));
const global = () => ({
  components: { ElButton },
  plugins: [createI18n({ legacy: false, locale: "en", messages: { en } })],
});
const question: KnowledgeQuestion = {
  id: 1,
  courseId: 2,
  courseTitle: "Small steps",
  courseStatus: "PUBLISHED",
  questionText: "Can you explain this topic?",
  status: "PUBLISHED",
  version: 0,
  choice: null,
};
describe("knowledge answers", () => {
  beforeEach(() => vi.clearAllMocks());
  it("does not preselect an answer and requires an explicit choice", async () => {
    const w = mount(KnowledgeAnswerForm, {
      props: { question },
      global: global(),
    });
    expect(w.findAll("input:checked")).toHaveLength(0);
    expect(w.get("button").attributes("disabled")).toBeDefined();
    expect(w.findAll("label").map((item) => item.text())).toEqual([
      "✓Yes",
      "−No",
      "?Not sure",
    ]);
    await w.get("form").trigger("submit");
    expect(mocks.answer).not.toHaveBeenCalled();
    w.unmount();
  });
  it("retains the selected answer after a failed save", async () => {
    mocks.answer.mockRejectedValue(
      new Error("Connection lost. Please try again."),
    );
    const w = mount(KnowledgeAnswerForm, {
      props: { question },
      global: global(),
    });
    await w.get('input[value="NOT_SURE"]').setValue();
    await w.get("form").trigger("submit");
    await flushPromises();
    expect(mocks.answer).toHaveBeenCalledWith(1, "NOT_SURE", 0);
    expect(
      (w.get('input[value="NOT_SURE"]').element as HTMLInputElement).checked,
    ).toBe(true);
    expect(w.get('[role="alert"]').text()).toContain("Connection lost");
    w.unmount();
  });
  it("saves changes to an existing answer and emits the server result", async () => {
    const result = {
      id: 5,
      questionId: 1,
      studentId: 8,
      choice: "NO",
      updateTime: "2026-10-27T00:00:00Z",
    };
    mocks.answer.mockResolvedValue(result);
    const w = mount(KnowledgeAnswerForm, {
      props: { question: { ...question, choice: "YES" } },
      global: global(),
    });
    expect(w.get("button").attributes("disabled")).toBeDefined();
    await w.get('input[value="NO"]').setValue();
    await w.get("form").trigger("submit");
    await flushPromises();
    expect(w.emitted("saved")?.[0]).toEqual([result]);
    expect(w.get('[role="status"]').text()).toBe("Answer saved.");
    w.unmount();
  });
  it("locks the wording of answered questions while allowing archive", async () => {
    mocks.update.mockResolvedValue({ code: 200 });
    const w = mount(KnowledgeQuestionEditor, {
      props: { question: { ...question, answerCount: 1 } },
      global: global(),
    });
    expect(w.get("textarea").attributes("readonly")).toBeDefined();
    await w.get("select").setValue("ARCHIVED");
    await w.get("form").trigger("submit");
    await flushPromises();
    expect(mocks.update).toHaveBeenCalledWith(1, {
      questionText: question.questionText,
      status: "ARCHIVED",
      version: 0,
    });
    w.unmount();
  });
});
describe("diary visibility labels", () => {
  it("labels both earlier and new entries as shared diaries", () => {
    const w = mount(MoodHistory, {
      props: {
        rows: [
          {
            id: 1,
            studentId: 2,
            weekStart: "2026-10-19",
            moodLevel: 3,
            privateNote: "Older private text",
            noteShared: false,
            createTime: "",
          },
          {
            id: 2,
            studentId: 2,
            weekStart: "2026-10-26",
            moodLevel: 4,
            privateNote: "Shared diary text",
            noteShared: true,
            createTime: "",
          },
        ],
        current: 1,
        total: 2,
        loading: false,
        error: "",
        showNotes: true,
      },
      global: global(),
    });
    expect(w.findAll("summary").map((item) => item.text())).toEqual([
      "Diary",
      "Diary",
    ]);
    w.unmount();
  });
  it("shows only shared diary fields returned to mentors", () => {
    const w = mount(MoodHistory, {
      props: {
        rows: [
          { weekStart: "2026-10-19", moodLevel: 3, diaryNote: null },
          {
            weekStart: "2026-10-26",
            moodLevel: 4,
            diaryNote: "Shared diary text",
          },
        ],
        current: 1,
        total: 2,
        loading: false,
        error: "",
        showNotes: true,
      },
      global: global(),
    });
    expect(w.findAll("summary")).toHaveLength(1);
    expect(w.text()).toContain("Shared diary text");
    expect(w.text()).not.toContain("Earlier private note");
    w.unmount();
  });
});
