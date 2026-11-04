export interface StudentQuestionnaire {
  id: number;
  title: string;
  url: string;
}
export interface Questionnaire extends StudentQuestionnaire {
  visible: boolean;
  version: number;
}
export interface QuestionnaireInput {
  title: string;
  url: string;
  visible: boolean;
}
