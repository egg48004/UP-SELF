import { stopSpeech } from "../preferences/composables/useSpeech";
export function stopOtherMedia(current?: HTMLMediaElement) {
  stopSpeech();
  document
    .querySelectorAll<HTMLMediaElement>("audio,video")
    .forEach((media) => {
      if (media !== current && !media.paused) media.pause();
    });
}
