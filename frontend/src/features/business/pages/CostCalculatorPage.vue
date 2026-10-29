<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { onBeforeRouteLeave } from "vue-router";
import { useI18n } from "vue-i18n";
import { costApi } from "../api";
import type { CostInput, CostResult } from "../types";
import { money, validAmount, validQuantity } from "../presentation";
import AsyncState from "../../../shared/ui/AsyncState.vue";
import { confirmAction } from "../../../shared/ui/confirm";
const { t } = useI18n(),
  form = reactive<CostInput>({
    materialsCost: "",
    otherCosts: "",
    quantity: "",
    pricePerItem: "",
  }),
  result = ref<CostResult>(),
  loading = ref(true),
  loadError = ref(""),
  error = ref(""),
  notice = ref(""),
  busy = ref(false),
  version = ref(0),
  invalid = ref<string[]>([]);
const baseline = ref(JSON.stringify(form)),
  dirty = computed(() => JSON.stringify(form) !== baseline.value),
  loss = computed(() => result.value?.estimatedProfit.startsWith("-"));
watch(form, () => {
  result.value = undefined;
  notice.value = "";
});
async function load(confirm = false) {
  if (
    confirm &&
    dirty.value &&
    !(await confirmAction(t("business.reloadConfirm")))
  )
    return;
  loading.value = true;
  loadError.value = "";
  try {
    const p = await costApi.current();
    version.value = p.version;
    if (p.saved) {
      Object.assign(form, {
        materialsCost: p.materialsCost,
        otherCosts: p.otherCosts,
        quantity: String(p.quantity),
        pricePerItem: p.pricePerItem,
      });
      await Promise.resolve();
      result.value = p;
    } else {
      Object.assign(form, {
        materialsCost: "",
        otherCosts: "",
        quantity: "",
        pricePerItem: "",
      });
      await Promise.resolve();
      result.value = undefined;
    }
    baseline.value = JSON.stringify(form);
    invalid.value = [];
    error.value = "";
    notice.value = confirm
      ? t(p.saved ? "business.refreshed" : "business.noSaved")
      : "";
  } catch (e) {
    loadError.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
function validate() {
  invalid.value = (
    ["materialsCost", "otherCosts", "pricePerItem"] as const
  ).filter((k) => !validAmount(form[k]));
  if (!validQuantity(form.quantity)) invalid.value.push("quantity");
  if (invalid.value.length) {
    document.getElementById("cost-" + invalid.value[0])?.focus();
    return false;
  }
  return true;
}
async function calculate(save = false) {
  if (busy.value) return;
  error.value = "";
  notice.value = "";
  if (!validate()) return;
  busy.value = true;
  try {
    const p = save
      ? await costApi.save({ ...form }, version.value)
      : await costApi.calculate({ ...form });
    result.value = p;
    if (save) {
      version.value = p.version;
      baseline.value = JSON.stringify(form);
      notice.value = t("business.planSaved");
    }
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
onBeforeRouteLeave(
  async () =>
    !busy.value && (!dirty.value || (await confirmAction(t("business.leave")))),
);
onMounted(() => load());
</script>
<template>
  <section class="calculator-page">
    <RouterLink to="/student/business">
      ← {{ t("business.back") }}
    </RouterLink>
    <h1><span aria-hidden="true">🧮 </span>{{ t("business.calculator") }}</h1>
    <p>{{ t("business.calculatorHelp") }}</p>
    <AsyncState :loading="loading" :error="loadError" @retry="load()">
      <form class="stack panel" :aria-busy="busy" @submit.prevent="calculate()">
        <fieldset :disabled="busy" class="stack">
          <legend>{{ t("business.costs") }}</legend>
          <template
            v-for="field in ['materialsCost', 'otherCosts'] as const"
            :key="field"
          >
            <label :for="'cost-' + field">{{
              t(
                field === "materialsCost"
                  ? "business.materials"
                  : "business.other",
              )
            }}</label><input
              :id="'cost-' + field"
              v-model="form[field]"
              inputmode="decimal"
              maxlength="13"
              :aria-invalid="invalid.includes(field)"
              :aria-describedby="
                invalid.includes(field) ? 'error-' + field : undefined
              "
            />
            <p
              v-if="invalid.includes(field)"
              :id="'error-' + field"
              class="notice error"
              role="alert"
            >
              {{ t("business.amountError") }}
            </p>
          </template>
        </fieldset>
        <fieldset :disabled="busy" class="stack">
          <legend>{{ t("business.sales") }}</legend>
          <label for="cost-quantity">{{ t("business.quantity") }}</label><input
            id="cost-quantity"
            v-model="form.quantity"
            inputmode="numeric"
            maxlength="7"
            :aria-invalid="invalid.includes('quantity')"
            :aria-describedby="
              invalid.includes('quantity') ? 'error-quantity' : undefined
            "
          />
          <p
            v-if="invalid.includes('quantity')"
            id="error-quantity"
            class="notice error"
            role="alert"
          >
            {{ t("business.quantityError") }}
          </p>
          <label for="cost-pricePerItem">{{ t("business.price") }}</label><input
            id="cost-pricePerItem"
            v-model="form.pricePerItem"
            inputmode="decimal"
            maxlength="13"
            :aria-invalid="invalid.includes('pricePerItem')"
            :aria-describedby="
              invalid.includes('pricePerItem')
                ? 'error-pricePerItem'
                : undefined
            "
          />
          <p
            v-if="invalid.includes('pricePerItem')"
            id="error-pricePerItem"
            class="notice error"
            role="alert"
          >
            {{ t("business.amountError") }}
          </p>
        </fieldset>
        <p v-if="error" role="alert" class="notice error">
          {{ error }}
        </p>
        <div class="actions">
          <el-button type="primary" native-type="submit" :loading="busy">
            {{
              t("business.calculate")
            }}
          </el-button><el-button :disabled="busy" @click="calculate(true)">
            {{
              t("business.savePlan")
            }}
          </el-button><el-button :disabled="busy" @click="load(true)">
            {{
              t("business.reload")
            }}
          </el-button>
        </div>
        <p class="muted">
          {{ t("business.planHelp") }}
        </p>
      </form>
      <p v-if="notice" class="notice" role="status">
        {{ notice }}
      </p>
      <section
        class="panel"
        aria-live="polite"
        aria-atomic="true"
        :aria-label="t('business.results')"
      >
        <h2>{{ t("business.results") }}</h2>
        <dl v-if="result" class="cost-results">
          <div>
            <dt>{{ t("business.total") }}</dt>
            <dd>{{ money(result.totalCost) }}</dd>
          </div>
          <div>
            <dt>{{ t("business.revenue") }}</dt>
            <dd>{{ money(result.expectedRevenue) }}</dd>
          </div>
          <div>
            <dt>{{ t(loss ? "business.loss" : "business.profit") }}</dt>
            <dd>{{ money(result.estimatedProfit.replace(/^-/u, "")) }}</dd>
          </div>
        </dl>
        <p v-if="result && loss">
          {{ t("business.lossHelp") }}
        </p>
        <p v-if="!result">
          {{ t(dirty ? "business.changed" : "business.noResults") }}
        </p>
      </section>
    </AsyncState>
  </section>
</template>
<style scoped>
.calculator-page {
  max-width: 52rem;
}
fieldset {
  border: 1px solid var(--border, #b5c8c8);
  border-radius: 0.75rem;
  padding: 1rem;
  min-width: 0;
}
legend {
  font-weight: 700;
  padding: 0 0.4rem;
}
.cost-results {
  display: grid;
  gap: 1rem;
}
.cost-results dd {
  margin: 0;
  font-size: 1.4em;
  font-weight: 700;
  overflow-wrap: anywhere;
}
input {
  max-width: 100%;
  box-sizing: border-box;
}
</style>
