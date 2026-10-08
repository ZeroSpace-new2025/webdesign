(function () {
	"use strict";

	// 登录页只负责收集凭据、调用接口、保存 token 并跳转到控制台。
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

	// token 同时写入 localStorage（JSON 接口用 Authorization 头）与 Cookie
	// （HTML 链接无法自定义请求头，页面控制器需要凭 Cookie 拿到登录态）。
	function saveToken(token) {
		if (!token) {
			return;
		}
		window.localStorage.setItem("dsh_token", token);
		document.cookie = "dsh_token=" + encodeURIComponent(token) + "; path=/; max-age=43200; SameSite=Lax";
	}

	async function submitLogin(event) {
		event.preventDefault();
		setError("");
		const employeeNo = usernameInput.value.trim();
		const password = passwordInput.value;
		if (!employeeNo || !password) {
			setError("请输入工号和密码");
			return;
		}

		submitButton.disabled = true;
		const label = submitButton.querySelector("span");
		if (label) {
			label.textContent = "登录中";
		}
		try {
			const response = await fetch("/api/v1/user/auth/login", {
				method: "POST",
				headers: {"Content-Type": "application/json"},
				body: JSON.stringify({employeeNo, password})
			});
			const result = await response.json();
			if (!result.success) {
				throw new Error(result.message || "登录失败");
			}
			saveToken(result.data && result.data.token);
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
			usernameInput.value = button.dataset.employeeNo || button.dataset.username || "";
			passwordInput.value = button.dataset.password || "";
			usernameInput.focus();
			setError("");
		});
	});

	form.addEventListener("submit", submitLogin);
	refreshIcons();
})();
