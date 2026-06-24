(function () {
    const DEBOUNCE_MS = 300;
    const timers = {};

    function debounce(key, fn) {
        clearTimeout(timers[key]);
        timers[key] = setTimeout(fn, DEBOUNCE_MS);
    }

    async function check(endpoint, value) {
        const params = new URLSearchParams({ value: value });
        const response = await fetch('/api/v1/auth/check/' + endpoint + '?' + params);
        return response.json();
    }

    function setHint(elementId, message, ok) {
        const el = document.getElementById(elementId);
        if (!el) return;
        el.textContent = message || '';
        el.className = 'field-hint' + (message ? (ok ? ' ok' : ' error') : '');
    }

    function setupUsernameCheck() {
        const input = document.getElementById('username');
        if (!input) return;
        input.addEventListener('input', function () {
            debounce('username', async function () {
                const value = input.value.trim();
                if (!value) {
                    setHint('username-hint', '', false);
                    return;
                }
                const result = await check('username', value);
                const data = result.data;
                setHint('username-hint', data.message, data.available);
            });
        });
    }

    function setupEmailCheck() {
        const input = document.getElementById('email');
        if (!input) return;
        input.addEventListener('input', function () {
            debounce('email', async function () {
                const value = input.value.trim();
                if (!value) {
                    setHint('email-hint', '', false);
                    return;
                }
                const result = await check('email', value);
                const data = result.data;
                setHint('email-hint', data.message, data.available);
            });
        });
    }

    function setupPasswordCheck() {
        const input = document.getElementById('password');
        const rulesEl = document.getElementById('password-rules');
        if (!input || !rulesEl) return;
        input.addEventListener('input', function () {
            debounce('password', async function () {
                const value = input.value;
                if (!value) {
                    rulesEl.innerHTML = '';
                    return;
                }
                const result = await check('password', value);
                const data = result.data;
                rulesEl.innerHTML = (data.rules || []).map(function (rule) {
                    const ok = rule.passed === true;
                    const text = rule.message || rule;
                    return '<li class="' + (ok ? 'ok' : 'error') + '">' + text + '</li>';
                }).join('');
            });
        });
    }

    function setupConfirmCheck() {
        const password = document.getElementById('password');
        const confirm = document.getElementById('confirmPassword');
        if (!password || !confirm) return;
        const i18n = window.authI18n || {};
        function validate() {
            if (!confirm.value) {
                setHint('confirm-hint', '', false);
                return;
            }
            const match = password.value === confirm.value;
            setHint('confirm-hint',
                    match ? (i18n.passwordMatch || '') : (i18n.passwordMismatch || ''),
                    match);
        }
        confirm.addEventListener('input', validate);
        password.addEventListener('input', validate);
    }

    document.addEventListener('DOMContentLoaded', function () {
        setupUsernameCheck();
        setupEmailCheck();
        setupPasswordCheck();
        setupConfirmCheck();
    });
})();
