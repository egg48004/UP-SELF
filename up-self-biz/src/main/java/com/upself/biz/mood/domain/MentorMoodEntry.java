package com.upself.biz.mood.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** All saved diaries are shared with current eligible mentors; never expose storage paths. */
@Data
@EqualsAndHashCode(callSuper = true)
public class MentorMoodEntry extends MoodPoint {
    private String diaryNote;
    private Long recordingId, recordingSize;
    private String recordingName, recordingMime;
}
