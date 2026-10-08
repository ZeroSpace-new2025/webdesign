(function () {
	"use strict";

	// 登录页只负责收集凭据、调用接口和切换到控制台。
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

	async function submitLogin(event) {
		event.preventDefault();
		setError("");
		const username = usernameInput.value.trim();
		const password = passwordInput.value;
		if (!username || !password) {
			setError("请输入用户名和密码");
			return;
		}

		submitButton.disabled = true;
		const label = submitButton.querySelector("span");
		if (label) {
			label.textContent = "登录中";
		}
		try {
			const response = await fetch("/api/user/login", {
				method: "POST",
				headers: {"Content-Type": "application/json"},
				body: JSON.stringify({username, password})
			});
			const result = await response.json();
			if (!result.success) {
				throw new Error(result.message || "登录失败");
			}
			window.location.assign("/console");
		} catch (error) {
			setError(error.message || "登录失败");
		} finally {
			submitButton.disabled = false;
			if (label) {
				label.textContent = "登录";
			}
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
