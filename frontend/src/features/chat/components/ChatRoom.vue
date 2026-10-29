<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from "vue";
import { onBeforeRouteLeave } from "vue-router";
import { useI18n } from "vue-i18n";
import type { ChatApi } from "../api";
import ReportDialog from "../../reports/components/ReportDialog.vue";
import { useAuthStore } from "../../auth/store";
import type { ChatMessage } from "../types";
import type { ChatGroup } from "../types";
import { useDiscussion, mergeMessages } from "../composables/useDiscussion";
import AudioRecorder from "../../recordings/components/AudioRecorder.vue";
import ChatAttachment from "./ChatAttachment.vue";
import ChatMembers from "./ChatMembers.vue";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import { confirmAction } from "../../../shared/ui/confirm";
const props = defineProps<{ id: number; api: ChatApi; manager: boolean }>();
const emit = defineEmits<{
  changed: [];
  dirty: [value: boolean];
  updated: [group: ChatGroup];
}>();
const { t } = useI18n();
const auth = useAuthStore(),
  reportMessage = ref<ChatMessage>();
const room = useDiscussion(props.api, props.id),
  { group, rows, drafts, loading, error, connection, hasEarlier, denied } =
    room;
const text = ref(""),
  recording = ref<File | null>(null),
  active = ref(false),
  selectedFile = ref<File | null>(null),
  alt = ref(""),
  busy = ref(""),
  notice = ref(""),
  membersOpen = ref(false),
  editing = ref(false),
  edit = ref<ChatGroup>(),
  list = ref<HTMLElement | null>(null),
  newMessages = ref(false),
  imageInput = ref<HTMLInputElement | null>(null);
let requestKey = crypto.randomUUID(),
  lastPayload = "",
  disposed = false;
const pending = computed(
  () =>
    !!text.value.trim() ||
    !!recording.value ||
    active.value ||
    !!selectedFile.value,
);
const closed = computed(
  () => !group.value || group.value.status === "CLOSED" || denied.value,
);
watch(pending, (value) => emit("dirty", value));
watch(
  () => group.value?.version,
  () => {
    if (group.value) emit("updated", { ...group.value });
  },
);
function atBottom() {
  const el = list.value;
  return !el || el.scrollHeight - el.scrollTop - el.clientHeight < 80;
}
async function bottom() {
  await nextTick();
  if (list.value) list.value.scrollTop = list.value.scrollHeight;
  newMessages.value = false;
}
watch(
  () => rows.value.at(-1)?.id,
  async (now, old) => {
    if (now === old) return;
    const follow = !old || atBottom();
    await nextTick();
    if (follow) await bottom();
    else newMessages.value = true;
  },
);
watch(denied, (value) => {
  if (value) {
    reportMessage.value = undefined;
    recording.value = null;
    selectedFile.value = null;
    membersOpen.value = false;
    editing.value = false;
  }
});
function fileChanged(event: Event) {
  selectedFile.value = (event.target as HTMLInputElement).files?.[0] || null;
  alt.value = "";
  (event.target as HTMLInputElement).value = "";
}
const imageSelected = computed(
  () => !!selectedFile.value && /\.(png|jpe?g)$/i.test(selectedFile.value.name),
);
async function upload(file: File | null) {
  if (!file || busy.value) return;
  error.value = "";
  notice.value = "";
  const audio = /\.(webm|m4a|mp4|ogg|wav)$/i.test(file.name);
  if (
    !/\.(pdf|docx|txt|png|jpe?g|webm|m4a|mp4|ogg|wav)$/i.test(file.name) ||
    !file.size ||
    file.size > (audio ? 5 : 20) * 1024 * 1024
  ) {
    error.value = t("chat.invalidFile");
    return;
  }
  if (drafts.value.length >= 5) {
    error.value = t("chat.tooMany");
    return;
  }
  if (/\.(png|jpe?g)$/i.test(file.name) && !alt.value.trim()) {
    error.value = t("chat.imageHelp");
    return;
  }
  busy.value = "upload";
  try {
    const f = await props.api.upload(props.id, file, alt.value);
    if (disposed) return;
    drafts.value = [...drafts.value, f];
    if (file === recording.value) recording.value = null;
    if (file === selectedFile.value) selectedFile.value = null;
    alt.value = "";
  } catch (e) {
    if (!disposed) error.value = (e as Error).message;
  } finally {
    if (!disposed) busy.value = "";
  }
}
async function removeFile(id: number) {
  busy.value = "remove";
  error.value = "";
  try {
    await props.api.removeFile(id);
    if (!disposed) drafts.value = drafts.value.filter((f) => f.id !== id);
  } catch (e) {
    if (!disposed) error.value = (e as Error).message;
  } finally {
    if (!disposed) busy.value = "";
  }
}
async function send() {
  if (busy.value || closed.value) return;
  error.value = "";
  notice.value = "";
  if (recording.value || active.value || selectedFile.value) {
    error.value = t("chat.uploadFirst");
    return;
  }
  const payload = JSON.stringify([
    text.value.trim(),
    drafts.value.map((f) => f.id),
  ]);
  if (payload !== lastPayload) {
    requestKey = crypto.randomUUID();
    lastPayload = payload;
  }
  busy.value = "send";
  try {
    const m = await props.api.send(
      props.id,
      text.value,
      drafts.value.map((f) => f.id),
      requestKey,
    );
    if (disposed) return;
    rows.value = mergeMessages(rows.value, [m]);
    text.value = "";
    drafts.value = [];
    lastPayload = "";
    notice.value = t("chat.sent");
    await bottom();
  } catch (e) {
    if (!disposed)
      error.value = (e as Error).message + " " + t("chat.retrySend");
  } finally {
    if (!disposed) busy.value = "";
  }
}
async function earlier() {
  const el = list.value,
    previous = el?.scrollHeight || 0;
  await room.earlier();
  await nextTick();
  if (el) el.scrollTop = el.scrollHeight - previous;
}
async function saveGroup() {
  if (!edit.value) return;
  busy.value = "group";
  error.value = "";
  try {
    await props.api.update(edit.value);
    if (disposed) return;
    editing.value = false;
    await room.load();
    emit("changed");
  } catch (e) {
    if (!disposed) error.value = (e as Error).message;
  } finally {
    if (!disposed) busy.value = "";
  }
}
onBeforeRouteLeave(
  async () => !pending.value || (await confirmAction(t("chat.leave"))),
);
function beforeUnload(event: BeforeUnloadEvent) {
  if (pending.value) {
    event.preventDefault();
    event.returnValue = "";
  }
}
window.addEventListener("beforeunload", beforeUnload);
onBeforeUnmount(() => {
  disposed = true;
  window.removeEventListener("beforeunload", beforeUnload);
  emit("dirty", false);
});
</script>
<template>
  <section class="panel chat-room" aria-label="Discussion messages">
    <ReportDialog
      v-if="reportMessage"
      :message="reportMessage"
      @close="reportMessage = undefined"
      @submitted="
        reportMessage = undefined;
        notice = t('reports.submitted');
      "
    />
    <AsyncState
      :loading="loading"
      :error="denied ? error : ''"
      @retry="room.load"
    >
      <template v-if="group && !denied">
        <div class="page-heading">
          <h2>{{ group.title }}</h2>
          <div class="button-row">
            <el-button @click="membersOpen = true">
              {{ t("chat.members") }}
            </el-button><el-button
              v-if="manager"
              @click="
                edit = { ...group };
                editing = true;
              "
            >
              {{ t("chat.edit") }}
            </el-button><el-button :disabled="!!busy" @click="room.poll">
              {{ t("chat.refresh") }}
            </el-button>
          </div>
        </div>
        <p v-if="closed" role="status">
          {{ t("chat.closedHelp") }}
        </p>
        <p v-if="connection" role="status">
          {{ t("chat.connection") }}
        </p>
        <div
          ref="list"
          class="message-list"
          tabindex="0"
          aria-label="Messages"
          @scroll="atBottom() && (newMessages = false)"
        >
          <el-button v-if="hasEarlier" @click="earlier">
            {{ t("chat.earlier") }}
          </el-button>
          <p v-if="!rows.length">
            {{ t("chat.emptyMessages") }}
          </p>
          <ol>
            <li v-for="m in rows" :key="m.id" class="message">
              <div class="message-heading">
                <strong>{{ m.senderName }}</strong><time :datetime="m.createTime">{{
                  new Date(m.createTime).toLocaleString("en-MY")
                }}</time>
                <details
                  v-if="m.senderId !== auth.user?.id"
                  class="message-options"
                >
                  <summary
                    :aria-label="t('reports.actions', { name: m.senderName })"
                  >
                    ⋯
                  </summary>
                  <button type="button" @click="reportMessage = m">
                    {{ t("reports.report") }}
                  </button>
                </details>
              </div>
              <p v-if="m.body" class="plain-text">
                {{ m.body }}
              </p>
              <ChatAttachment
                v-for="f in m.files"
                :key="f.id"
                :file="f"
                :api="api"
              />
            </li>
          </ol>
        </div>
        <el-button v-if="newMessages" @click="bottom">
          {{ t("chat.newMessages") }} ↓
        </el-button>
        <p class="sr-only" role="status">
          {{ newMessages ? t("chat.updates") : "" }}
        </p>
        <p v-if="error" role="alert" class="notice error">
          {{ error }}
        </p>
        <p v-if="notice" role="status">
          {{ notice }}
        </p>
        <form v-if="!closed" class="stack composer" @submit.prevent="send">
          <label for="chat-message">{{ t("chat.message") }}</label><textarea
            id="chat-message"
            v-model="text"
            maxlength="2000"
            rows="3"
            :disabled="!!busy || active"
          />
          <details>
            <summary>{{ t("chat.file") }}</summary>
            <p>{{ t("chat.fileHelp") }}</p>
            <input
              ref="imageInput"
              class="sr-only"
              type="file"
              tabindex="-1"
              aria-label="Choose discussion file"
              accept=".pdf,.docx,.txt,.png,.jpg,.jpeg,.webm,.m4a,.mp4,.ogg,.wav"
              @change="fileChanged"
            /><el-button
              :disabled="!!busy || active || drafts.length >= 5"
              @click="imageInput?.click()"
            >
              {{ t("chat.chooseFile") }}
            </el-button><span class="filename">{{
              selectedFile?.name || t("chat.noFile")
            }}</span>
            <div v-if="imageSelected" class="stack">
              <label for="chat-alt">{{ t("chat.imageDescription") }}</label><input
                id="chat-alt"
                v-model="alt"
                maxlength="200"
                :disabled="!!busy"
              />
            </div>
            <el-button
              :disabled="!selectedFile || !!busy || active"
              @click="upload(selectedFile)"
            >
              {{ t("chat.upload") }}
            </el-button>
          </details>
          <AudioRecorder
            v-model="recording"
            :disabled="!!busy"
            uploadable
            :upload-disabled="drafts.length >= 5"
            :max-seconds="60"
            :max-bytes="5 * 1024 * 1024"
            :limit-help="t('chat.audioHelp')"
            :size-error="t('chat.invalidFile')"
            :help-text="t('chat.recorderHelp')"
            @activity="active = $event"
            @upload="upload"
          />
          <section v-if="drafts.length">
            <h3>{{ t("chat.pending") }}</h3>
            <p>{{ t("chat.privateDraft") }}</p>
            <ul class="draft-list">
              <li v-for="f in drafts" :key="f.id">
                <ChatAttachment :file="f" :api="api" /><el-button
                  :disabled="!!busy || active"
                  :aria-label="'Remove attachment: ' + f.originalName"
                  @click="removeFile(f.id)"
                >
                  Remove attachment
                </el-button>
              </li>
            </ul>
            <p class="muted">
              {{ t("chat.leaveFiles") }}
            </p>
          </section>
          <el-button
            native-type="submit"
            type="primary"
            :loading="busy === 'send'"
            :disabled="
              !!busy ||
                active ||
                !!recording ||
                !!selectedFile ||
                (!text.trim() && !drafts.length)
            "
          >
            {{ t("chat.send") }}
          </el-button>
        </form>
        <el-dialog
          v-model="membersOpen"
          :title="t('chat.members')"
          width="min(600px,95vw)"
          destroy-on-close
        >
          <ChatMembers
            v-if="membersOpen"
            :id="id"
            :api="api"
            :manager="manager"
            :closed="closed"
          />
        </el-dialog>
        <el-dialog
          v-model="editing"
          :title="t('chat.edit')"
          width="min(500px,95vw)"
          destroy-on-close
          :close-on-click-modal="false"
        >
          <form v-if="edit" class="stack" @submit.prevent="saveGroup">
            <label for="group-edit-name">{{ t("chat.name") }}</label><input
              id="group-edit-name"
              v-model="edit.title"
              maxlength="100"
              required
              :disabled="!!busy"
            /><label for="group-status">{{ t("chat.status") }}</label><select id="group-status" v-model="edit.status" :disabled="!!busy">
              <option value="ACTIVE">
                {{ t("chat.active") }}
              </option>
              <option value="CLOSED">
                {{ t("chat.closed") }}
              </option>
            </select>
            <div class="button-row">
              <el-button native-type="submit" :loading="!!busy">
                {{ t("chat.save") }}
              </el-button><el-button :disabled="!!busy" @click="editing = false">
                {{ t("chat.cancel") }}
              </el-button>
            </div>
          </form>
        </el-dialog>
      </template>
    </AsyncState>
  </section>
</template>
<style scoped>
.chat-room {
  min-width: 0;
}
.message-list {
  max-height: 32rem;
  overflow: auto;
  overflow-anchor: none;
  border: 1px solid var(--border, #c4d3d5);
  border-radius: 0.7rem;
  padding: 1rem;
}
.message-list ol {
  list-style: none;
  padding: 0;
  margin: 0;
}
.message {
  padding: 1rem 0;
  border-bottom: 1px solid var(--border, #c4d3d5);
  overflow-wrap: anywhere;
}
.message-heading {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
  align-items: center;
}
time {
  font-size: 0.85em;
}
.composer {
  margin-top: 1rem;
}
textarea,
input,
select {
  max-width: 100%;
  box-sizing: border-box;
}
textarea {
  width: 100%;
  resize: vertical;
}
.draft-list {
  list-style: none;
  padding: 0;
}
.draft-list li {
  border-bottom: 1px solid var(--border, #c4d3d5);
  padding: 0.6rem 0;
}
.filename {
  overflow-wrap: anywhere;
}
summary {
  cursor: pointer;
  padding: 0.6rem 0;
}
details > * {
  margin-block: 0.5rem;
}
</style>

<style scoped>
.message-options summary {
  cursor: pointer;
  min-width: 44px;
  min-height: 44px;
  text-align: center;
  font-size: 1.5rem;
  list-style: none;
}
.message-options button {
  min-height: 44px;
  cursor: pointer;
}
.message-options {
  margin-left: auto;
}
</style>
