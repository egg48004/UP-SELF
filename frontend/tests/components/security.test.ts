import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createI18n } from 'vue-i18n';
import { ElButton } from 'element-plus';
const mocks = vi.hoisted(() => ({ password: vi.fn(), clear: vi.fn(), replace: vi.fn() }));
vi.mock('../../src/features/auth/api', () => ({ authApi: { password: mocks.password } }));
vi.mock('../../src/features/auth/store', () => ({ useAuthStore: () => ({ clear: mocks.clear }) }));
vi.mock('vue-router', () => ({ useRouter: () => ({ replace: mocks.replace }) }));
import SecurityPage from '../../src/features/auth/pages/SecurityPage.vue';
import en from '../../src/shared/i18n/en';
import { session } from '../../src/shared/auth/session';
function render() { return mount(SecurityPage, { global: { plugins: [createI18n({ legacy: false, locale: 'en', messages: { en } })], components: { ElButton } } }); }
describe('password form without changing any real account credentials', () => {
  beforeEach(() => { vi.clearAllMocks(); session.save('fixture-only', 'student'); });
  it('submits through the existing API and only signs out after confirmed success', async () => {
    mocks.password.mockResolvedValue({ code: 200 }); const wrapper = render();
    await wrapper.get('#old-password').setValue('fixture-old'); await wrapper.get('#new-password').setValue('fixture-new-only');
    await wrapper.get('form').trigger('submit'); await flushPromises();
    expect(mocks.password).toHaveBeenCalledWith('student', 'fixture-old', 'fixture-new-only');
    expect(mocks.clear).toHaveBeenCalledOnce(); expect(mocks.replace).toHaveBeenCalledWith('/login');
  });
  it('keeps the form and session when the server rejects the current password', async () => {
    mocks.password.mockRejectedValue(new Error('The current password is incorrect.')); const wrapper = render();
    await wrapper.get('#old-password').setValue('fixture-old'); await wrapper.get('#new-password').setValue('fixture-new-only');
    await wrapper.get('form').trigger('submit'); await flushPromises();
    expect(wrapper.get('[role="alert"]').text()).toContain('current password');
    expect(mocks.clear).not.toHaveBeenCalled(); expect(mocks.replace).not.toHaveBeenCalled();
    expect((wrapper.get('#new-password').element as HTMLInputElement).value).toBe('fixture-new-only');
  });
});
