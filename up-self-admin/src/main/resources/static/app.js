"use strict";
const $ = id => document.getElementById(id);
const roleLabels = { student: "Student", mentor: "Mentor", platform_admin: "Platform administrator", admin: "Maintenance administrator" };
let channel = sessionStorage.getItem('channel') || 'student', token = sessionStorage.getItem('token'), ticket = null, permissions = [], pageNum = 1, total = 0, selected = null;
function message(t) { $('message').textContent = t || ''; }
function home() { token = null; ticket = null; sessionStorage.removeItem('token'); $('loginPanel').hidden = false; $('totpPanel').hidden = true; $('dashboard').hidden = true; $('logout').hidden = true; $('bindingSecret').textContent = ''; $('totpCode').value = ''; $('password').value = ''; }
async function api(path, method = 'GET', body) {
    const headers = { 'Content-Type': 'application/json' };
    if (token)
        headers.Authorization = 'Bearer ' + token;
    const response = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    let r;
    try {
        r = await response.json();
    }
    catch (e) {
        throw Error('The server response is unavailable. Check the development console.');
    }
    if (r.code !== 200) {
        if (r.code === 401)
            home();
        throw Error(r.msg || 'The request failed.');
    }
    return r;
}
async function run(fn) {
    message('');
    try {
        await fn();
    }
    catch (e) {
        message(e.message);
    }
}
async function dashboard() {
    const r = await api(channel === 'student' ? '/client/getInfo' : '/getInfo');
    permissions = r.permissions || [];
    $('welcome').textContent = 'Welcome, ' + r.user.nickName;
    $('identity').textContent = 'Username：' + r.user.userName + ' · Role：' + r.roles.map(role => roleLabels[role] || role).join(', ');
    $('permissions').textContent = permissions.length ? permissions.join('\n') : 'No additional API permissions are assigned.';
    $('loginPanel').hidden = true;
    $('totpPanel').hidden = true;
    $('bindingSecret').textContent = '';
    $('dashboard').hidden = false;
    $('logout').hidden = false;
    const manage = permissions.includes('system:user:list') || permissions.includes('*:*:*');
    $('userPanel').hidden = !manage;
    $('resetTotp').hidden = !permissions.includes('*:*:*');
    if (manage)
        await loadUsers();
}
async function complete(r) { token = r.token; ticket = null; sessionStorage.setItem('token', token); sessionStorage.setItem('channel', channel); await dashboard(); }
$('loginForm').addEventListener('submit', e => {
    e.preventDefault();
    run(async () => {
        channel = $('channel').value;
        const r = await api(channel === 'student' ? '/client/login' : '/empLogin', 'POST', { username: $('username').value, password: $('password').value });
        $('password').value = '';
        if (r.token)
            return complete(r);
        ticket = r.ticket;
        $('loginPanel').hidden = true;
        $('totpPanel').hidden = false;
        $('binding').hidden = !r.needBind;
        $('bindingSecret').textContent = r.needBind ? r.secret : '';
        $('totpTitle').textContent = r.needBind ? 'Set up your authenticator' : 'Enter verification code';
    });
});
$('totpForm').addEventListener('submit', e => { e.preventDefault(); run(async () => { const r = await api('/empLogin/totp', 'POST', { ticket, code: $('totpCode').value }); $('totpCode').value = ''; await complete(r); }); });
$('back').onclick = home;
$('logout').onclick = () => run(async () => { await api(channel === 'student' ? '/client/logout' : '/logout', 'POST'); home(); message('You have signed out.'); });
$('passwordForm').addEventListener('submit', e => { e.preventDefault(); run(async () => { await api(channel === 'student' ? '/client/profile/password' : '/system/user/profile/updatePwd', 'PUT', { oldPassword: $('oldPassword').value, newPassword: $('newPassword').value }); $('passwordForm').reset(); home(); message('Your password has been changed. Please sign in again.'); }); });
async function loadUsers() {
    const r = await api('/system/user/list?current=' + pageNum + '&pageSize=10&userName=' + encodeURIComponent($('search').value));
    total = r.total;
    $('count').textContent = 'Total: ' + total + ' accounts';
    $('page').textContent = 'Page ' + pageNum + '';
    $('prev').disabled = pageNum <= 1;
    $('next').disabled = pageNum * 10 >= total;
    $('users').replaceChildren();
    for (const u of r.rows) {
        const tr = document.createElement('tr');
        for (const v of [u.id, u.userName, u.nickName, u.roleKey, u.status === '0' ? 'Active' : 'Disabled']) {
            const td = document.createElement('td');
            td.textContent = v;
            tr.append(td);
        }
        const td = document.createElement('td'), b = document.createElement('button');
        b.textContent = 'Edit';
        b.onclick = () => { selected = u; $('editName').textContent = u.userName; $('editNick').value = u.nickName; $('editRole').value = u.roleId; $('editStatus').value = u.status; $('editPanel').hidden = false; };
        td.append(b);
        tr.append(td);
        $('users').append(tr);
    }
}
$('searchForm').addEventListener('submit', e => { e.preventDefault(); pageNum = 1; run(loadUsers); });
$('prev').onclick = () => { pageNum--; run(loadUsers); };
$('next').onclick = () => { pageNum++; run(loadUsers); };
$('createForm').addEventListener('submit', e => { e.preventDefault(); run(async () => { await api('/system/user', 'POST', { userName: $('createName').value, nickName: $('createNick').value, password: $('createPassword').value, roleId: Number($('createRole').value) }); $('createForm').reset(); pageNum = 1; await loadUsers(); message('The account has been created.'); }); });
$('editForm').addEventListener('submit', e => { e.preventDefault(); run(async () => { await api('/system/user', 'PUT', { id: selected.id, nickName: $('editNick').value, roleId: Number($('editRole').value), status: $('editStatus').value }); $('editPanel').hidden = true; await loadUsers(); message('Saved. Existing sessions for this account have been invalidated.'); }); });
$('resetForm').addEventListener('submit', e => { e.preventDefault(); run(async () => { await api('/system/user/resetPwd', 'PUT', { id: selected.id, password: $('resetPassword').value }); $('resetForm').reset(); message('The password has been reset. This account must sign in again.'); }); });
$('resetTotp').onclick = () => run(async () => { await api('/system/user/' + selected.id + '/resetTotp', 'PUT'); message('The authenticator has been reset. This account must bind it again.'); });
if (token)
    run(dashboard);
// Keep validation text English even when the browser's own language is different.
document.addEventListener("invalid", event => {
    const field = event.target;
    if (field.setCustomValidity) {
        field.setCustomValidity(field.validity.valueMissing
            ? "Please complete this field."
            : "Please enter a value in the required format and length.");
    }
}, true);
document.addEventListener("input", event => {
    if (event.target.setCustomValidity)
        event.target.setCustomValidity("");
});
document.addEventListener("change", event => {
    if (event.target.setCustomValidity)
        event.target.setCustomValidity("");
});
