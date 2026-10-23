export interface EfficacyPerson { id: number; displayName: string; username: string }
export interface EfficacyInstrument {
  version: string; name: string; instruction: string; questions: string[]; responses: string[];
  sourceUrl: string; attribution: string; license: string; licenseUrl: string;
}
export interface EfficacyPoint { assessmentDate: string; score: number; instrumentVersion: string }
export interface EfficacyEntry extends EfficacyPoint { id: number; studentId: number; answerOne: number; answerTwo: number; answerThree: number; createTime: string }
export interface EfficacyCurrent {
  today: string; timeZone: string; intervalDays: number; acknowledged: boolean; noticeVersion: string;
  available: boolean; nextAvailableDate: string; latest: EfficacyEntry | null; viewers: EfficacyPerson[]; instrument: EfficacyInstrument;
}
export interface EfficacyTrend { startDate: string; endDate: string; timeZone: string; points: EfficacyPoint[] }
