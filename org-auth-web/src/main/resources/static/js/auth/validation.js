(function () {
    'use strict';

    var DEBOUNCE_MS = 300;
    var timers = {};

    function debounce(key, fn) {
        clearTimeout(timers[key]);
        timers[key] = setTimeout(fn, DEBOUNCE_MS);
    }

    async function check(endpoint, value) {
        var params = new URLSearchParams({ value: value });
        var response = await fetch('/api/v1/auth/check/' + endpoint + '?' + params);
        return response.json();
    }

    function setHint(elementId, message, ok) {
        var el = document.getElementById(elementId);
        if (!el) {
            return;
        }
        el.textContent = message || '';
        el.className = 'field-hint' + (message ? (ok ? ' ok' : ' error') : '');
    }

    function setupUsernameCheck() {
        var input = document.getElementById('username');
        if (!input) {
            return;
        }
        input.addEventListener('input', function () {
            debounce('username', async function () {
                var value = input.value.trim();
                if (!value) {
                    setHint('username-hint', '', false);
                    return;
                }
                var result = await check('username', value);
                var data = result.data;
                setHint('username-hint', data.message, data.available);
            });
        });
    }

    function setupEmailCheck() {
        var input = document.getElementById('email');
        if (!input) {
            return;
        }
        input.addEventListener('input', function () {
            debounce('email', async function () {
                var value = input.value.trim();
                if (!value) {
                    setHint('email-hint', '', false);
                    return;
                }
                var result = await check('email', value);
                var data = result.data;
                setHint('email-hint', data.message, data.available);
            });
        });
    }

    function setupPasswordCheck() {
        var input = document.getElementById('password');
        var rulesEl = document.getElementById('password-rules');
        if (!input || !rulesEl) {
            return;
        }
        input.addEventListener('input', function () {
            debounce('password', async function () {
                var value = input.value;
                if (!value) {
                    rulesEl.innerHTML = '';
                    return;
                }
                var result = await check('password', value);
                var data = result.data;
                rulesEl.innerHTML = (data.rules || []).map(function (rule) {
                    var ok = rule.passed === true;
                    var text = rule.message || rule;
                    return '<li class="' + (ok ? 'ok' : 'error') + '">' + text + '</li>';
                }).join('');
            });
        });
    }

    function setupConfirmCheck() {
        var password = document.getElementById('password');
        var confirm = document.getElementById('confirmPassword');
        if (!password || !confirm) {
            return;
        }
        var i18n = window.authI18n || {};
        function validate() {
            if (!confirm.value) {
                setHint('confirm-hint', '', false);
                return;
            }
            var match = password.value === confirm.value;
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
