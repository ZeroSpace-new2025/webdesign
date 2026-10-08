(function () {
	"use strict";

	// 登录页只负责收集凭据和做提交前校验，认证由 Spring Security 处理。
	const form = document.getElementById("login-form");
	const usernameInput = document.getElementById("username");
	const passwordInput = document.getElementById("password");
	const errorElement = document.getElementById("login-error");
	const submitButton = document.getElementById("login-submit");

	function refreshIcons() {
		if (window.lucide) {
			window.lucide.createIcons();
		}
	}

	function setError(message) {
		if (errorElement) {
			errorElement.textContent = message || "";
		}
	}

	function submitLogin(event) {
		setError("");
		const username = usernameInput.value.trim();
		const password = passwordInput.value;
		if (!username || !password) {
			event.preventDefault();
			setError("请输入用户名和密码");
			return;
		}

		usernameInput.value = username;
		submitButton.disabled = true;
		const label = submitButton.querySelector("span");
		if (label) {
			label.textContent = "登录中";
		}
	}

	// 演示账号按钮用于快速填充登录表单。
	document.querySelectorAll(".account-option").forEach((button) => {
		button.addEventListener("click", () => {
			usernameInput.value = button.dataset.username || "";
			passwordInput.value = button.dataset.password || "";
			usernameInput.focus();
			setError("");
		});
	});

	form.addEventListener("submit", submitLogin);
	refreshIcons();
})();
