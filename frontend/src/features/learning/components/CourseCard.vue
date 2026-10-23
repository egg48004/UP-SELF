<script setup lang="ts">
import { useI18n } from "vue-i18n";
import { ref, watch } from "vue";
import BookmarkButton from "./BookmarkButton.vue";
import CourseFacts from "./CourseFacts.vue";
import type { Course } from "../types";
const props = defineProps<{ course: Course; index: number }>(),
  emit = defineEmits<{ bookmarkChanged: [saved: boolean] }>(),
  saved = ref(props.course.bookmarked);
watch(() => props.course, course => { saved.value = course.bookmarked; });
function changed(value: boolean) {
  saved.value = value;
  emit("bookmarkChanged", value);
}
const { t } = useI18n();
</script>
<template>
  <article class="course-card" :aria-labelledby="'course-heading-' + course.id">
    <div :class="['course-art', 'art-' + (index % 3)]" aria-hidden="true">
      <span>{{ ["🌱", "🧭", "💡"][index % 3] }}</span><small>UP-SELF</small>
    </div>
    <div class="course-card-body">
      <span class="eyebrow">{{ t("myCourses") }}</span>
      <h2 :id="'course-heading-' + course.id">
        {{ course.title }}
      </h2>
      <p class="course-summary">
        {{ course.description }}
      </p>
      <CourseFacts :course="course" />
      <RouterLink class="button-link" :to="'/student/courses/' + course.id">
        {{ t("openCourse") }} <span aria-hidden="true">→</span>
      </RouterLink>
      <BookmarkButton :course-id="course.id" :saved="saved" :title="course.title" @changed="changed" />
    </div>
  </article>
</template>
