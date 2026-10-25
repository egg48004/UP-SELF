/** Keep Chrome utterances short without dropping words or changing their order. */
export function splitSpeechText(text: string): string[] {
  let rest = text.replace(/\s+/gu, " ").trim();
  const chunks: string[] = [];
  while (rest) {
    const words = rest.match(/\S+\s*/gu) || [];
    let length = 0;
    for (const word of words.slice(0, 24)) {
      if (length && length + word.length > 160) break;
      length += word.length;
      if (length >= 160) break;
    }
    if (length > 160) length = Array.from(rest).slice(0, 160).join("").length;
    if (length < rest.length) {
      const prefix = rest.slice(0, length);
      const ends = [...prefix.matchAll(/[.!?;:,]["')\]]?(?=\s|$)/gu)];
      const last = ends.at(-1);
      if (last && last.index >= length / 2)
        length = last.index + last[0].length;
    }
    chunks.push(rest.slice(0, length).trim());
    rest = rest.slice(length).trimStart();
  }
  return chunks;
}

export function voiceKey(voice: SpeechSynthesisVoice): string {
  return `${voice.voiceURI}|${voice.lang}|${voice.name}`;
}

export function matchingVoices(
  voices: SpeechSynthesisVoice[],
  language: string,
) {
  const lang = (language || "en").replaceAll("_", "-").toLowerCase();
  const base = lang.split("-")[0];
  const score = (voice: SpeechSynthesisVoice) =>
    (voice.localService ? 100 : 0) +
    (/natural|premium|enhanced/iu.test(voice.name) ? 50 : 0) +
    (voice.lang.toLowerCase() === lang ? 20 : 0) +
    (voice.default ? 10 : 0);
  return voices
    .filter(
      (voice) =>
        voice.lang.replaceAll("_", "-").toLowerCase().split("-")[0] === base,
    )
    .sort((a, b) => score(b) - score(a) || a.name.localeCompare(b.name));
}
