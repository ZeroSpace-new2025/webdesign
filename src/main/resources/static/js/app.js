(function () {
	"use strict";

	// 页面数据全部来自后端接口，前端只维护当前筛选和选中月份。
	const state = {
		users: [],
		roles: [],
		permissions: [],
		months: [],
		monthlyReport: null,
		employeeReports: [],
		selectedMonth: currentMonth(),
		currentView: "overview"
	};

	const viewMeta = {
		overview: {title: "数据概览", eyebrow: "USER & REPORTING"},
		users: {title: "用户管理", eyebrow: "EMPLOYEE DIRECTORY"},
		roles: {title: "角色权限", eyebrow: "ACCESS CONTROL"},
		reports: {title: "财务报表", eyebrow: "FINANCIAL REPORT"},
		audit: {title: "消费审计", eyebrow: "CONSUMPTION AUDIT"}
	};

	const elements = {
		pageTitle: document.getElementById("page-title"),
		pageEyebrow: document.getElementById("page-eyebrow"),
		todayLabel: document.getElementById("today-label"),
		userTableBody: document.getElementById("user-table-body"),
		userResultCount: document.getElementById("user-result-count"),
		roleTableBody: document.getElementById("role-table-body"),
		reportTableBody: document.getElementById("report-table-body"),
		auditTableBody: document.getElementById("audit-table-body"),
		userDialog: document.getElementById("user-dialog"),
		roleDialog: document.getElementById("role-dialog"),
		importDialog: document.getElementById("import-dialog"),
		auditDialog: document.getElementById("audit-dialog"),
		userForm: document.getElementById("user-form"),
		roleForm: document.getElementById("role-form"),
		importForm: document.getElementById("import-form")
	};

	// ---------- 通用格式化与请求工具 ----------

	function currentMonth() {
		const date = new Date();
		return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
	}

	function money(value) {
		const amount = Number(value || 0);
		return `¥${amount.toFixed(2)}`;
	}

	function dateTime(value) {
		if (!value) {
			return "--";
		}
		return new Intl.DateTimeFormat("zh-CN", {
			month: "2-digit",
			day: "2-digit",
			hour: "2-digit",
			minute: "2-digit"
		}).format(new Date(Number(value)));
	}

	function monthLabel(month) {
		if (!month) {
			return "--";
		}
		const parts = String(month).split("-");
		return `${parts[0]}年${Number(parts[1])}月`;
	}

	function escapeHtml(value) {
		return String(value == null ? "" : value)
			.replaceAll("&", "&amp;")
			.replaceAll("<", "&lt;")
			.replaceAll(">", "&gt;")
			.replaceAll('"', "&quot;")
			.replaceAll("'", "&#039;");
	}

	function refreshIcons() {
		if (window.lucide) {
			window.lucide.createIcons();
		}
	}

	async function api(url, options) {
		const config = Object.assign({}, options || {});
		config.headers = Object.assign({}, config.headers || {});
		if (config.body && !(config.body instanceof FormData)) {
			config.headers["Content-Type"] = "application/json";
			if (typeof config.body !== "string") {
				config.body = JSON.stringify(config.body);
			}
		}
		const response = await fetch(url, config);
		const result = await response.json();
		if (!result.success) {
			throw new Error(result.message || "请求失败");
		}
		return result.data;
	}

	function toast(message, isError) {
		const container = document.getElementById("toast-container");
		const item = document.createElement("div");
		item.className = `toast${isError ? " is-error" : ""}`;
		item.innerHTML = `
			<i data-lucide="${isError ? "circle-alert" : "circle-check"}"></i>
			<span>${escapeHtml(message)}</span>`;
		container.appendChild(item);
		refreshIcons();
		window.setTimeout(() => item.remove(), 3200);
	}

	function handleError(error) {
		toast(error.message || "操作失败", true);
	}

	// ---------- 视图与基础数据 ----------

	function roleMap() {
		return new Map(state.roles.map((role) => [Number(role.roleId), role]));
	}

	function userRoles(user) {
		const roles = roleMap();
		return (user.roleIds || []).map((id) => roles.get(Number(id))).filter(Boolean);
	}

	function setView(view) {
		state.currentView = view;
		document.querySelectorAll(".nav-item").forEach((button) => {
			button.classList.toggle("is-active", button.dataset.view === view);
		});
		document.querySelectorAll(".content-view").forEach((section) => {
			section.classList.toggle("is-active", section.id === `view-${view}`);
		});
		const meta = viewMeta[view] || viewMeta.overview;
		elements.pageTitle.textContent = meta.title;
		elements.pageEyebrow.textContent = meta.eyebrow;
	}

	async function loadAll() {
		try {
			const [roles, permissions, users, months] = await Promise.all([
				api("/api/role"),
				api("/api/role/permission"),
				api("/api/user/query", {method: "POST", body: {}}),
				api("/api/report/months")
			]);
			state.roles = roles || [];
			state.permissions = permissions || [];
			state.users = users || [];
			state.months = months && months.length ? months : [state.selectedMonth];
			state.selectedMonth = state.months[0] || state.selectedMonth;
			populateSelections();
			renderAll();
			await Promise.all([loadMonthlyReport(state.selectedMonth), loadAuditReports(state.selectedMonth)]);
		} catch (error) {
			handleError(error);
		}
	}

	function populateSelections() {
		// 筛选下拉、角色多选框和月份选择器都由接口数据动态生成。
		const departmentFilter = document.getElementById("user-department-filter");
		const departments = [...new Set(state.users.map((user) => user.department).filter(Boolean))].sort();
		departmentFilter.innerHTML = `<option value="">全部部门</option>${departments
			.map((department) => `<option value="${escapeHtml(department)}">${escapeHtml(department)}</option>`)
			.join("")}`;

		const roleFilter = document.getElementById("user-role-filter");
		roleFilter.innerHTML = `<option value="">全部角色</option>${state.roles
			.map((role) => `<option value="${role.roleId}">${escapeHtml(role.name)}</option>`)
			.join("")}`;

		document.getElementById("user-role-options").innerHTML = state.roles
			.map((role) => `
				<label class="check-option">
					<input type="checkbox" name="user-role" value="${role.roleId}">
					<span><strong>${escapeHtml(role.name)}</strong><small>${escapeHtml(role.code)}</small></span>
				</label>`)
			.join("");

		document.getElementById("role-permission-options").innerHTML = state.permissions
			.map((permission) => `
				<label class="check-option">
					<input type="checkbox" name="role-permission" value="${escapeHtml(permission.code)}">
					<span><strong>${escapeHtml(permission.name)}</strong><small>${escapeHtml(permission.description)}</small></span>
				</label>`)
			.join("");

		const monthOptions = state.months
			.map((month) => `<option value="${escapeHtml(month)}">${monthLabel(month)}</option>`)
			.join("");
		document.getElementById("report-month").innerHTML = monthOptions;
		document.getElementById("audit-month").innerHTML = monthOptions;
		document.getElementById("report-month").value = state.selectedMonth;
		document.getElementById("audit-month").value = state.selectedMonth;
	}

	function renderAll() {
		renderOverview();
		renderUsers();
		renderRoles();
		document.getElementById("nav-user-count").textContent = String(state.users.length);
		refreshIcons();
	}

	function renderOverview() {
		// 概览同时使用用户统计和当前月度报表数据。
		document.getElementById("metric-users").textContent = String(state.users.length);
		document.getElementById("metric-active").textContent = String(
			state.users.filter((user) => user.enabled).length
		);
		document.getElementById("metric-roles").textContent = String(state.roles.length);
		document.getElementById("metric-revenue").textContent = money(state.monthlyReport?.totalAmount);
		document.getElementById("overview-month").textContent = monthLabel(state.monthlyReport?.month);

		const enabled = state.users.filter((user) => user.enabled).length;
		document.getElementById("overview-enabled").textContent = String(enabled);
		document.getElementById("overview-disabled").textContent = String(state.users.length - enabled);

		const dishList = document.getElementById("overview-dish-list");
		const dishes = state.monthlyReport?.items || [];
		const maxQuantity = Math.max(...dishes.map((item) => Number(item.quantity || 0)), 1);
		dishList.innerHTML = dishes.length
			? dishes.slice(0, 6).map((item, index) => `
				<div class="ranking-item">
					<span class="ranking-index">${String(index + 1).padStart(2, "0")}</span>
					<div class="ranking-main">
						<div>
							<strong>${escapeHtml(item.recipeName)}</strong>
							<small>${escapeHtml(item.category)}</small>
						</div>
						<div class="progress-track"><span style="width:${Math.round(Number(item.quantity) / maxQuantity * 100)}%"></span></div>
					</div>
					<div class="ranking-value">
						<strong>${Number(item.quantity)}</strong>
						<small>${money(item.salesAmount)}</small>
					</div>
				</div>`).join("")
			: `<div class="empty-state">暂无销售数据</div>`;

		const departmentCounts = state.users.reduce((result, user) => {
			const department = user.department || "未分配";
			result.set(department, (result.get(department) || 0) + 1);
			return result;
		}, new Map());
		const maxDepartment = Math.max(...departmentCounts.values(), 1);
		document.getElementById("overview-departments").innerHTML = departmentCounts.size
			? [...departmentCounts.entries()]
				.sort((a, b) => b[1] - a[1])
				.map(([department, count]) => `
					<div class="department-row">
						<span>${escapeHtml(department)}</span>
						<div class="progress-track"><span style="width:${Math.round(count / maxDepartment * 100)}%"></span></div>
						<strong>${count}</strong>
					</div>`)
				.join("")
			: `<div class="empty-state">暂无员工数据</div>`;
	}

	function filteredUsers() {
		// 用户列表在前端即时筛选，避免每次输入都请求后端。
		const keyword = document.getElementById("user-search").value.trim().toLowerCase();
		const department = document.getElementById("user-department-filter").value;
		const status = document.getElementById("user-status-filter").value;
		const roleId = document.getElementById("user-role-filter").value;
		return state.users.filter((user) => {
			const haystack = [user.username, user.name, user.workstation, user.phone]
				.filter(Boolean).join(" ").toLowerCase();
			const matchesKeyword = !keyword || haystack.includes(keyword);
			const matchesDepartment = !department || user.department === department;
			const matchesStatus = status === "" || String(Boolean(user.enabled)) === status;
			const matchesRole = !roleId || (user.roleIds || []).map(Number).includes(Number(roleId));
			return matchesKeyword && matchesDepartment && matchesStatus && matchesRole;
		});
	}

	function renderUsers() {
		const users = filteredUsers();
		elements.userResultCount.textContent = `共 ${users.length} 条`;
		if (!users.length) {
			elements.userTableBody.innerHTML = `<tr><td colspan="6"><div class="empty-state">没有符合条件的员工</div></td></tr>`;
			return;
		}
		elements.userTableBody.innerHTML = users.map((user) => {
			const roles = userRoles(user);
			return `
				<tr>
					<td>
						<div class="employee-cell">
							<span class="avatar">${escapeHtml((user.name || "?").slice(0, 1))}</span>
							<div><strong>${escapeHtml(user.name)}</strong><small>${escapeHtml(user.username)}</small></div>
						</div>
					</td>
					<td>${escapeHtml(user.department || "--")}<span class="secondary-line">${escapeHtml(user.workstation || "--")}</span></td>
					<td>${escapeHtml(user.phone || "--")}</td>
					<td><div class="role-list">${roles.length
						? roles.map((role) => `<span class="role-chip">${escapeHtml(role.name)}</span>`).join("")
						: `<span class="secondary-line">未分配</span>`}</div></td>
					<td><span class="status-tag ${user.enabled ? "status-tag-success" : "status-tag-danger"}">
						${user.enabled ? "正常" : "已停用"}
					</span></td>
					<td>
						<div class="row-actions">
							<button class="icon-button" type="button" title="编辑" data-user-action="edit" data-user-id="${user.userId}">
								<i data-lucide="pencil"></i>
							</button>
							<button class="icon-button" type="button" title="${user.enabled ? "停用" : "启用"}"
								data-user-action="toggle" data-user-id="${user.userId}" data-enabled="${user.enabled}">
								<i data-lucide="${user.enabled ? "user-x" : "user-check"}"></i>
							</button>
							<button class="icon-button" type="button" title="删除" data-user-action="delete" data-user-id="${user.userId}">
								<i data-lucide="trash-2"></i>
							</button>
						</div>
					</td>
				</tr>`;
		}).join("");
		refreshIcons();
	}

	function renderRoles() {
		if (!state.roles.length) {
			elements.roleTableBody.innerHTML = `<tr><td colspan="4"><div class="empty-state">暂无角色</div></td></tr>`;
			return;
		}
		const permissionNames = new Map(state.permissions.map((permission) => [permission.code, permission.name]));
		elements.roleTableBody.innerHTML = state.roles.map((role) => `
			<tr>
				<td><strong>${escapeHtml(role.name)}</strong><span class="secondary-line">${escapeHtml(role.description || "--")}</span></td>
				<td><span class="status-tag status-tag-neutral">${escapeHtml(role.code)}</span></td>
				<td><div class="permission-list">${(role.permissionCodes || []).length
					? role.permissionCodes.map((code) => `<span class="permission-chip">${escapeHtml(permissionNames.get(code) || code)}</span>`).join("")
					: `<span class="secondary-line">无业务权限</span>`}</div></td>
				<td>
					<div class="row-actions">
						<button class="icon-button" type="button" title="编辑" data-role-action="edit" data-role-id="${role.roleId}">
							<i data-lucide="pencil"></i>
						</button>
						<button class="icon-button" type="button" title="删除" data-role-action="delete" data-role-id="${role.roleId}">
							<i data-lucide="trash-2"></i>
						</button>
					</div>
				</td>
			</tr>`).join("");
		refreshIcons();
	}

	async function loadMonthlyReport(month) {
		try {
			state.monthlyReport = await api(`/api/report/monthly?month=${encodeURIComponent(month)}`);
			renderMonthlyReport();
			renderOverview();
		} catch (error) {
			handleError(error);
		}
	}

	function renderMonthlyReport() {
		// 销售占比以菜品数量为基准，突出高频菜品。
		const report = state.monthlyReport || {};
		const items = report.items || [];
		document.getElementById("report-orders").textContent = String(report.orderCount || 0);
		document.getElementById("report-quantity").textContent = String(report.totalQuantity || 0);
		document.getElementById("report-amount").textContent = money(report.totalAmount);
		const totalQuantity = Math.max(Number(report.totalQuantity || 0), 1);
		elements.reportTableBody.innerHTML = items.length
			? items.map((item) => {
				const ratio = Math.round(Number(item.quantity) / totalQuantity * 100);
				return `
					<tr>
						<td><strong>${escapeHtml(item.recipeName)}</strong></td>
						<td>${escapeHtml(item.category || "--")}</td>
						<td>${Number(item.quantity)}</td>
						<td>
							<div class="sale-bar-cell">
								<div class="progress-track"><span style="width:${ratio}%"></span></div>
								<span>${ratio}%</span>
							</div>
						</td>
						<td class="align-right">${money(item.salesAmount)}</td>
					</tr>`;
			}).join("")
			: `<tr><td colspan="5"><div class="empty-state">该月份暂无销售数据</div></td></tr>`;
	}

	async function loadAuditReports(month) {
		try {
			state.employeeReports = await api(`/api/report/employees?month=${encodeURIComponent(month)}`);
			renderAuditReports();
		} catch (error) {
			handleError(error);
		}
	}

	function filteredAuditReports() {
		const keyword = document.getElementById("audit-search").value.trim().toLowerCase();
		return state.employeeReports.filter((report) => {
			if (!keyword) {
				return true;
			}
			return [report.employeeName, report.department, report.workstation]
				.filter(Boolean).join(" ").toLowerCase().includes(keyword);
		});
	}

	function renderAuditReports() {
		const reports = filteredAuditReports();
		elements.auditTableBody.innerHTML = reports.length
			? reports.map((report) => `
				<tr data-audit-user-id="${report.userId}">
					<td>
						<div class="employee-cell">
							<span class="avatar">${escapeHtml((report.employeeName || "?").slice(0, 1))}</span>
							<div><strong>${escapeHtml(report.employeeName)}</strong><small>订单 ${report.orderCount} 笔</small></div>
						</div>
					</td>
					<td>${escapeHtml(report.department || "--")}<span class="secondary-line">${escapeHtml(report.workstation || "--")}</span></td>
					<td>${report.orderCount}</td>
					<td>${report.totalQuantity}</td>
					<td class="align-right"><strong>${money(report.totalAmount)}</strong></td>
				</tr>`).join("")
			: `<tr><td colspan="5"><div class="empty-state">没有符合条件的消费记录</div></td></tr>`;
	}

	function openAuditDetail(userId) {
		// 员工汇总数据中已经带有订单明细，点击表格行即可直接展示。
		const report = state.employeeReports.find((item) => Number(item.userId) === Number(userId));
		if (!report) {
			return;
		}
		document.getElementById("audit-dialog-title").textContent = `${report.employeeName} · ${monthLabel(report.month)}`;
		const details = report.details || [];
		document.getElementById("audit-detail-content").innerHTML = details.length
			? details.map((order) => `
				<article class="audit-order">
					<header>
						<strong>订单 #${order.orderId}</strong>
						<span>${dateTime(order.createdTime)} · ${money(order.totalAmount)}</span>
					</header>
					<ul>${(order.items || []).map((item) => `
						<li>${escapeHtml(item.recipeName)} × ${item.quantity}，${money(item.amount)}</li>`).join("")}</ul>
				</article>`).join("")
			: `<div class="empty-state">该员工本月暂无消费记录</div>`;
		elements.auditDialog.showModal();
		refreshIcons();
	}

	function openUserDialog(user) {
		elements.userForm.reset();
		document.getElementById("user-form-error").textContent = "";
		const passwordField = document.getElementById("user-password-field");
		if (user) {
			document.getElementById("user-dialog-title").textContent = "编辑员工";
			document.getElementById("user-id").value = user.userId;
			document.getElementById("user-username").value = user.username || "";
			document.getElementById("user-username").disabled = true;
			document.getElementById("user-name").value = user.name || "";
			document.getElementById("user-department").value = user.department || "";
			document.getElementById("user-workstation").value = user.workstation || "";
			document.getElementById("user-phone").value = user.phone || "";
			passwordField.hidden = true;
			const roleIds = new Set((user.roleIds || []).map(Number));
			document.querySelectorAll('input[name="user-role"]').forEach((checkbox) => {
				checkbox.checked = roleIds.has(Number(checkbox.value));
			});
		} else {
			document.getElementById("user-dialog-title").textContent = "新增员工";
			document.getElementById("user-id").value = "";
			document.getElementById("user-username").disabled = false;
			document.getElementById("user-password").value = "123456";
			passwordField.hidden = false;
			document.querySelectorAll('input[name="user-role"]').forEach((checkbox) => {
				checkbox.checked = Number(checkbox.value) === 5;
			});
		}
		elements.userDialog.showModal();
		refreshIcons();
	}

	async function submitUser(event) {
		// 编辑时不修改用户名和密码，新增时才提交初始密码。
		event.preventDefault();
		const errorElement = document.getElementById("user-form-error");
		errorElement.textContent = "";
		const userId = document.getElementById("user-id").value;
		const roleIds = [...document.querySelectorAll('input[name="user-role"]:checked')].map((input) => Number(input.value));
		try {
			if (userId) {
				await api("/api/user", {
					method: "PUT",
					body: {
						userId: Number(userId),
						name: document.getElementById("user-name").value,
						department: document.getElementById("user-department").value,
						workstation: document.getElementById("user-workstation").value,
						phone: document.getElementById("user-phone").value,
						roleIds
					}
				});
				toast("员工信息已更新");
			} else {
				await api("/api/user/register", {
					method: "POST",
					body: {
						username: document.getElementById("user-username").value,
						password: document.getElementById("user-password").value,
						name: document.getElementById("user-name").value,
						department: document.getElementById("user-department").value,
						workstation: document.getElementById("user-workstation").value,
						phone: document.getElementById("user-phone").value,
						roleIds
					}
				});
				toast("员工账号已创建");
			}
			elements.userDialog.close();
			await reloadUsers();
		} catch (error) {
			errorElement.textContent = error.message;
		}
	}

	function openRoleDialog(role) {
		elements.roleForm.reset();
		document.getElementById("role-form-error").textContent = "";
		if (role) {
			document.getElementById("role-dialog-title").textContent = "编辑角色";
			document.getElementById("role-id").value = role.roleId;
			document.getElementById("role-code").value = role.code || "";
			document.getElementById("role-name").value = role.name || "";
			document.getElementById("role-description").value = role.description || "";
			const permissionCodes = new Set(role.permissionCodes || []);
			document.querySelectorAll('input[name="role-permission"]').forEach((checkbox) => {
				checkbox.checked = permissionCodes.has(checkbox.value);
			});
		} else {
			document.getElementById("role-dialog-title").textContent = "新增角色";
			document.getElementById("role-id").value = "";
			document.querySelectorAll('input[name="role-permission"]').forEach((checkbox) => {
				checkbox.checked = false;
			});
		}
		elements.roleDialog.showModal();
		refreshIcons();
	}

	async function submitRole(event) {
		// 角色基础信息和权限分开保存，便于后续单独扩展权限策略。
		event.preventDefault();
		const errorElement = document.getElementById("role-form-error");
		errorElement.textContent = "";
		const roleId = document.getElementById("role-id").value;
		const permissionCodes = [...document.querySelectorAll('input[name="role-permission"]:checked')]
			.map((input) => input.value);
		const payload = {
			roleId: roleId ? Number(roleId) : null,
			code: document.getElementById("role-code").value.trim().toUpperCase(),
			name: document.getElementById("role-name").value.trim(),
			description: document.getElementById("role-description").value.trim(),
			permissionCodes
		};
		try {
			const savedRole = roleId
				? await api("/api/role", {method: "PUT", body: payload})
				: await api("/api/role", {method: "POST", body: payload});
			if (roleId) {
				await api(`/api/role/${savedRole.roleId}/permission`, {method: "PUT", body: permissionCodes});
			}
			toast(roleId ? "角色已更新" : "角色已创建");
			elements.roleDialog.close();
			await reloadRoles();
		} catch (error) {
			errorElement.textContent = error.message;
		}
	}

	async function reloadUsers() {
		state.users = await api("/api/user/query", {method: "POST", body: {}});
		populateSelections();
		renderUsers();
		renderOverview();
		document.getElementById("nav-user-count").textContent = String(state.users.length);
	}

	async function reloadRoles() {
		[state.roles, state.permissions] = await Promise.all([
			api("/api/role"),
			api("/api/role/permission")
		]);
		populateSelections();
		renderRoles();
		renderUsers();
		renderOverview();
	}

	async function handleUserAction(button) {
		const userId = Number(button.dataset.userId);
		const action = button.dataset.userAction;
		const user = state.users.find((item) => Number(item.userId) === userId);
		if (!user) {
			return;
		}
		if (action === "edit") {
			openUserDialog(user);
			return;
		}
		if (action === "toggle") {
			try {
				await api(`/api/user/${userId}/status`, {
					method: "PATCH",
					body: {enabled: !user.enabled}
				});
				toast(user.enabled ? "账号已停用" : "账号已启用");
				await reloadUsers();
			} catch (error) {
				handleError(error);
			}
			return;
		}
		if (action === "delete" && window.confirm(`确认删除员工“${user.name}”？`)) {
			try {
				await api(`/api/user/${userId}`, {method: "DELETE"});
				toast("员工已删除");
				await reloadUsers();
			} catch (error) {
				handleError(error);
			}
		}
	}

	async function handleRoleAction(button) {
		const roleId = Number(button.dataset.roleId);
		const action = button.dataset.roleAction;
		const role = state.roles.find((item) => Number(item.roleId) === roleId);
		if (!role) {
			return;
		}
		if (action === "edit") {
			openRoleDialog(role);
			return;
		}
		if (action === "delete" && window.confirm(`确认删除角色“${role.name}”？`)) {
			try {
				await api(`/api/role/${roleId}`, {method: "DELETE"});
				toast("角色已删除");
				await reloadRoles();
			} catch (error) {
				handleError(error);
			}
		}
	}

	async function submitImport(event) {
		// 使用FormData上传，Service层负责解析CSV或XLSX。
		event.preventDefault();
		const errorElement = document.getElementById("import-form-error");
		const resultElement = document.getElementById("import-result");
		errorElement.textContent = "";
		const fileInput = document.getElementById("import-file");
		if (!fileInput.files.length) {
			errorElement.textContent = "请选择导入文件";
			return;
		}
		const formData = new FormData();
		formData.append("file", fileInput.files[0]);
		try {
			const result = await api("/api/user/import", {method: "POST", body: formData});
			resultElement.innerHTML = `
				<strong>导入完成：成功 ${result.successCount} 条，失败 ${result.failureCount} 条</strong>
				${(result.errors || []).length
					? `<ul>${result.errors.map((error) => `<li>第 ${error.rowNumber} 行：${escapeHtml(error.message)}</li>`).join("")}</ul>`
					: ""}`;
			toast("员工数据导入完成");
			await reloadUsers();
		} catch (error) {
			errorElement.textContent = error.message;
		}
	}

	function bindEvents() {
		// 使用事件委托处理动态生成的表格行按钮。
		document.querySelectorAll(".nav-item").forEach((button) => {
			button.addEventListener("click", () => setView(button.dataset.view));
		});

		document.querySelectorAll(".dialog-close").forEach((button) => {
			button.addEventListener("click", () => button.closest("dialog").close());
		});

		["user-search", "user-department-filter", "user-status-filter", "user-role-filter"].forEach((id) => {
			document.getElementById(id).addEventListener("input", renderUsers);
			document.getElementById(id).addEventListener("change", renderUsers);
		});

		document.getElementById("audit-search").addEventListener("input", renderAuditReports);

		document.getElementById("open-user-create").addEventListener("click", () => openUserDialog(null));
		document.getElementById("open-role-create").addEventListener("click", () => openRoleDialog(null));
		document.getElementById("open-import").addEventListener("click", () => {
			elements.importForm.reset();
			document.getElementById("import-result").innerHTML = "";
			document.getElementById("import-form-error").textContent = "";
			document.getElementById("import-file-name").textContent = "支持 .xlsx、.csv";
			elements.importDialog.showModal();
			refreshIcons();
		});
		document.getElementById("download-template").addEventListener("click", () => {
			window.location.assign("/api/user/import/template");
		});

		elements.userForm.addEventListener("submit", submitUser);
		elements.roleForm.addEventListener("submit", submitRole);
		elements.importForm.addEventListener("submit", submitImport);

		elements.userTableBody.addEventListener("click", (event) => {
			const button = event.target.closest("[data-user-action]");
			if (button) {
				handleUserAction(button);
			}
		});
		elements.roleTableBody.addEventListener("click", (event) => {
			const button = event.target.closest("[data-role-action]");
			if (button) {
				handleRoleAction(button);
			}
		});
		elements.auditTableBody.addEventListener("click", (event) => {
			const row = event.target.closest("[data-audit-user-id]");
			if (row) {
				openAuditDetail(row.dataset.auditUserId);
			}
		});

		document.getElementById("import-file").addEventListener("change", (event) => {
			const file = event.target.files[0];
			document.getElementById("import-file-name").textContent = file ? file.name : "支持 .xlsx、.csv";
		});

		document.getElementById("report-month").addEventListener("change", async (event) => {
			state.selectedMonth = event.target.value;
			await loadMonthlyReport(state.selectedMonth);
		});
		document.getElementById("audit-month").addEventListener("change", async (event) => {
			state.selectedMonth = event.target.value;
			document.getElementById("report-month").value = state.selectedMonth;
			await Promise.all([
				loadMonthlyReport(state.selectedMonth),
				loadAuditReports(state.selectedMonth)
			]);
		});
		document.getElementById("refresh-report").addEventListener("click", async () => {
			try {
				await api(`/api/report/refresh?month=${encodeURIComponent(state.selectedMonth)}`, {method: "POST"});
				await Promise.all([
					loadMonthlyReport(state.selectedMonth),
					loadAuditReports(state.selectedMonth)
				]);
				toast("报表缓存已刷新");
			} catch (error) {
				handleError(error);
			}
		});
		document.getElementById("print-report").addEventListener("click", () => window.print());

		document.getElementById("logout-button").addEventListener("click", async () => {
			try {
				await api("/api/user/logout", {method: "POST"});
			} catch (ignored) {
				// 无论请求结果如何都返回登录页。
			}
			window.location.assign("/login");
		});
	}

	function initialize() {
		// 页面先加载共享数据，再并行请求月报和消费审计。
		elements.todayLabel.textContent = new Intl.DateTimeFormat("zh-CN", {dateStyle: "full"}).format(new Date());
		bindEvents();
		loadAll();
		refreshIcons();
	}

	initialize();
})();
