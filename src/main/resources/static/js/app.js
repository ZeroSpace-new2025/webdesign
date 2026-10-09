(function () {
	"use strict";

	// 页面数据全部来自后端接口，前端只维护当前筛选和选中月份。
	const state = {
		users: [],
		userTotal: 0,
		roles: [],
		permissions: [],
		months: [],
		monthlyReport: null,
		deptReports: [],
		editingUser: null,
		selectedMonth: currentMonth(),
		currentView: "overview"
	};

	// /console 注入的权限点与角色（与后端 @RequiresPerm 保持一致），用于决定视图与请求。
	const config = window.APP_CONFIG || {};
	const permCodes = new Set(Array.isArray(config.permCodes) ? config.permCodes : []);
	const roleCodes = new Set(Array.isArray(config.roleCodes) ? config.roleCodes : []);

	// 预置角色（与后端 common/RoleCodes.ALL 一致）：名称即角色身份，不可改名、不可删除。
	const presetRoleNames = new Set([
		"MANAGER", "KITCHEN_SUPERVISOR", "DELIVERY_STAFF", "FINANCE", "EMPLOYEE"
	]);

	const viewMeta = {
		overview: {title: "数据概览", eyebrow: "USER & REPORTING"},
		users: {title: "员工管理", eyebrow: "EMPLOYEE DIRECTORY"},
		roles: {title: "角色权限", eyebrow: "ACCESS CONTROL"},
		reports: {title: "财务报表", eyebrow: "FINANCIAL REPORT"},
		audit: {title: "消费审计", eyebrow: "CONSUMPTION AUDIT"}
	};

	// ---------- DOM 取值/赋值工具（视图被 th:if 隐藏时元素不存在，全部做空值保护） ----------

	function byId(id) {
		return document.getElementById(id);
	}

	function valueOf(id) {
		const node = byId(id);
		return node ? String(node.value == null ? "" : node.value) : "";
	}

	function setText(id, value) {
		const node = byId(id);
		if (node) {
			node.textContent = value;
		}
	}

	function setHtml(id, html) {
		const node = byId(id);
		if (node) {
			node.innerHTML = html;
		}
	}

	function on(id, event, handler) {
		const node = byId(id);
		if (node) {
			node.addEventListener(event, handler);
		}
	}

	function refreshIcons() {
		if (window.lucide) {
			window.lucide.createIcons();
		}
	}

	function openDialog(id) {
		const node = byId(id);
		if (node && typeof node.showModal === "function") {
			node.showModal();
		}
		refreshIcons();
	}

	function closeDialog(id) {
		const node = byId(id);
		if (node) {
			node.close();
		}
	}

	// ---------- 权限判定 ----------

	function can(permCode) {
		return permCodes.has(permCode);
	}

	// 消费审计接口允许 audit:view 或 FINANCE 角色（见 ApiReportController）。
	function canAudit() {
		return can("audit:view") || roleCodes.has("FINANCE");
	}

	// 角色字典接口需要 role:manage；拿不到角色时不要提交 roleIds，否则会把已有角色清空。
	function canAssignRoles() {
		return can("role:manage") && document.querySelectorAll('input[name="user-role"]').length > 0;
	}

	// ---------- 通用格式化与请求工具 ----------

	function currentMonth() {
		const date = new Date();
		return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
	}

	// 后端未提供月份列表接口时的降级：最近 12 个月（含本月），倒序。
	function recentMonths(count) {
		const months = [];
		const now = new Date();
		for (let index = 0; index < (count || 12); index += 1) {
			const date = new Date(now.getFullYear(), now.getMonth() - index, 1);
			months.push(`${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`);
		}
		return months;
	}

	function money(value) {
		const amount = Number(value || 0);
		return `¥${amount.toFixed(2)}`;
	}

	function dateTime(value) {
		if (value == null || value === "") {
			return "--";
		}
		const text = String(value).trim();
		if (/^\d+$/.test(text)) {
			return new Intl.DateTimeFormat("zh-CN", {
				month: "2-digit",
				day: "2-digit",
				hour: "2-digit",
				minute: "2-digit"
			}).format(new Date(Number(text)));
		}
		return text.replace("T", " ").slice(0, 16);
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

	// 统一分页返回体 PageResult{total, list}；兼容不分页时直接返回数组的情况。
	function pageList(page) {
		if (Array.isArray(page)) {
			return page;
		}
		return page && Array.isArray(page.list) ? page.list : [];
	}

	function pageTotal(page, fallback) {
		return page && typeof page.total === "number" ? page.total : fallback;
	}

	async function api(url, options) {
		const request = Object.assign({}, options || {});
		request.headers = Object.assign({}, request.headers || {});
		if (request.body && !(request.body instanceof FormData)) {
			request.headers["Content-Type"] = "application/json";
			if (typeof request.body !== "string") {
				request.body = JSON.stringify(request.body);
			}
		}
		// 登录后签发的 JWT：扁平接口一律带 Authorization 头，服务端不再接受自报身份
		const token = window.localStorage.getItem("dsh_token");
		if (token) {
			request.headers["Authorization"] = "Bearer " + token;
		}
		const response = await fetch(url, request);
		if (response.status === 401) {
			window.location.assign("/login");
			throw new Error("登录已失效，请重新登录");
		}
		let result;
		try {
			result = await response.json();
		} catch (ignored) {
			throw new Error(`请求失败（HTTP ${response.status}）`);
		}
		if (!result.success) {
			throw new Error(result.message || "请求失败");
		}
		return result.data;
	}

	// 包一层 catch，避免单个接口失败导致整页初始化中断。
	function guard(task) {
		return Promise.resolve()
			.then(task)
			.catch((error) => handleError(error));
	}

	function toast(message, isError) {
		const container = byId("toast-container");
		if (!container) {
			return;
		}
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
		toast(error && error.message ? error.message : "操作失败", true);
	}

	// ---------- 视图与基础数据 ----------

	function roleMap() {
		return new Map(state.roles.map((role) => [Number(role.roleId), role]));
	}

	// UserVO 同时带 roles（RoleVO 列表）与 roleIds，优先用 roles，缺失时回退到本地角色表。
	function userRoles(user) {
		if (Array.isArray(user.roles) && user.roles.length) {
			return user.roles;
		}
		const roles = roleMap();
		return (user.roleIds || []).map((id) => roles.get(Number(id))).filter(Boolean);
	}

	function roleLabel(role) {
		return role.name || "--";
	}

	// 预置角色名按后端 RoleCodes 取值匹配，不依赖固定 ID。
	function isPresetRole(role) {
		return Boolean(role && presetRoleNames.has(String(role.name || "").toUpperCase()));
	}

	function statusClass(status) {
		if (status === "ACTIVE") {
			return "status-tag-success";
		}
		if (status === "DISABLED") {
			return "status-tag-danger";
		}
		return "status-tag-neutral";
	}

	const statusTextMap = {ACTIVE: "启用", DISABLED: "停用", LOCKED: "锁定"};

	function statusLabel(user) {
		return user.statusText || statusTextMap[user.status] || "--";
	}

	// 部门字典由员工数据的 deptId + deptName 去重派生（后端未提供部门字典接口）。
	function departments() {
		const map = new Map();
		state.users.forEach((user) => {
			if (user.deptId == null) {
				return;
			}
			const key = String(user.deptId);
			if (!map.has(key)) {
				map.set(key, {deptId: user.deptId, deptName: user.deptName || `部门 ${key}`});
			}
		});
		return [...map.values()].sort((left, right) => String(left.deptName).localeCompare(String(right.deptName), "zh-CN"));
	}

	function deptKey(entity) {
		return entity && entity.deptId != null ? String(entity.deptId) : "none";
	}

	function setView(view) {
		if (!viewMeta[view]) {
			return;
		}
		state.currentView = view;
		document.querySelectorAll(".nav-item").forEach((button) => {
			button.classList.toggle("is-active", button.dataset.view === view);
		});
		document.querySelectorAll(".content-view").forEach((section) => {
			section.classList.toggle("is-active", section.id === `view-${view}`);
		});
		const meta = viewMeta[view] || viewMeta.overview;
		setText("page-title", meta.title);
		setText("page-eyebrow", meta.eyebrow);
	}

	async function loadAll() {
		// 无权限的视图已被模板隐藏，这里也不要发必然 403 的请求。
		const bootstrap = [];
		if (can("user:manage")) {
			bootstrap.push(guard(loadUsers));
		}
		if (can("role:manage")) {
			bootstrap.push(guard(loadRoles));
			bootstrap.push(guard(loadPermissions));
		}
		if (can("report:view") || canAudit()) {
			bootstrap.push(guard(loadMonths));
		}
		await Promise.all(bootstrap);

		populateSelections();
		renderAll();

		// 月份与审计依赖上一步的基础数据，因此放到第二阶段。
		const detail = [];
		if (can("report:view")) {
			detail.push(guard(loadMonthlyReport));
		}
		if (canAudit()) {
			detail.push(guard(loadDeptReports));
		}
		await Promise.all(detail);
	}

	async function loadUsers() {
		const page = await api("/api/v1/user/users?pageNum=1&pageSize=100");
		state.users = pageList(page);
		state.userTotal = pageTotal(page, state.users.length);
	}

	async function loadRoles() {
		// 角色字典一次最多取 100 条（pageSize 上限），与角色页大小一致。
		const page = await api("/api/v1/user/roles?pageNum=1&pageSize=100");
		state.roles = pageList(page);
	}

	async function loadPermissions() {
		const permissions = await api("/api/v1/user/permissions");
		state.permissions = Array.isArray(permissions) ? permissions : [];
	}

	async function loadMonths() {
		try {
			// GET /reports/months 对 report:view 与 audit:view 都放行。
			const months = await api("/api/v1/user/reports/months");
			state.months = Array.isArray(months) && months.length ? months.map(String) : recentMonths();
		} catch (ignored) {
			// 接口不可用时降级为最近 12 个月，不打扰用户。
			state.months = recentMonths();
		}
		if (!state.months.includes(state.selectedMonth)) {
			state.selectedMonth = state.months[0] || state.selectedMonth;
		}
	}

	async function loadMonthlyReport() {
		state.monthlyReport = await api(`/api/v1/user/reports/monthly?month=${encodeURIComponent(state.selectedMonth)}`);
		renderMonthlyReport();
		renderOverview();
	}

	async function loadDeptReports() {
		const reports = await api(`/api/v1/user/reports/dept-consumption?month=${encodeURIComponent(state.selectedMonth)}`);
		state.deptReports = Array.isArray(reports) ? reports : [];
		renderDeptReports();
	}

	function populateSelections() {
		// 筛选下拉、角色多选框和月份选择器都由接口数据动态生成。
		const deptList = departments();
		const deptFilter = byId("user-department-filter");
		if (deptFilter) {
			deptFilter.innerHTML = `<option value="">全部部门</option>${deptList
				.map((dept) => `<option value="${escapeHtml(dept.deptId)}">${escapeHtml(dept.deptName)}</option>`)
				.join("")}`;
		}
		setHtml("dept-options", deptList
			.map((dept) => `<option value="${escapeHtml(dept.deptName)}"></option>`)
			.join(""));

		const roleFilter = byId("user-role-filter");
		if (roleFilter) {
			roleFilter.innerHTML = `<option value="">全部角色</option>${state.roles
				.map((role) => `<option value="${escapeHtml(role.roleId)}">${escapeHtml(roleLabel(role))}</option>`)
				.join("")}`;
		}

		setHtml("user-role-options", state.roles.length
			? state.roles.map((role) => `
				<label class="check-option">
					<input type="checkbox" name="user-role" value="${escapeHtml(role.roleId)}">
					<span><strong>${escapeHtml(roleLabel(role))}</strong></span>
				</label>`).join("")
			: `<span class="secondary-line">需要角色管理权限（role:manage）才能读取角色字典</span>`);

		const importDept = byId("import-dept");
		if (importDept) {
			importDept.innerHTML = `<option value="">不指定部门</option>${deptList
				.map((dept) => `<option value="${escapeHtml(dept.deptId)}">${escapeHtml(dept.deptName)}</option>`)
				.join("")}`;
		}
		setHtml("import-role-options", state.roles.length
			? state.roles.map((role) => `
				<label class="check-option">
					<input type="checkbox" name="import-role" value="${escapeHtml(role.roleId)}">
					<span><strong>${escapeHtml(roleLabel(role))}</strong></span>
				</label>`).join("")
			: `<span class="secondary-line">无可用角色（需要角色管理权限）</span>`);

		setHtml("role-permission-options", state.permissions
			.map((permission) => `
				<label class="check-option">
					<input type="checkbox" name="role-permission" value="${escapeHtml(permission.permCode)}">
					<span><strong>${escapeHtml(permission.permName)}</strong><small>${escapeHtml(permission.module || permission.description || "")}</small></span>
				</label>`)
			.join(""));

		const months = state.months.length ? state.months : [state.selectedMonth];
		const monthOptions = months
			.map((month) => `<option value="${escapeHtml(month)}">${monthLabel(month)}</option>`)
			.join("");
		["report-month", "audit-month"].forEach((id) => {
			const node = byId(id);
			if (node) {
				node.innerHTML = monthOptions;
				node.value = state.selectedMonth;
			}
		});
	}

	function renderAll() {
		renderOverview();
		renderUsers();
		renderRoles();
		setText("nav-user-count", String(state.users.length));
		refreshIcons();
	}

	function renderOverview() {
		// 概览同时使用用户统计和当前月度报表数据；无对应权限时相应区块不存在，取值为 --。
		if (can("user:manage")) {
			const active = state.users.filter((user) => user.status === "ACTIVE").length;
			setText("metric-users", String(state.users.length));
			setText("metric-active", String(active));
			setText("overview-enabled", String(active));
			setText("overview-disabled", String(Math.max(state.users.length - active, 0)));
		} else {
			setText("metric-users", "--");
			setText("metric-active", "--");
			setText("overview-enabled", "--");
			setText("overview-disabled", "--");
		}
		setText("metric-roles", can("role:manage") ? String(state.roles.length) : "--");
		setText("metric-revenue", can("report:view") ? money(state.monthlyReport && state.monthlyReport.totalAmount) : "--");
		setText("overview-month", can("report:view") ? monthLabel(state.monthlyReport && state.monthlyReport.month) : "--");

		const dishList = byId("overview-dish-list");
		if (dishList) {
			const dishes = (state.monthlyReport && state.monthlyReport.items) || [];
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
							<strong>${Number(item.quantity || 0)}</strong>
							<small>${money(item.amount)}</small>
						</div>
					</div>`).join("")
				: `<div class="empty-state">暂无销售数据</div>`;
		}

		const deptList = byId("overview-departments");
		if (deptList) {
			const departmentCounts = state.users.reduce((result, user) => {
				const department = user.deptName || "未分配";
				result.set(department, (result.get(department) || 0) + 1);
				return result;
			}, new Map());
			const maxDepartment = Math.max(...departmentCounts.values(), 1);
			deptList.innerHTML = departmentCounts.size
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
	}

	function filteredUsers() {
		// 员工列表在前端即时筛选，避免每次输入都请求后端。
		const keyword = valueOf("user-search").trim().toLowerCase();
		const deptId = valueOf("user-department-filter");
		const status = valueOf("user-status-filter");
		const roleId = valueOf("user-role-filter");
		return state.users.filter((user) => {
			const haystack = [user.employeeNo, user.name, user.workstation, user.phone]
				.filter(Boolean).join(" ").toLowerCase();
			const matchesKeyword = !keyword || haystack.includes(keyword);
			const matchesDept = !deptId || String(user.deptId) === String(deptId);
			const matchesStatus = !status || user.status === status;
			const matchesRole = !roleId || (user.roleIds || []).map(Number).includes(Number(roleId));
			return matchesKeyword && matchesDept && matchesStatus && matchesRole;
		});
	}

	function renderUsers() {
		const tableBody = byId("user-table-body");
		if (!tableBody) {
			return;
		}
		const users = filteredUsers();
		const totalHint = state.userTotal > state.users.length ? `（共 ${state.userTotal} 条，仅显示前 ${state.users.length} 条）` : "";
		setText("user-result-count", `共 ${users.length} 条${totalHint}`);
		if (!users.length) {
			tableBody.innerHTML = `<tr><td colspan="6"><div class="empty-state">没有符合条件的员工</div></td></tr>`;
			return;
		}
		tableBody.innerHTML = users.map((user) => {
			const roles = userRoles(user);
			const canToggle = user.status === "ACTIVE" || user.status === "DISABLED";
			return `
				<tr>
					<td>
						<div class="employee-cell">
							<span class="avatar">${escapeHtml((user.name || "?").slice(0, 1))}</span>
							<div><strong>${escapeHtml(user.name)}</strong><small>${escapeHtml(user.employeeNo || "--")}</small></div>
						</div>
					</td>
					<td>${escapeHtml(user.deptName || "--")}<span class="secondary-line">${escapeHtml(user.workstation || "--")}</span></td>
					<td>${escapeHtml(user.phone || "--")}</td>
					<td><div class="role-list">${roles.length
						? roles.map((role) => `<span class="role-chip">${escapeHtml(roleLabel(role))}</span>`).join("")
						: `<span class="secondary-line">未分配</span>`}</div></td>
					<td><span class="status-tag ${statusClass(user.status)}">
						${escapeHtml(statusLabel(user))}
					</span></td>
					<td>
						<div class="row-actions">
							<button class="icon-button" type="button" title="编辑" data-user-action="edit" data-user-id="${user.userId}">
								<i data-lucide="pencil"></i>
							</button>
							<button class="icon-button" type="button" title="${user.status === "ACTIVE" ? "停用" : "启用"}"
								data-user-action="toggle" data-user-id="${user.userId}" data-user-status="${escapeHtml(user.status || "")}"
								${canToggle ? "" : "disabled"}>
								<i data-lucide="${user.status === "ACTIVE" ? "user-x" : "user-check"}"></i>
							</button>
							<button class="icon-button" type="button" title="停用" data-user-action="disable" data-user-id="${user.userId}">
								<i data-lucide="user-x"></i>
							</button>
						</div>
					</td>
				</tr>`;
		}).join("");
		refreshIcons();
	}

	function renderRoles() {
		const tableBody = byId("role-table-body");
		if (!tableBody) {
			return;
		}
		if (!state.roles.length) {
			tableBody.innerHTML = `<tr><td colspan="3"><div class="empty-state">暂无角色</div></td></tr>`;
			return;
		}
		const permissionNames = new Map(state.permissions.map((permission) => [permission.permCode, permission.permName]));
		tableBody.innerHTML = state.roles.map((role) => {
			const permCodes = [...(role.permCodes || [])];
			return `
			<tr>
				<td><strong>${escapeHtml(roleLabel(role))}</strong>${isPresetRole(role)
					? `<span class="secondary-line">预置角色</span>` : ""}</td>
				<td><div class="permission-list">${permCodes.length
					? permCodes.map((code) => `<span class="permission-chip">${escapeHtml(permissionNames.get(code) || code)}</span>`).join("")
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
			</tr>`;
		}).join("");
		refreshIcons();
	}

	function renderMonthlyReport() {
		// 销售占比以菜品数量为基准，突出高频菜品。
		const report = state.monthlyReport || {};
		const items = report.items || [];
		setText("report-orders", String(report.orderCount || 0));
		setText("report-quantity", String(report.totalQuantity || 0));
		setText("report-amount", money(report.totalAmount));
		const totalQuantity = Math.max(Number(report.totalQuantity || 0), 1);
		setHtml("report-table-body", items.length
			? items.map((item) => {
				const ratio = Math.round(Number(item.quantity || 0) / totalQuantity * 100);
				return `
					<tr>
						<td><strong>${escapeHtml(item.recipeName)}</strong></td>
						<td>${escapeHtml(item.category || "--")}</td>
						<td>${Number(item.quantity || 0)}</td>
						<td>
							<div class="sale-bar-cell">
								<div class="progress-track"><span style="width:${ratio}%"></span></div>
								<span>${ratio}%</span>
							</div>
						</td>
						<td class="align-right">${money(item.amount)}</td>
					</tr>`;
			}).join("")
			: `<tr><td colspan="5"><div class="empty-state">该月份暂无销售数据</div></td></tr>`);
		refreshIcons();
	}

	function filteredDeptReports() {
		const keyword = valueOf("audit-search").trim().toLowerCase();
		if (!keyword) {
			return state.deptReports;
		}
		return state.deptReports.filter((report) =>
			[report.deptName, report.deptId].filter((value) => value != null && value !== "")
				.join(" ").toLowerCase().includes(keyword));
	}

	function renderDeptReports() {
		const tableBody = byId("audit-table-body");
		if (!tableBody) {
			return;
		}
		const reports = filteredDeptReports();
		tableBody.innerHTML = reports.length
			? reports.map((report) => `
				<tr data-audit-dept-id="${escapeHtml(deptKey(report))}">
					<td>
						<div class="employee-cell">
							<span class="avatar">${escapeHtml((report.deptName || "?").slice(0, 1))}</span>
							<div>
								<strong>${escapeHtml(report.deptName || "未分配部门")}</strong>
								<small>部门编号 ${escapeHtml(report.deptId == null ? "--" : report.deptId)}</small>
							</div>
						</div>
					</td>
					<td>${Number(report.employeeCount || 0)}<span class="secondary-line">有消费员工</span></td>
					<td>${Number(report.orderCount || 0)}</td>
					<td>${Number(report.totalQuantity || 0)}</td>
					<td class="align-right"><strong>${money(report.totalAmount)}</strong></td>
				</tr>`).join("")
			: `<tr><td colspan="5"><div class="empty-state">该月份暂无部门消费数据</div></td></tr>`;
		refreshIcons();
	}

	async function openAuditDetail(key) {
		// 部门汇总来自 /reports/dept-consumption；点击行后按员工逐个取 /reports/employee-consumption 明细。
		const report = state.deptReports.find((item) => deptKey(item) === String(key));
		if (!report) {
			return;
		}
		const content = byId("audit-detail-content");
		setText("audit-dialog-title", `${report.deptName || "未分配部门"} · ${monthLabel(state.selectedMonth)}`);
		setHtml("audit-detail-content", `<div class="empty-state">正在加载员工消费明细…</div>`);
		openDialog("audit-dialog");

		if (!can("user:manage")) {
			setHtml("audit-detail-content", `<div class="empty-state">
				该部门本月消费 ${money(report.totalAmount)}，共 ${Number(report.orderCount || 0)} 笔订单、${Number(report.totalQuantity || 0)} 份菜品、${Number(report.employeeCount || 0)} 名员工有消费。
				展开到员工明细需要员工管理权限（user:manage）。
			</div>`);
			return;
		}

		const employees = state.users.filter((user) => deptKey(user) === String(key));
		if (!employees.length) {
			setHtml("audit-detail-content", `<div class="empty-state">该部门本月没有可展示的员工记录</div>`);
			return;
		}
		const month = encodeURIComponent(state.selectedMonth);
		const results = await Promise.all(employees.map((user) =>
			api(`/api/v1/user/reports/employee-consumption?employeeId=${encodeURIComponent(user.userId)}&month=${month}&withDetails=true`)
				.catch(() => null)));
		renderAuditDetail(results.filter(Boolean));
	}

	function renderAuditDetail(reports) {
		const ordersOf = (report) => (report.details || []).map((order) => `
			<li>${escapeHtml(order.orderDate || dateTime(order.createdAt))} · 订单 ${escapeHtml(order.orderNo || order.orderId || "--")}
				· ${escapeHtml(order.statusText || order.status || "")} · ${money(order.totalAmount)}：
				${(order.items || []).map((item) => `${escapeHtml(item.recipeName)} × ${Number(item.quantity || 0)}`).join("，") || "无菜品明细"}</li>`).join("");

		setHtml("audit-detail-content", reports.length
			? reports.map((report) => {
				const orders = report.details || [];
				return `
				<article class="audit-order">
					<header>
						<strong>${escapeHtml(report.employeeName || "--")}</strong>
						<span>${escapeHtml(report.employeeNo || "--")} · 订单 ${Number(report.orderCount || 0)} 笔
							· 菜品 ${Number(report.totalQuantity || 0)} 份 · ${money(report.totalAmount)}</span>
					</header>
					${orders.length
						? `<ul>${ordersOf(report)}</ul>`
						: `<div class="empty-state">该员工本月暂无消费记录</div>`}
				</article>`;
			}).join("")
			: `<div class="empty-state">没有取到员工消费明细</div>`);
		refreshIcons();
	}

	function openUserDialog(user) {
		const form = byId("user-form");
		if (form) {
			form.reset();
		}
		state.editingUser = user || null;
		setText("user-form-error", "");
		const employeeNoInput = byId("user-employee-no");
		const passwordField = byId("user-password-field");
		const roleCheckboxes = document.querySelectorAll('input[name="user-role"]');
		if (user) {
			setText("user-dialog-title", "编辑员工");
			const idInput = byId("user-id");
			if (idInput) {
				idInput.value = user.userId;
			}
			// UserUpdateCmd 不含工号，编辑时只读展示。
			if (employeeNoInput) {
				employeeNoInput.value = user.employeeNo || "";
				employeeNoInput.disabled = true;
			}
			const nameInput = byId("user-name");
			if (nameInput) {
				nameInput.value = user.name || "";
			}
			const deptInput = byId("user-dept-name");
			if (deptInput) {
				deptInput.value = user.deptName || "";
			}
			const workstationInput = byId("user-workstation");
			if (workstationInput) {
				workstationInput.value = user.workstation || "";
			}
			const phoneInput = byId("user-phone");
			if (phoneInput) {
				phoneInput.value = user.phone || "";
			}
			if (passwordField) {
				passwordField.hidden = true;
			}
			const roleIds = new Set((user.roleIds || []).map(Number));
			roleCheckboxes.forEach((checkbox) => {
				checkbox.checked = roleIds.has(Number(checkbox.value));
			});
		} else {
			setText("user-dialog-title", "新增员工");
			const idInput = byId("user-id");
			if (idInput) {
				idInput.value = "";
			}
			if (employeeNoInput) {
				employeeNoInput.disabled = false;
			}
			const passwordInput = byId("user-password");
			if (passwordInput) {
				passwordInput.value = "123456";
			}
			if (passwordField) {
				passwordField.hidden = false;
			}
			// 默认勾选企业员工角色（按角色名称匹配，不依赖固定 ID）。
			const roles = roleMap();
			roleCheckboxes.forEach((checkbox) => {
				const role = roles.get(Number(checkbox.value));
				checkbox.checked = Boolean(role && role.name === "EMPLOYEE");
			});
		}
		openDialog("user-dialog");
	}

	function selectedRoleIds(name) {
		return [...document.querySelectorAll(`input[name="${name}"]:checked`)].map((input) => Number(input.value));
	}

	function departmentPayload() {
		// 部门以 deptName 文本 + 已存在的 deptId 提交；新部门名没有 deptId 时提交 null。
		const deptName = valueOf("user-dept-name").trim();
		const matched = departments().find((dept) => dept.deptName === deptName);
		let deptId = matched ? matched.deptId : null;
		const editing = state.editingUser;
		if (deptId == null && editing && (editing.deptName || "") === deptName && editing.deptId != null) {
			// 部门名没改、但当前页员工数据里没有该部门时，沿用原 deptId，避免提交 null 把部门ID清空。
			deptId = editing.deptId;
		}
		return {deptName: deptName || null, deptId};
	}

	async function submitUser(event) {
		// 新增提交工号与初始密码；编辑只改姓名/部门/工位/电话，角色单独走分配接口。
		event.preventDefault();
		const errorElement = byId("user-form-error");
		if (errorElement) {
			errorElement.textContent = "";
		}
		const userId = valueOf("user-id");
		const roleIds = selectedRoleIds("user-role");
		const withRoles = canAssignRoles();
		const dept = departmentPayload();
		const payload = {
			name: valueOf("user-name").trim(),
			deptId: dept.deptId,
			deptName: dept.deptName,
			workstation: valueOf("user-workstation").trim(),
			phone: valueOf("user-phone").trim()
		};
		try {
			if (userId) {
				await api(`/api/v1/user/users/${encodeURIComponent(userId)}`, {method: "PUT", body: payload});
				// UserUpdateCmd 不含 roleIds，角色用 RoleAssignRequest 全量覆盖（无角色字典时不动角色）。
				if (withRoles) {
					await api(`/api/v1/user/users/${encodeURIComponent(userId)}/roles`, {method: "PUT", body: {roleIds}});
				}
				toast("员工信息已更新");
			} else {
				const createPayload = Object.assign({}, payload, {
					employeeNo: valueOf("user-employee-no").trim(),
					initialPassword: valueOf("user-password") || "123456"
				});
				if (withRoles) {
					createPayload.roleIds = roleIds;
				}
				await api("/api/v1/user/users", {method: "POST", body: createPayload});
				toast("员工账号已创建");
			}
			closeDialog("user-dialog");
			await reloadUsers();
		} catch (error) {
			if (errorElement) {
				errorElement.textContent = error.message;
			}
		}
	}

	function openRoleDialog(role) {
		const form = byId("role-form");
		if (form) {
			form.reset();
		}
		setText("role-form-error", "");
		const nameInput = byId("role-name");
		const permissionCheckboxes = document.querySelectorAll('input[name="role-permission"]');
		const idInput = byId("role-id");
		if (role) {
			setText("role-dialog-title", "编辑角色");
			if (idInput) {
				idInput.value = role.roleId;
			}
			if (nameInput) {
				nameInput.value = roleLabel(role);
				// 角色名称即角色身份：预置角色被后端 @RequiresPerm/RoleCodes 引用，禁止改名。
				nameInput.disabled = isPresetRole(role);
			}
			const permCodeSet = new Set(role.permCodes || []);
			permissionCheckboxes.forEach((checkbox) => {
				checkbox.checked = permCodeSet.has(checkbox.value);
			});
		} else {
			setText("role-dialog-title", "新增角色");
			if (idInput) {
				idInput.value = "";
			}
			if (nameInput) {
				nameInput.disabled = false;
			}
			permissionCheckboxes.forEach((checkbox) => {
				checkbox.checked = false;
			});
		}
		openDialog("role-dialog");
	}

	async function submitRole(event) {
		// 角色基础信息与权限分开保存：POST /roles 只建角色，权限走 /roles/{roleId}/permissions。
		event.preventDefault();
		setText("role-form-error", "");
		const roleId = valueOf("role-id");
		const permCodeList = [...document.querySelectorAll('input[name="role-permission"]:checked')]
			.map((input) => input.value);
		const name = valueOf("role-name").trim();
		try {
			let targetId = roleId ? Number(roleId) : null;
			if (targetId) {
				await api(`/api/v1/user/roles/${targetId}`, {method: "PUT", body: {name}});
			} else {
				targetId = await api("/api/v1/user/roles", {
					method: "POST",
					body: {name: name.toUpperCase()}
				});
			}
			if (targetId) {
				await api(`/api/v1/user/roles/${targetId}/permissions`, {method: "PUT", body: {permCodes: permCodeList}});
			}
			toast(roleId ? "角色已更新" : "角色已创建");
			closeDialog("role-dialog");
			await reloadRoles();
		} catch (error) {
			setText("role-form-error", error.message);
		}
	}

	async function reloadUsers() {
		if (!can("user:manage")) {
			return;
		}
		await guard(async () => {
			await loadUsers();
			populateSelections();
			renderUsers();
			renderOverview();
		});
	}

	async function reloadRoles() {
		if (!can("role:manage")) {
			return;
		}
		await guard(async () => {
			await loadRoles();
			await loadPermissions();
			populateSelections();
			renderRoles();
			renderUsers();
			renderOverview();
		});
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
			// 账号状态在 ACTIVE 与 DISABLED 之间切换（UserStatusRequest.status）。
			const target = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
			try {
				await api(`/api/v1/user/users/${userId}/status`, {method: "PUT", body: {status: target}});
				toast(target === "DISABLED" ? "账号已停用" : "账号已启用");
				await reloadUsers();
			} catch (error) {
				handleError(error);
			}
			return;
		}
		if (action === "disable" && window.confirm(`确认停用员工“${user.name}”？停用为逻辑停用，历史订单与报表仍保留其归属。`)) {
			try {
				await api(`/api/v1/user/users/${userId}`, {method: "DELETE"});
				toast("员工账号已停用");
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
		if (action === "delete" && window.confirm(`确认删除角色“${roleLabel(role)}”？`)) {
			try {
				await api(`/api/v1/user/roles/${roleId}`, {method: "DELETE"});
				toast("角色已删除");
				await reloadRoles();
			} catch (error) {
				handleError(error);
			}
		}
	}

	async function submitImport(event) {
		// 文件走 multipart 的 file 字段，deptId / roleIds 走查询参数（均可选）。
		event.preventDefault();
		setText("import-form-error", "");
		const fileInput = byId("import-file");
		if (!fileInput || !fileInput.files.length) {
			setText("import-form-error", "请选择导入文件");
			return;
		}
		const formData = new FormData();
		formData.append("file", fileInput.files[0]);
		const params = new URLSearchParams();
		const deptId = valueOf("import-dept");
		if (deptId) {
			params.append("deptId", deptId);
		}
		selectedRoleIds("import-role").forEach((id) => params.append("roleIds", String(id)));
		const query = params.toString();
		try {
			const result = (await api(`/api/v1/user/users/import${query ? `?${query}` : ""}`, {
				method: "POST",
				body: formData
			})) || {};
			const errors = Array.isArray(result.errors) ? result.errors : [];
			const successCount = Number(result.successCount || 0);
			const failCount = Number(result.failCount == null ? errors.length : result.failCount);
			const total = Number(result.total == null ? successCount + failCount : result.total);
			setHtml("import-result", `
				<strong>导入完成：共 ${total} 行，成功 ${successCount} 行，失败 ${failCount} 行</strong>
				${errors.length
					? `<ul>${errors.map((error) => `<li>第 ${escapeHtml(error.row)} 行：${escapeHtml(error.reason)}</li>`).join("")}</ul>`
					: ""}`);
			toast("员工数据导入完成");
			await reloadUsers();
		} catch (error) {
			setText("import-form-error", error.message);
		}
	}

	// 文件流接口无法带 Authorization 头，走页面 Cookie 里的 token（登录时已写入）。
	function download(url) {
		window.location.assign(url);
	}

	function bindEvents() {
		// 使用事件委托处理动态生成的表格行按钮。
		document.querySelectorAll(".nav-item").forEach((button) => {
			button.addEventListener("click", () => setView(button.dataset.view));
		});

		document.querySelectorAll(".dialog-close").forEach((button) => {
			button.addEventListener("click", () => {
				const dialog = button.closest("dialog");
				if (dialog) {
					dialog.close();
				}
			});
		});

		["user-search", "user-department-filter", "user-status-filter", "user-role-filter"].forEach((id) => {
			on(id, "input", renderUsers);
			on(id, "change", renderUsers);
		});

		on("audit-search", "input", renderDeptReports);

		on("open-user-create", "click", () => openUserDialog(null));
		on("open-role-create", "click", () => openRoleDialog(null));
		on("open-import", "click", () => {
			const form = byId("import-form");
			if (form) {
				form.reset();
			}
			setHtml("import-result", "");
			setText("import-form-error", "");
			setText("import-file-name", "支持 .xlsx、.csv");
			openDialog("import-dialog");
		});
		on("download-template", "click", () => download("/api/v1/user/users/import/template"));

		on("user-form", "submit", submitUser);
		on("role-form", "submit", submitRole);
		on("import-form", "submit", submitImport);

		const userTableBody = byId("user-table-body");
		if (userTableBody) {
			userTableBody.addEventListener("click", (event) => {
				const button = event.target.closest("[data-user-action]");
				if (button) {
					handleUserAction(button);
				}
			});
		}
		const roleTableBody = byId("role-table-body");
		if (roleTableBody) {
			roleTableBody.addEventListener("click", (event) => {
				const button = event.target.closest("[data-role-action]");
				if (button) {
					handleRoleAction(button);
				}
			});
		}
		const auditTableBody = byId("audit-table-body");
		if (auditTableBody) {
			auditTableBody.addEventListener("click", (event) => {
				const row = event.target.closest("[data-audit-dept-id]");
				if (row) {
					openAuditDetail(row.dataset.auditDeptId);
				}
			});
		}

		on("import-file", "change", (event) => {
			const file = event.target.files[0];
			setText("import-file-name", file ? file.name : "支持 .xlsx、.csv");
		});

		on("report-month", "change", async (event) => {
			state.selectedMonth = event.target.value;
			await guard(loadMonthlyReport);
		});
		on("audit-month", "change", async (event) => {
			state.selectedMonth = event.target.value;
			const reportMonth = byId("report-month");
			if (reportMonth) {
				reportMonth.value = state.selectedMonth;
			}
			const tasks = [guard(loadDeptReports)];
			if (can("report:view")) {
				tasks.push(guard(loadMonthlyReport));
			}
			await Promise.all(tasks);
		});
		on("generate-report", "click", async () => {
			await guard(async () => {
				const result = await api(`/api/v1/user/reports/monthly/generate?month=${encodeURIComponent(state.selectedMonth)}`, {method: "POST"});
				await loadMonthlyReport();
				toast(result && result.generatedAt ? `报表已生成（${result.generatedAt}）` : "报表已生成");
			});
		});
		on("refresh-report", "click", async () => {
			await guard(async () => {
				await api(`/api/v1/user/reports/monthly/refresh?month=${encodeURIComponent(state.selectedMonth)}`, {method: "POST"});
				await Promise.all([
					loadMonthlyReport(),
					canAudit() ? loadDeptReports() : Promise.resolve()
				]);
				toast("报表缓存已刷新");
			});
		});
		on("export-report", "click", () => {
			// 仅 XLSX / CSV 可用，PDF 后端会按参数校验失败（40001）返回。
			download(`/api/v1/user/reports/monthly/export?month=${encodeURIComponent(state.selectedMonth)}&format=XLSX`);
		});
		on("print-report", "click", () => window.print());

		on("logout-button", "click", async () => {
			try {
				await api("/api/v1/user/auth/logout", {method: "POST"});
			} catch (ignored) {
				// 无论请求结果如何都清掉本地登录态并返回登录页。
			}
			window.localStorage.removeItem("dsh_token");
			document.cookie = "dsh_token=; path=/; max-age=0; SameSite=Lax";
			window.location.assign("/login");
		});
	}

	function initialize() {
		// 页面先加载共享数据，再并行请求月报和部门消费汇总。
		setText("today-label", new Intl.DateTimeFormat("zh-CN", {dateStyle: "full"}).format(new Date()));
		bindEvents();
		loadAll();
		refreshIcons();
	}

	initialize();
})();
