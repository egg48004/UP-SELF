import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { useSpeech } from '../../src/features/preferences/composables/useSpeech';

describe('system speech lifecycle', () => {
  const engine = { cancel: vi.fn(), speak: vi.fn(), pause: vi.fn(), resume: vi.fn(), getVoices: vi.fn() };
  let speech: ReturnType<typeof useSpeech>;
  let dispose: () => void;
  beforeEach(() => {
    vi.resetAllMocks();
    engine.getVoices.mockReturnValue([{ lang: 'en-US' }]);
    vi.stubGlobal('speechSynthesis', engine);
    vi.stubGlobal('SpeechSynthesisUtterance', class {
      lang = ''; onend: (() => void) | null = null; onerror: (() => void) | null = null;
      constructor(public text: string) {}
    });
    const view = mount(defineComponent({ setup() { speech = useSpeech(); return () => null; } }));
    dispose = () => view.unmount();
  });
  afterEach(() => { dispose(); vi.unstubAllGlobals(); });
  it('reads English and supports pause, resume and stop', () => {
    speech.read('One small step.', 'en-GB');
    expect(engine.speak.mock.calls[0]?.[0]).toMatchObject({ text: 'One small step.', lang: 'en-GB' });
    speech.pause(); expect(speech.paused.value).toBe(true); expect(engine.pause).toHaveBeenCalledOnce();
    speech.resume(); expect(speech.paused.value).toBe(false); expect(engine.resume).toHaveBeenCalledOnce();
    speech.stop(); expect(speech.speaking.value).toBe(false);
  });
  it('shows a fallback when the requested voice is unavailable', () => {
    engine.getVoices.mockReturnValue([]);
    speech.read('One small step.');
    expect(speech.error.value).toBe('speechUnavailable'); expect(engine.speak).not.toHaveBeenCalled();
  });
  it('clears all active state on a native engine error', () => {
    speech.read('Hello'); speech.pause();
    engine.speak.mock.calls[0]?.[0].onerror();
    expect(speech.speaking.value).toBe(false); expect(speech.paused.value).toBe(false);
    expect(speech.error.value).toBe('speechUnavailable');
  });
  it('does not let delayed events from an old utterance end the new one', () => {
    speech.read('First'); const endFirst = engine.speak.mock.calls[0]?.[0].onend;
    speech.read('Second'); endFirst();
    expect(speech.speaking.value).toBe(true);
    engine.speak.mock.calls[1]?.[0].onend(); expect(speech.speaking.value).toBe(false);
  });
  it('recovers when the engine throws synchronously', () => {
    engine.speak.mockImplementation(() => { throw new Error('Engine unavailable'); });
    speech.read('Hello'); expect(speech.speaking.value).toBe(false); expect(speech.error.value).toBe('speechUnavailable');
  });
  it('cancels playback when the reader unmounts', () => {
    speech.read('Hello'); const before = engine.cancel.mock.calls.length;
    dispose(); expect(engine.cancel.mock.calls.length).toBe(before + 1); expect(speech.speaking.value).toBe(false);
  });
});
