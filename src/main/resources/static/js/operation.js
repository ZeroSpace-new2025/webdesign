/* ============================================================
   M3 运营与履约 · 页面脚本
   ============================================================ */
(function () {
    'use strict';

    // 全选/反选（配送批量打印用）
    window.m3ToggleAll = function (source) {
        var checkboxes = document.querySelectorAll('input[name="taskIds"]');
        checkboxes.forEach(function (cb) { cb.checked = source.checked; });
    };

    // 打印前确认
    window.m3ConfirmPrint = function (event) {
        var checked = document.querySelectorAll('input[name="taskIds"]:checked');
        if (checked.length === 0) {
            event.preventDefault();
            alert('请至少选择一张配送单');
            return false;
        }
        return confirm('确认打印选中的 ' + checked.length + ' 张配送单？');
    };

    // 表单提交前的时间校验（时间窗口配置用）
    document.addEventListener('DOMContentLoaded', function () {
        var form = document.querySelector('form[data-m3="service-window"]');
        if (form) {
            form.addEventListener('submit', function (e) {
                var cutoff = form.querySelector('input[name="cutoffTime"]').value;
                var delivery = form.querySelector('input[name="deliveryStartTime"]').value;
                if (cutoff && delivery && cutoff >= delivery) {
                    e.preventDefault();
                    alert('订餐截止时间必须早于配餐开始时间');
                    return false;
                }
            });
        }
    });
})();