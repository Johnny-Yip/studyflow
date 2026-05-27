const SESSION_TOKEN_KEY = "studyflow.token";
const SESSION_USER_KEY = "studyflow.user";
const AUTH_REQUIRED_MESSAGE = "Please log in to view your planner.";

const state = {
    auth: {
        token: localStorage.getItem(SESSION_TOKEN_KEY) || "",
        user: readStoredUser(),
    },
    summary: {
        totalCourses: 0,
        totalTasks: 0,
        completedTasks: 0,
        openTasks: 0,
        overdueTasks: 0,
        completionPercentage: 0,
    },
    courses: [],
    allTasks: [],
    tasks: [],
    grades: [],
};

const elements = {
    appHeader: document.querySelector("#appHeader"),
    tabs: document.querySelectorAll(".tab-button"),
    sections: document.querySelectorAll(".dashboard-section"),
    dashboardTabs: document.querySelector("#dashboardTabs"),
    dashboardShell: document.querySelector("#dashboardShell"),
    authSection: document.querySelector("#authSection"),
    authModeButtons: document.querySelectorAll(".auth-mode-button"),
    authForms: document.querySelectorAll(".auth-form"),
    authMessage: document.querySelector("#authMessage"),
    loginForm: document.querySelector("#loginForm"),
    registerForm: document.querySelector("#registerForm"),
    logoutButton: document.querySelector("#logoutButton"),
    authUser: document.querySelector("#authUser"),
    message: document.querySelector("#message"),
    courseForm: document.querySelector("#courseForm"),
    taskForm: document.querySelector("#taskForm"),
    taskFilterForm: document.querySelector("#taskFilterForm"),
    gradeForm: document.querySelector("#gradeForm"),
    courseList: document.querySelector("#courseList"),
    taskList: document.querySelector("#taskList"),
    gradeList: document.querySelector("#gradeList"),
    taskCourse: document.querySelector("#taskCourse"),
    taskFilterCourse: document.querySelector("#taskFilterCourse"),
    taskFilterReset: document.querySelector("#taskFilterReset"),
    gradeCourse: document.querySelector("#gradeCourse"),
    gradeFormTitle: document.querySelector("#gradeFormTitle"),
    gradeSubmitButton: document.querySelector("#gradeSubmitButton"),
    gradeCancelButton: document.querySelector("#gradeCancelButton"),
    taskDueDate: document.querySelector("#taskDueDate"),
    courseCount: document.querySelector("#courseCount"),
    taskCount: document.querySelector("#taskCount"),
    completedTaskCount: document.querySelector("#completedTaskCount"),
    openTaskCount: document.querySelector("#openTaskCount"),
    overdueTaskCount: document.querySelector("#overdueTaskCount"),
    completionRate: document.querySelector("#completionRate"),
};

document.addEventListener("DOMContentLoaded", async () => {
    elements.taskDueDate.min = getToday();
    elements.taskDueDate.value = getToday();

    setupTabs();
    setupForms();
    setupTaskFilters();
    setupAuth();

    if (state.auth.token) {
        await loadDashboard();
    } else {
        returnToLogin(AUTH_REQUIRED_MESSAGE);
    }
});

function setupAuth() {
    elements.authModeButtons.forEach((button) => {
        button.addEventListener("click", () => {
            setAuthMode(button.dataset.authMode);
        });
    });

    elements.loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        showAuthMessage("Signing in...");

        try {
            const payload = {
                email: elements.loginForm.email.value.trim(),
                password: elements.loginForm.password.value,
            };

            const response = await request("/api/auth/login", {
                method: "POST",
                body: JSON.stringify(payload),
            }, false);

            setSession(response);
            showAuthMessage("");
            await loadDashboard();
        } catch (error) {
            showAuthMessage(error.message, "error");
        }
    });

    elements.registerForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        showAuthMessage("Creating account...");

        try {
            const payload = {
                name: elements.registerForm.name.value.trim(),
                email: elements.registerForm.email.value.trim(),
                password: elements.registerForm.password.value,
            };

            const response = await request("/api/auth/register", {
                method: "POST",
                body: JSON.stringify(payload),
            }, false);

            setSession(response);
            showAuthMessage("");
            await loadDashboard();
        } catch (error) {
            showAuthMessage(error.message, "error");
        }
    });

    elements.logoutButton.addEventListener("click", () => {
        returnToLogin("Logged out.", "success");
    });
}

function setAuthMode(mode, options = {}) {
    const shouldClearMessage = options.clearMessage ?? true;
    const normalizedMode = mode === "register" ? "register" : "login";

    elements.authModeButtons.forEach((button) => {
        button.classList.toggle("active", button.dataset.authMode === normalizedMode);
    });

    elements.authForms.forEach((form) => {
        form.hidden = form.dataset.authMode !== normalizedMode;
    });

    if (shouldClearMessage) {
        showAuthMessage("");
    }
}

function renderAuthState() {
    const isAuthenticated = Boolean(state.auth.token);
    elements.appHeader.hidden = !isAuthenticated;
    elements.authSection.hidden = isAuthenticated;
    elements.dashboardShell.hidden = !isAuthenticated;
    elements.dashboardTabs.hidden = !isAuthenticated;
    elements.logoutButton.hidden = !isAuthenticated;
    elements.authUser.hidden = !isAuthenticated;

    if (isAuthenticated && state.auth.user) {
        elements.authUser.textContent = `${state.auth.user.name} (${state.auth.user.email})`;
        return;
    }

    elements.authUser.textContent = "";
}

function returnToLogin(message = AUTH_REQUIRED_MESSAGE, type = "") {
    clearSession();
    renderAuthState();
    resetDashboardState();
    setAuthMode("login", { clearMessage: false });
    showAuthMessage(message, type);
}

function setSession(authResponse) {
    state.auth.token = authResponse.token;
    state.auth.user = {
        userId: authResponse.userId,
        name: authResponse.name,
        email: authResponse.email,
    };
    localStorage.setItem(SESSION_TOKEN_KEY, state.auth.token);
    localStorage.setItem(SESSION_USER_KEY, JSON.stringify(state.auth.user));
    elements.loginForm.reset();
    elements.registerForm.reset();
    renderAuthState();
}

function clearSession() {
    state.auth.token = "";
    state.auth.user = null;
    localStorage.removeItem(SESSION_TOKEN_KEY);
    localStorage.removeItem(SESSION_USER_KEY);
}

function resetDashboardState() {
    state.summary = {
        totalCourses: 0,
        totalTasks: 0,
        completedTasks: 0,
        openTasks: 0,
        overdueTasks: 0,
        completionPercentage: 0,
    };
    state.courses = [];
    state.allTasks = [];
    state.tasks = [];
    state.grades = [];
    render();
}

function setupTabs() {
    elements.tabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            const target = tab.dataset.section;

            elements.tabs.forEach((button) => {
                button.classList.toggle("active", button === tab);
            });

            elements.sections.forEach((section) => {
                section.classList.toggle("active", section.id === target);
            });
        });
    });
}

function setupForms() {
    elements.courseForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const payload = {
            name: elements.courseForm.name.value.trim(),
            description: elements.courseForm.description.value.trim(),
        };

        await request("/api/courses", {
            method: "POST",
            body: JSON.stringify(payload),
        });

        elements.courseForm.reset();
        showMessage("Course created.", "success");
        await loadDashboard();
    });

    elements.taskForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const courseId = elements.taskForm.courseId.value;
        const payload = {
            title: elements.taskForm.title.value.trim(),
            description: elements.taskForm.description.value.trim(),
            dueDate: elements.taskForm.dueDate.value,
            priority: elements.taskForm.priority.value,
            status: elements.taskForm.status.value,
        };

        await request(`/api/courses/${courseId}/tasks`, {
            method: "POST",
            body: JSON.stringify(payload),
        });

        elements.taskForm.reset();
        elements.taskForm.dueDate.value = getToday();
        elements.taskForm.priority.value = "MEDIUM";
        elements.taskForm.status.value = "TODO";
        showMessage("Task created.", "success");
        await loadDashboard();
    });

    elements.gradeForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const gradeId = elements.gradeForm.dataset.gradeId;
        const courseId = elements.gradeForm.courseId.value;
        const payload = {
            assignmentName: elements.gradeForm.assignmentName.value.trim(),
            score: Number(elements.gradeForm.score.value),
            maxScore: Number(elements.gradeForm.maxScore.value),
            weight: Number(elements.gradeForm.weight.value),
        };

        if (gradeId) {
            await request(`/api/grades/${gradeId}`, {
                method: "PUT",
                body: JSON.stringify(payload),
            });
            showMessage("Grade updated.", "success");
        } else {
            await request(`/api/courses/${courseId}/grades`, {
                method: "POST",
                body: JSON.stringify(payload),
            });
            showMessage("Grade created.", "success");
        }

        resetGradeForm();
        await loadDashboard();
    });

    elements.gradeCancelButton.addEventListener("click", () => {
        resetGradeForm();
    });
}

function setupTaskFilters() {
    elements.taskFilterForm.addEventListener("input", debounce(loadFilteredTasks, 250));
    elements.taskFilterForm.addEventListener("change", loadFilteredTasks);
    elements.taskFilterForm.addEventListener("submit", (event) => {
        event.preventDefault();
        loadFilteredTasks();
    });

    elements.taskFilterReset.addEventListener("click", () => {
        elements.taskFilterForm.reset();
        loadFilteredTasks();
    });
}

async function loadDashboard() {
    if (!state.auth.token) {
        returnToLogin(AUTH_REQUIRED_MESSAGE);
        return;
    }

    try {
        showMessage("Loading dashboard...");
        const [summary, courses, allTasks] = await Promise.all([
            request("/api/dashboard/summary"),
            request("/api/courses"),
            request("/api/tasks?sort=dueDate"),
        ]);
        state.summary = summary;
        state.courses = courses;
        state.allTasks = allTasks;
        const [tasks, grades] = await Promise.all([
            request(buildTaskQuery()),
            loadGradesForCourses(state.courses),
        ]);
        state.tasks = tasks;
        state.grades = grades;
        render();
        renderAuthState();
        showMessage("");
    } catch (error) {
        if (!state.auth.token) {
            showAuthMessage(error.message || AUTH_REQUIRED_MESSAGE, "error");
            return;
        }

        showMessage(error.message, "error");
    }
}

async function loadFilteredTasks() {
    if (!state.auth.token) {
        return;
    }

    try {
        state.tasks = await request(buildTaskQuery());
        renderTasks();
        showMessage("");
    } catch (error) {
        if (!state.auth.token) {
            showAuthMessage(error.message || AUTH_REQUIRED_MESSAGE, "error");
            return;
        }

        showMessage(error.message, "error");
    }
}

async function loadGradesForCourses(courses) {
    const gradeGroups = await Promise.all(
        courses.map((course) => request(`/api/courses/${course.id}/grades`))
    );
    return gradeGroups.flat();
}

async function request(url, options = {}, withAuth = true) {
    const headers = {
        "Content-Type": "application/json",
        Accept: "application/json",
        ...options.headers,
    };

    if (withAuth && state.auth.token) {
        headers.Authorization = `Bearer ${state.auth.token}`;
    }

    const response = await fetch(url, {
        ...options,
        headers,
    });

    if (response.status === 401 && withAuth) {
        returnToLogin(AUTH_REQUIRED_MESSAGE, "error");
        throw new Error(AUTH_REQUIRED_MESSAGE);
    }

    if (response.status === 401) {
        const detail = await readErrorMessage(response);
        if (detail === "Invalid email or password") {
            throw new Error(detail);
        }

        returnToLogin(AUTH_REQUIRED_MESSAGE, "error");
        throw new Error(AUTH_REQUIRED_MESSAGE);
    }

    if (!response.ok) {
        const fallback = `${response.status} ${response.statusText}`;
        const detail = await readErrorMessage(response, fallback);

        throw new Error(detail);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

async function readErrorMessage(response, fallback = `${response.status} ${response.statusText}`) {
    let detail = fallback;

    try {
        const error = await response.json();
        detail = formatApiError(error) || fallback;
    } catch {
        detail = fallback;
    }

    if (response.status === 401 && (detail === "Unauthorized" || detail === fallback)) {
        return AUTH_REQUIRED_MESSAGE;
    }

    return detail;
}

function render() {
    renderMetrics();
    renderCourseOptions(elements.taskCourse, elements.taskForm, "Create a course first");
    renderCourseOptions(elements.gradeCourse, elements.gradeForm, "Create a course first");
    renderTaskFilterOptions();
    renderCourses();
    renderTasks();
    renderGrades();
}

function renderMetrics() {
    const completion = Math.round(state.summary.completionPercentage);

    elements.courseCount.textContent = state.summary.totalCourses;
    elements.taskCount.textContent = state.summary.totalTasks;
    elements.completedTaskCount.textContent = state.summary.completedTasks;
    elements.openTaskCount.textContent = state.summary.openTasks;
    elements.overdueTaskCount.textContent = state.summary.overdueTasks;
    elements.completionRate.textContent = `${completion}%`;
}

function renderCourseOptions(select, form, emptyLabel) {
    if (!state.courses.length) {
        select.innerHTML = `<option value="">${emptyLabel}</option>`;
        select.disabled = true;
        form.querySelector("button[type='submit']").disabled = true;
        return;
    }

    select.disabled = Boolean(form.dataset.gradeId);
    form.querySelector("button[type='submit']").disabled = false;
    const previousValue = select.value;
    select.innerHTML = state.courses
        .map((course) => `<option value="${course.id}">${escapeHtml(course.name)}</option>`)
        .join("");
    if (previousValue && state.courses.some((course) => String(course.id) === previousValue)) {
        select.value = previousValue;
    }
}

function renderCourses() {
    if (!state.courses.length) {
        elements.courseList.innerHTML = emptyState("No courses yet", "Create your first course to start planning.");
        return;
    }

    elements.courseList.innerHTML = state.courses
        .map((course) => {
            const taskCount = state.allTasks.filter((task) => task.courseId === course.id).length;
            const gradeCount = state.grades.filter((grade) => grade.courseId === course.id).length;
            return `
                <article class="item-card">
                    <div class="item-topline">
                        <div>
                            <h3>${escapeHtml(course.name)}</h3>
                            <p>${escapeHtml(course.description || "No description")}</p>
                        </div>
                        <span class="badge">${taskCount} tasks</span>
                    </div>
                    <div class="meta-row">
                        <span class="badge">${escapeHtml(course.userName || "Student")}</span>
                        <span class="badge">${gradeCount} grades</span>
                    </div>
                </article>
            `;
        })
        .join("");
}

function renderTaskFilterOptions() {
    const previousValue = elements.taskFilterCourse.value;
    elements.taskFilterCourse.innerHTML = '<option value="">All courses</option>' + state.courses
        .map((course) => `<option value="${course.id}">${escapeHtml(course.name)}</option>`)
        .join("");
    if (previousValue && state.courses.some((course) => String(course.id) === previousValue)) {
        elements.taskFilterCourse.value = previousValue;
    }
}

function renderTasks() {
    if (!state.tasks.length) {
        elements.taskList.innerHTML = emptyState("No tasks yet", "Add an assignment or reminder for one of your courses.");
        return;
    }

    const sortedTasks = [...state.tasks].sort((a, b) => {
        if (a.status === "DONE" && b.status !== "DONE") {
            return 1;
        }
        if (a.status !== "DONE" && b.status === "DONE") {
            return -1;
        }
        return a.dueDate.localeCompare(b.dueDate);
    });

    elements.taskList.innerHTML = sortedTasks
        .map((task) => `
            <article class="item-card">
                <div class="item-topline">
                    <div>
                        <h3>${escapeHtml(task.title)}</h3>
                        <p>${escapeHtml(task.description || "No description")}</p>
                    </div>
                    <span class="badge ${task.status === "DONE" ? "done" : ""}">${formatStatus(task.status)}</span>
                </div>
                <div class="meta-row">
                    <span class="badge">${escapeHtml(task.courseName)}</span>
                    <span class="badge ${priorityClass(task.priority)}">${formatPriority(task.priority)}</span>
                    <span class="badge">Due ${formatDate(task.dueDate)}</span>
                </div>
                <div class="action-row">
                    <button class="secondary-button" type="button" data-action="complete" data-task-id="${task.id}" ${task.status === "DONE" ? "disabled" : ""}>
                        Mark done
                    </button>
                    <button class="danger-button" type="button" data-action="delete" data-task-id="${task.id}">
                        Delete
                    </button>
                </div>
            </article>
        `)
        .join("");

    elements.taskList.querySelectorAll("button[data-action]").forEach((button) => {
        button.addEventListener("click", () => handleTaskAction(button));
    });
}

function renderGrades() {
    if (!state.courses.length) {
        elements.gradeList.innerHTML = emptyState("No gradebook yet", "Create a course before adding graded assignments.");
        return;
    }

    if (!state.grades.length) {
        elements.gradeList.innerHTML = emptyState("No grades yet", "Add an assignment score to start tracking course performance.");
        return;
    }

    const sortedGrades = [...state.grades].sort((a, b) => {
        return a.courseName.localeCompare(b.courseName) || a.assignmentName.localeCompare(b.assignmentName);
    });

    elements.gradeList.innerHTML = sortedGrades
        .map((grade) => {
            const percent = Math.round(grade.percentage);
            return `
                <article class="item-card">
                    <div class="item-topline">
                        <div>
                            <h3>${escapeHtml(grade.assignmentName)}</h3>
                            <p>${escapeHtml(grade.courseName)}</p>
                        </div>
                        <span class="grade-score">${formatNumber(grade.score)} / ${formatNumber(grade.maxScore)}</span>
                    </div>
                    <div class="progress-track" aria-label="${percent}% score">
                        <div class="progress-bar" style="width: ${Math.min(percent, 100)}%"></div>
                    </div>
                    <div class="meta-row">
                        <span class="badge">${percent}%</span>
                        <span class="badge">${formatNumber(grade.weight)}% weight</span>
                        <span class="badge">${formatNumber(grade.weightedScore)} weighted points</span>
                    </div>
                    <div class="action-row">
                        <button class="secondary-button" type="button" data-action="edit-grade" data-grade-id="${grade.id}">
                            Edit
                        </button>
                        <button class="danger-button" type="button" data-action="delete-grade" data-grade-id="${grade.id}">
                            Delete
                        </button>
                    </div>
                </article>
            `;
        })
        .join("");

    elements.gradeList.querySelectorAll("button[data-action]").forEach((button) => {
        button.addEventListener("click", () => handleGradeAction(button));
    });
}

async function handleTaskAction(button) {
    const taskId = Number(button.dataset.taskId);
    const task = state.tasks.find((item) => item.id === taskId);

    if (!task) {
        return;
    }

    try {
        if (button.dataset.action === "complete") {
            await request(`/api/tasks/${task.id}`, {
                method: "PUT",
                body: JSON.stringify({
                    title: task.title,
                    description: task.description,
                    dueDate: task.dueDate,
                    priority: task.priority,
                    status: "DONE",
                }),
            });
            showMessage("Task marked done.", "success");
        }

        if (button.dataset.action === "delete") {
            await request(`/api/tasks/${task.id}`, {
                method: "DELETE",
            });
            showMessage("Task deleted.", "success");
        }

        await loadDashboard();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function handleGradeAction(button) {
    const gradeId = Number(button.dataset.gradeId);
    const grade = state.grades.find((item) => item.id === gradeId);

    if (!grade) {
        return;
    }

    try {
        if (button.dataset.action === "edit-grade") {
            fillGradeForm(grade);
            return;
        }

        if (button.dataset.action === "delete-grade") {
            await request(`/api/grades/${grade.id}`, {
                method: "DELETE",
            });
            resetGradeForm();
            showMessage("Grade deleted.", "success");
        }

        await loadDashboard();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

function fillGradeForm(grade) {
    elements.gradeForm.dataset.gradeId = grade.id;
    elements.gradeForm.courseId.value = String(grade.courseId);
    elements.gradeForm.assignmentName.value = grade.assignmentName;
    elements.gradeForm.score.value = grade.score;
    elements.gradeForm.maxScore.value = grade.maxScore;
    elements.gradeForm.weight.value = grade.weight;
    elements.gradeCourse.disabled = true;
    elements.gradeFormTitle.textContent = "Edit grade";
    elements.gradeSubmitButton.textContent = "Save grade";
    elements.gradeCancelButton.hidden = false;
}

function resetGradeForm() {
    elements.gradeForm.reset();
    delete elements.gradeForm.dataset.gradeId;
    elements.gradeCourse.disabled = !state.courses.length;
    elements.gradeFormTitle.textContent = "Add grade";
    elements.gradeSubmitButton.textContent = "Create grade";
    elements.gradeCancelButton.hidden = true;
    renderCourseOptions(elements.gradeCourse, elements.gradeForm, "Create a course first");
}

function showMessage(text, type = "") {
    elements.message.textContent = text;
    elements.message.className = `message ${type}`.trim();
}

function showAuthMessage(text, type = "") {
    elements.authMessage.textContent = text;
    elements.authMessage.className = `message auth-message ${type}`.trim();
}

function emptyState(title, text) {
    return `
        <div class="empty-state">
            <strong>${title}</strong>
            <span>${text}</span>
        </div>
    `;
}

function formatApiError(error) {
    if (error.fieldErrors) {
        return Object.values(error.fieldErrors).join(" ");
    }

    return error.message;
}

function formatDate(value) {
    return new Date(`${value}T00:00:00`).toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
        year: "numeric",
    });
}

function formatPriority(value) {
    return value.charAt(0) + value.slice(1).toLowerCase();
}

function formatStatus(value) {
    return value
        .toLowerCase()
        .split("_")
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join(" ");
}

function priorityClass(value) {
    return value.toLowerCase();
}

function formatNumber(value) {
    return Number(value).toLocaleString(undefined, {
        maximumFractionDigits: 2,
    });
}

function buildTaskQuery() {
    const params = new URLSearchParams();
    const formData = new FormData(elements.taskFilterForm);

    for (const [key, value] of formData.entries()) {
        const normalizedValue = String(value).trim();
        if (normalizedValue) {
            params.set(key, normalizedValue);
        }
    }

    if (!params.has("sort")) {
        params.set("sort", "dueDate");
    }

    return `/api/tasks?${params.toString()}`;
}

function debounce(callback, delay) {
    let timeoutId;

    return (...args) => {
        window.clearTimeout(timeoutId);
        timeoutId = window.setTimeout(() => callback(...args), delay);
    };
}

function getToday() {
    const today = new Date();
    const offsetDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000);
    return offsetDate.toISOString().slice(0, 10);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function readStoredUser() {
    const rawValue = localStorage.getItem(SESSION_USER_KEY);
    if (!rawValue) {
        return null;
    }

    try {
        return JSON.parse(rawValue);
    } catch {
        localStorage.removeItem(SESSION_USER_KEY);
        return null;
    }
}
