(function () {
    'use strict';

    function formHasValidationErrors(form) {
        return form.querySelector('.alert-error, .field-hint.error, .el-alert--error');
    }

    function clearAuthForm(form, options) {
        if (!form || formHasValidationErrors(form)) {
            return;
        }
        form.querySelectorAll('input[type="password"]').forEach(function (input) {
            input.value = '';
        });
        if (options && options.clearUsername) {
            form.querySelectorAll('input[name="username"], input[id="username"]').forEach(function (input) {
                if (input.type !== 'email') {
                    input.value = '';
                }
            });
        }
        if (options && options.clearEmail) {
            form.querySelectorAll('input[type="email"], input[name="email"], input[id="email"]').forEach(function (input) {
                input.value = '';
            });
        }
    }

    function initAuthForms() {
        document.querySelectorAll('form.auth-form').forEach(function (form) {
            clearAuthForm(form, {
                clearUsername: false,
                clearEmail: form.id === 'registerForm'
            });
        });
    }

    document.addEventListener('DOMContentLoaded', initAuthForms);

    window.addEventListener('pageshow', function (event) {
        if (!event.persisted) {
            return;
        }
        document.querySelectorAll('form.auth-form').forEach(function (form) {
            clearAuthForm(form, { clearUsername: true, clearEmail: true });
        });
    });
})();
