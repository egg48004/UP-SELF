<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { reportApi } from "../api";
import type { Report } from "../api";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import PaginationBar from "../../../shared/ui/PaginationBar.vue";
import { learningDate } from "../../learning/progress";
const { t } = useI18n(),
  rows = ref<Report[]>([]),
  status = ref("PENDING"),
  current = ref(1),
  total = ref(0),
  loading = ref(false),
  error = ref("");
let sequence = 0;
async function load(page = 1) {
  const seq = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const r = await reportApi.list(page, status.value);
    if (seq !== sequence) return;
    const last = Math.max(1, Math.ceil(r.total / 10));
    if (page > last) return await load(last);
    rows.value = r.rows;
    total.value = r.total;
    current.value = page;
  } catch (e) {
    if (seq === sequence) error.value = (e as Error).message;
  } finally {
    if (seq === sequence) loading.value = false;
  }
}

onMounted(() => load());
</script>
<template>
  <section>
    <h1>{{ t("reports.title") }}</h1>
    <p>{{ t("reports.adminHelp") }}</p>
    <label for="report-filter">{{ t("reports.filter") }}</label><select
      id="report-filter"
      v-model="status"
      :disabled="loading"
      @change="load()"
    >
      <option value="">
        {{ t("reports.all") }}
      </option>
      <option
        v-for="s in ['PENDING', 'RESOLVED', 'DISMISSED']"
        :key="s"
        :value="s"
      >
        {{ t("reports." + s) }}
      </option>
    </select>
    <el-button :disabled="loading" @click="load(current)">
      {{
        t("reports.refresh")
      }}
    </el-button>
    <AsyncState :loading="loading" :error="error" @retry="load(current)">
      <div class="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ t("reports.created") }}</th>
              <th>{{ t("reports.accused") }}</th>
              <th>{{ t("reports.reason") }}</th>
              <th>{{ t("reports.status") }}</th>
              <th>{{ t("reports.open") }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in rows" :key="r.id">
              <td>{{ learningDate(r.createTime) }}</td>
              <td>{{ r.accusedName }}</td>
              <td>{{ t("reports." + r.reason) }}</td>
              <td>{{ t("reports." + r.status) }}</td>
              <td>
                <RouterLink :to="'/manage/reports/' + r.id">
                  {{ t("reports.open") }}
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!rows.length">
          {{ t("reports.empty") }}
        </p>
      </div>
      <PaginationBar
        v-if="total > 10"
        :current="current"
        :total="total"
        @change="load"
      />
    </AsyncState>
  </section>
</template>
