import { ref, shallowRef, onMounted, onBeforeUnmount } from "vue";
import type { TableResult } from "../../shared/http/contracts";
/** Feature-local paging; one request per refresh and no polling or global state. */
export function useQuestionnaireList<T>(
  fetchPage: (page: number) => Promise<TableResult<T>>,
) {
  const rows = shallowRef<T[]>([]),
    current = ref(1),
    total = ref(0),
    loading = ref(false),
    error = ref("");
  let sequence = 0;
  async function load(page = current.value): Promise<void> {
    const token = ++sequence;
    loading.value = true;
    error.value = "";
    try {
      const result = await fetchPage(page);
      if (token !== sequence) return;
      const last = Math.max(1, Math.ceil(result.total / 10));
      if (page > last) return await load(last);
      rows.value = result.rows;
      total.value = result.total;
      current.value = page;
    } catch (e) {
      if (token === sequence) error.value = (e as Error).message;
    } finally {
      if (token === sequence) loading.value = false;
    }
  }
  onMounted(() => load());
  onBeforeUnmount(() => {
    sequence++;
  });
  return { rows, current, total, loading, error, load };
}
