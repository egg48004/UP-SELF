/** Only external HTTPS navigation; never embed or fetch a questionnaire. */
export function safeQuestionnaireUrl(value: string): string | null {
  if (
    !/^https:\/\//i.test(value) ||
    /^https:\/\/[^/?#]*@/i.test(value) ||
    value.includes(String.fromCharCode(92)) ||
    value.length > 2000 ||
    /[\s\u0000-\u001f\u007f]/u.test(value)
  )
    return null;
  try {
    const url = new URL(value);
    return url.protocol === "https:" &&
      !!url.hostname &&
      !url.username &&
      !url.password &&
      url.port !== "0"
      ? url.href
      : null;
  } catch {
    return null;
  }
}
