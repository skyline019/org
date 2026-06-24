(function () {
    'use strict';

    window.AuthCore = {
        csrfToken: function () {
            var meta = document.querySelector('meta[name="_csrf"]');
            return meta ? meta.getAttribute('content') : '';
        },

        csrfHeader: function () {
            var meta = document.querySelector('meta[name="_csrf_header"]');
            return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
        },

        mountVue: function (selector, setupFn) {
            document.addEventListener('DOMContentLoaded', function () {
                var el = document.querySelector(selector);
                if (!el || typeof Vue === 'undefined' || typeof ElementPlus === 'undefined') {
                    return;
                }
                var app = Vue.createApp({ setup: setupFn(el) });
                app.use(ElementPlus);
                app.mount(selector);
            });
        },

        readData: function (el) {
            return el ? el.dataset : {};
        }
    };
})();
