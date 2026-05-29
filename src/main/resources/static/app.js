const SESSION_TOKEN_KEY = "studyflow.token";
const SESSION_USER_KEY = "studyflow.user";
const CANVAS_BASE_URL_KEY = "studyflow.canvas.baseUrl";
const CANVAS_TOKEN_KEY = "studyflow.canvas.token";
const AUTH_REQUIRED_MESSAGE = "Please log in to view your planner.";
const DEFAULT_API_ERROR_MESSAGE = "Something went wrong. Please try again.";
const NETWORK_ERROR_MESSAGE = "Unable to reach StudyFlow right now. Please check your connection and try again.";
const FORM_VALIDATION_SUMMARY = "Please fix the highlighted fields and try again.";

const FRIENDLY_STATUS_MESSAGES = {
    400: "Please review the form and try again.",
    401: AUTH_REQUIRED_MESSAGE,
    403: "You do not have permission to do that.",
    404: "We could not find that resource.",
    409: "A conflicting item already exists. Please adjust your input.",
    422: "Please review the form and try again.",
    429: "Canvas is rate limiting requests. Please wait a few minutes and try again.",
    500: "The server ran into an issue. Please try again.",
    502: "Canvas returned an unexpected response. Please try again.",
    503: "StudyFlow is temporarily unavailable. Please try again shortly.",
};

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
    canvas: {
        bucket: "ALL",
        tasks: [],
        syncLogs: [],
        lastSync: null,
    },
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
    canvasSettingsForm: document.querySelector("#canvasSettingsForm"),
    courseList: document.querySelector("#courseList"),
    taskList: document.querySelector("#taskList"),
    gradeList: document.querySelector("#gradeList"),
    canvasTaskList: document.querySelector("#canvasTaskList"),
    canvasSyncSummary: document.querySelector("#canvasSyncSummary"),
    canvasSyncLogList: document.querySelector("#canvasSyncLogList"),
    canvasBucketButtons: document.querySelectorAll(".bucket-button"),
    canvasTestButton: document.querySelector("#canvasTestButton"),
    canvasSyncButton: document.querySelector("#canvasSyncButton"),
    canvasMockSyncButton: document.querySelector("#canvasMockSyncButton"),
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

    setupValidation();
    setupTabs();
    setupForms();
    setupTaskFilters();
    setupCanvas();
    setupAuth();

    if (state.auth.token) {
        await loadDashboard();
    } else {
        returnToLogin(AUTH_REQUIRED_MESSAGE);
    }
});

function setupValidation() {
    [
        elements.loginForm,
        elements.registerForm,
        elements.courseForm,
        elements.taskForm,
        elements.gradeForm,
        elements.canvasSettingsForm,
    ].forEach((form) => {
        if (!form) {
            return;
        }

        ensureFieldErrorElements(form);
        syncFormSubmitState(form);
    });
}

function ensureFieldErrorElements(form) {
    const fields = form.querySelectorAll("input[name], select[name], textarea[name]");

    fields.forEach((field) => {
        const fieldName = field.name;
        const errorId = `${form.id}-${fieldName}-error`;

        let errorElement = form.querySelector(`.field-error[data-field-error='${fieldName}']`);
        if (!errorElement) {
            errorElement = document.createElement("p");
            errorElement.className = "field-error";
            errorElement.dataset.fieldError = fieldName;
            errorElement.id = errorId;
            errorElement.setAttribute("aria-live", "polite");
            field.insertAdjacentElement("afterend", errorElement);
        }

        field.setAttribute("aria-describedby", errorId);
        field.addEventListener("input", () => clearFieldError(form, fieldName));
        field.addEventListener("change", () => clearFieldError(form, fieldName));
    });
}

function setupAuth() {
    elements.authModeButtons.forEach((button) => {
        button.addEventListener("click", () => {
            setAuthMode(button.dataset.authMode);
        });
    });

    elements.loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearFormErrors(elements.loginForm);

        if (!validateAndDisplay(elements.loginForm)) {
            showAuthMessage(FORM_VALIDATION_SUMMARY, "error");
            return;
        }

        setFormSubmitting(elements.loginForm, true, "Signing in...");
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
            applyApiFieldErrors(elements.loginForm, error.fieldErrors);
            showAuthMessage(buildFormErrorMessage(error), "error");
        } finally {
            setFormSubmitting(elements.loginForm, false);
        }
    });

    elements.registerForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearFormErrors(elements.registerForm);

        if (!validateAndDisplay(elements.registerForm)) {
            showAuthMessage(FORM_VALIDATION_SUMMARY, "error");
            return;
        }

        setFormSubmitting(elements.registerForm, true, "Creating account...");
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
            applyApiFieldErrors(elements.registerForm, error.fieldErrors);
            showAuthMessage(buildFormErrorMessage(error), "error");
        } finally {
            setFormSubmitting(elements.registerForm, false);
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

    clearFormErrors(elements.loginForm);
    clearFormErrors(elements.registerForm);

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
    clearFormErrors(elements.loginForm);
    clearFormErrors(elements.registerForm);
    renderAuthState();
}

function clearSession() {
    state.auth.token = "";
    state.auth.user = null;
    localStorage.removeItem(SESSION_TOKEN_KEY);
    localStorage.removeItem(SESSION_USER_KEY);
    sessionStorage.removeItem(CANVAS_TOKEN_KEY);
    if (elements.canvasSettingsForm) {
        elements.canvasSettingsForm.accessToken.value = "";
    }
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
    state.canvas = {
        bucket: "ALL",
        tasks: [],
        syncLogs: [],
        lastSync: null,
    };
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
        clearFormErrors(elements.courseForm);

        if (!validateAndDisplay(elements.courseForm)) {
            showMessage(FORM_VALIDATION_SUMMARY, "error");
            return;
        }

        setFormSubmitting(elements.courseForm, true, "Creating course...");

        try {
            const payload = {
                name: elements.courseForm.name.value.trim(),
                description: elements.courseForm.description.value.trim(),
            };

            await request("/api/courses", {
                method: "POST",
                body: JSON.stringify(payload),
            });

            elements.courseForm.reset();
            clearFormErrors(elements.courseForm);
            await loadDashboard({
                showLoading: false,
                successMessage: "Course created.",
            });
        } catch (error) {
            applyApiFieldErrors(elements.courseForm, error.fieldErrors);
            showMessage(buildFormErrorMessage(error), "error");
        } finally {
            setFormSubmitting(elements.courseForm, false);
        }
    });

    elements.taskForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearFormErrors(elements.taskForm);

        if (!validateAndDisplay(elements.taskForm)) {
            showMessage(FORM_VALIDATION_SUMMARY, "error");
            return;
        }

        setFormSubmitting(elements.taskForm, true, "Creating task...");

        try {
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
            clearFormErrors(elements.taskForm);
            await loadDashboard({
                showLoading: false,
                successMessage: "Task created.",
            });
        } catch (error) {
            applyApiFieldErrors(elements.taskForm, error.fieldErrors);
            showMessage(buildFormErrorMessage(error), "error");
        } finally {
            setFormSubmitting(elements.taskForm, false);
        }
    });

    elements.gradeForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearFormErrors(elements.gradeForm);

        if (!validateAndDisplay(elements.gradeForm)) {
            showMessage(FORM_VALIDATION_SUMMARY, "error");
            return;
        }

        const gradeId = elements.gradeForm.dataset.gradeId;
        setFormSubmitting(elements.gradeForm, true, gradeId ? "Saving grade..." : "Creating grade...");

        try {
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

                resetGradeForm();
                await loadDashboard({
                    showLoading: false,
                    successMessage: "Grade updated.",
                });
                return;
            }

            await request(`/api/courses/${courseId}/grades`, {
                method: "POST",
                body: JSON.stringify(payload),
            });

            resetGradeForm();
            await loadDashboard({
                showLoading: false,
                successMessage: "Grade created.",
            });
        } catch (error) {
            applyApiFieldErrors(elements.gradeForm, error.fieldErrors);
            showMessage(buildFormErrorMessage(error), "error");
        } finally {
            setFormSubmitting(elements.gradeForm, false);
        }
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

function setupCanvas() {
    elements.canvasSettingsForm.baseUrl.value = localStorage.getItem(CANVAS_BASE_URL_KEY) || "";
    elements.canvasSettingsForm.accessToken.value = sessionStorage.getItem(CANVAS_TOKEN_KEY) || "";

    elements.canvasSettingsForm.baseUrl.addEventListener("input", () => {
        const value = elements.canvasSettingsForm.baseUrl.value.trim();
        if (value) {
            localStorage.setItem(CANVAS_BASE_URL_KEY, value);
        } else {
            localStorage.removeItem(CANVAS_BASE_URL_KEY);
        }
    });

    elements.canvasSettingsForm.accessToken.addEventListener("input", () => {
        const value = elements.canvasSettingsForm.accessToken.value.trim();
        if (value) {
            sessionStorage.setItem(CANVAS_TOKEN_KEY, value);
        } else {
            sessionStorage.removeItem(CANVAS_TOKEN_KEY);
        }
    });

    elements.canvasSettingsForm.addEventListener("submit", handleCanvasSync);
    elements.canvasTestButton.addEventListener("click", handleCanvasTest);
    elements.canvasMockSyncButton.addEventListener("click", handleCanvasMockSync);

    elements.canvasBucketButtons.forEach((button) => {
        button.addEventListener("click", async () => {
            state.canvas.bucket = button.dataset.bucket;
            elements.canvasBucketButtons.forEach((item) => {
                item.classList.toggle("active", item === button);
            });
            await loadCanvasData();
        });
    });
}

async function loadDashboard(options = {}) {
    const {
        showLoading = true,
        successMessage = "",
    } = options;

    if (!state.auth.token) {
        returnToLogin(AUTH_REQUIRED_MESSAGE);
        return;
    }

    try {
        if (showLoading) {
            showMessage("Loading dashboard...");
        }

        const [summary, courses, allTasks] = await Promise.all([
            request("/api/dashboard/summary"),
            request("/api/courses"),
            request("/api/tasks?sort=dueDate"),
        ]);

        state.summary = summary;
        state.courses = courses;
        state.allTasks = allTasks;

        const [tasks, grades, canvasTasks, canvasSyncLogs] = await Promise.all([
            request(buildTaskQuery()),
            loadGradesForCourses(state.courses),
            request(buildCanvasTaskQuery()),
            request("/api/canvas/sync-logs"),
        ]);

        state.tasks = tasks;
        state.grades = grades;
        state.canvas.tasks = canvasTasks;
        state.canvas.syncLogs = canvasSyncLogs;

        render();
        renderAuthState();

        if (successMessage) {
            showMessage(successMessage, "success");
        } else {
            showMessage("");
        }
    } catch (error) {
        if (!state.auth.token) {
            showAuthMessage(error.message || AUTH_REQUIRED_MESSAGE, "error");
            return;
        }

        showMessage(error.message || DEFAULT_API_ERROR_MESSAGE, "error");
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

        showMessage(error.message || DEFAULT_API_ERROR_MESSAGE, "error");
    }
}

async function handleCanvasTest() {
    clearFormErrors(elements.canvasSettingsForm);

    if (!validateAndDisplay(elements.canvasSettingsForm)) {
        showMessage(FORM_VALIDATION_SUMMARY, "error");
        return;
    }

    setCanvasActionPending(elements.canvasTestButton, true, "Testing...");
    showMessage("Testing Canvas connection...");

    try {
        const response = await request("/api/canvas/test", {
            method: "POST",
            body: JSON.stringify(buildCanvasSettingsPayload()),
        });
        showMessage(response.message, "success");
    } catch (error) {
        applyApiFieldErrors(elements.canvasSettingsForm, error.fieldErrors);
        showMessage(buildFormErrorMessage(error), "error");
    } finally {
        setCanvasActionPending(elements.canvasTestButton, false);
    }
}

async function handleCanvasSync(event) {
    event.preventDefault();
    clearFormErrors(elements.canvasSettingsForm);

    if (!validateAndDisplay(elements.canvasSettingsForm)) {
        showMessage(FORM_VALIDATION_SUMMARY, "error");
        return;
    }

    setCanvasActionPending(elements.canvasSyncButton, true, "Syncing...");
    showMessage("Syncing Canvas courses and assignments...");

    try {
        const result = await request("/api/canvas/sync", {
            method: "POST",
            body: JSON.stringify(buildCanvasSettingsPayload()),
        });
        state.canvas.lastSync = result;
        await loadCanvasData({ silent: true });
        showMessage(formatSyncResult(result), "success");
    } catch (error) {
        applyApiFieldErrors(elements.canvasSettingsForm, error.fieldErrors);
        showMessage(buildFormErrorMessage(error), "error");
    } finally {
        setCanvasActionPending(elements.canvasSyncButton, false);
    }
}

async function handleCanvasMockSync() {
    setCanvasActionPending(elements.canvasMockSyncButton, true, "Loading demo...");
    showMessage("Loading demo Canvas data...");

    try {
        const result = await request("/api/canvas/mock-sync", {
            method: "POST",
        });
        state.canvas.lastSync = result;
        await loadCanvasData({ silent: true });
        showMessage(formatSyncResult(result), "success");
    } catch (error) {
        showMessage(error.message || DEFAULT_API_ERROR_MESSAGE, "error");
    } finally {
        setCanvasActionPending(elements.canvasMockSyncButton, false);
    }
}

async function loadCanvasData(options = {}) {
    const { silent = false } = options;
    if (!state.auth.token) {
        return;
    }

    try {
        const [tasks, syncLogs] = await Promise.all([
            request(buildCanvasTaskQuery()),
            request("/api/canvas/sync-logs"),
        ]);
        state.canvas.tasks = tasks;
        state.canvas.syncLogs = syncLogs;
        renderCanvas();
        if (!silent) {
            showMessage("");
        }
    } catch (error) {
        if (!state.auth.token) {
            showAuthMessage(error.message || AUTH_REQUIRED_MESSAGE, "error");
            return;
        }

        showMessage(error.message || DEFAULT_API_ERROR_MESSAGE, "error");
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

    let response;
    try {
        response = await fetch(url, {
            ...options,
            headers,
        });
    } catch {
        throw createRequestError(NETWORK_ERROR_MESSAGE);
    }

    if (response.status === 401 && withAuth) {
        returnToLogin(AUTH_REQUIRED_MESSAGE, "error");
        throw createRequestError(AUTH_REQUIRED_MESSAGE, { status: 401 });
    }

    if (!response.ok) {
        const payload = await readErrorPayload(response);
        const message = formatResponseError(response.status, payload);
        throw createRequestError(message, {
            status: response.status,
            fieldErrors: payload?.fieldErrors || {},
        });
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

async function readErrorPayload(response) {
    try {
        return await response.json();
    } catch {
        return null;
    }
}

function createRequestError(message, metadata = {}) {
    const error = new Error(message || DEFAULT_API_ERROR_MESSAGE);
    error.status = metadata.status || 0;
    error.fieldErrors = metadata.fieldErrors || {};
    return error;
}

function formatResponseError(status, payload) {
    const apiMessage = formatApiError(payload);
    const hasFieldErrors = Boolean(payload?.fieldErrors && Object.keys(payload.fieldErrors).length);

    if (hasFieldErrors) {
        return FORM_VALIDATION_SUMMARY;
    }

    if (apiMessage) {
        return apiMessage;
    }

    return fallbackErrorMessage(status);
}

function fallbackErrorMessage(status) {
    return FRIENDLY_STATUS_MESSAGES[status] || DEFAULT_API_ERROR_MESSAGE;
}

function render() {
    renderMetrics();
    renderCourseOptions(elements.taskCourse, elements.taskForm, "Create a course first");
    renderCourseOptions(elements.gradeCourse, elements.gradeForm, "Create a course first");
    renderTaskFilterOptions();
    renderCourses();
    renderTasks();
    renderGrades();
    renderCanvas();
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
        syncFormSubmitState(form);
        return;
    }

    const previousValue = select.value;
    select.innerHTML = state.courses
        .map((course) => `<option value="${course.id}">${escapeHtml(course.name)}</option>`)
        .join("");

    if (previousValue && state.courses.some((course) => String(course.id) === previousValue)) {
        select.value = previousValue;
    }

    select.disabled = form === elements.gradeForm && Boolean(form.dataset.gradeId);
    syncFormSubmitState(form);
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

function renderCanvas() {
    renderCanvasSummary();
    renderCanvasTasks();
    renderCanvasLogs();
}

function renderCanvasSummary() {
    if (state.canvas.lastSync) {
        const result = state.canvas.lastSync;
        const warningText = result.warnings?.length
            ? `<span class="warning-text">${escapeHtml(result.warnings.join(" "))}</span>`
            : "";
        elements.canvasSyncSummary.innerHTML = `
            <strong>Synced ${result.tasksUpserted} Canvas task${result.tasksUpserted === 1 ? "" : "s"}.</strong>
            <span>${result.coursesFetched} courses, ${result.assignmentsFetched} assignments, ${result.missingFromTodoCount} assignment${result.missingFromTodoCount === 1 ? "" : "s"} missing from Canvas To-do.</span>
            ${warningText}
        `;
        return;
    }

    const latestLog = state.canvas.syncLogs[0];
    if (latestLog) {
        elements.canvasSyncSummary.innerHTML = `
            <strong>Last sync: ${escapeHtml(latestLog.status.toLowerCase())}.</strong>
            <span>${latestLog.tasksUpserted} tasks saved locally on ${formatDateTime(latestLog.completedAt)}.</span>
        `;
        return;
    }

    elements.canvasSyncSummary.innerHTML = `
        <strong>Canvas sync is ready.</strong>
        <span>Connect Canvas or load demo data to see tasks Canvas To-do may miss.</span>
    `;
}

function renderCanvasTasks() {
    if (!state.canvas.tasks.length) {
        elements.canvasTaskList.innerHTML = emptyState(
            `No ${formatBucketLabel(state.canvas.bucket).toLowerCase()} Canvas tasks`,
            "Sync Canvas or load demo data to fill this smarter task list."
        );
        return;
    }

    elements.canvasTaskList.innerHTML = state.canvas.tasks
        .map((task) => {
            const dueLabel = task.dueDate ? `Due ${formatDate(task.dueDate)}` : "No due date";
            const sourceBadges = (task.sources || [])
                .map((source) => `<span class="badge source">${escapeHtml(source)}</span>`)
                .join("");
            const canvasLink = task.htmlUrl
                ? `<a class="secondary-button link-button" href="${escapeHtml(task.htmlUrl)}" target="_blank" rel="noreferrer">Open in Canvas</a>`
                : "";

            return `
                <article class="item-card canvas-task-card">
                    <div class="item-topline">
                        <div>
                            <h3>${escapeHtml(task.title)}</h3>
                            <p>${escapeHtml(task.description || "No description")}</p>
                        </div>
                        <span class="badge ${canvasStatusClass(task.status)}">${formatCanvasStatus(task.status)}</span>
                    </div>
                    <div class="priority-meter" aria-label="Priority score ${task.priorityScore} out of 100">
                        <div class="priority-fill" style="width: ${Math.min(task.priorityScore, 100)}%"></div>
                    </div>
                    <div class="meta-row">
                        <span class="badge">${escapeHtml(task.courseName)}</span>
                        <span class="badge ${priorityScoreClass(task.priorityScore)}">Priority ${task.priorityScore}/100 (${escapeHtml(task.priorityLabel)})</span>
                        <span class="badge">${dueLabel}</span>
                        ${task.missingFromCanvasTodo ? '<span class="badge missing-todo">Missing from Canvas To-do</span>' : ""}
                    </div>
                    <div class="meta-row">
                        ${sourceBadges}
                    </div>
                    ${canvasLink ? `<div class="action-row">${canvasLink}</div>` : ""}
                </article>
            `;
        })
        .join("");
}

function renderCanvasLogs() {
    if (!state.canvas.syncLogs.length) {
        elements.canvasSyncLogList.innerHTML = emptyState("No sync history yet", "Your local sync log will appear here.");
        return;
    }

    elements.canvasSyncLogList.innerHTML = state.canvas.syncLogs
        .map((log) => `
            <article class="sync-log-item">
                <div>
                    <strong>${escapeHtml(log.status)}</strong>
                    <span>${formatDateTime(log.completedAt)}</span>
                </div>
                <p>${escapeHtml(log.message || "Canvas sync completed.")}</p>
                <div class="meta-row">
                    <span class="badge">${log.coursesFetched} courses</span>
                    <span class="badge">${log.assignmentsFetched} assignments</span>
                    <span class="badge">${log.tasksUpserted} tasks</span>
                    <span class="badge">${log.missingFromTodoCount} missing from To-do</span>
                </div>
            </article>
        `)
        .join("");
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

            await loadDashboard({
                showLoading: false,
                successMessage: "Task updated.",
            });
            return;
        }

        if (button.dataset.action === "delete") {
            await request(`/api/tasks/${task.id}`, {
                method: "DELETE",
            });

            await loadDashboard({
                showLoading: false,
                successMessage: "Task deleted.",
            });
        }
    } catch (error) {
        showMessage(error.message || DEFAULT_API_ERROR_MESSAGE, "error");
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
            await loadDashboard({
                showLoading: false,
                successMessage: "Grade deleted.",
            });
        }
    } catch (error) {
        showMessage(error.message || DEFAULT_API_ERROR_MESSAGE, "error");
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
    clearFormErrors(elements.gradeForm);
    syncFormSubmitState(elements.gradeForm);
}

function resetGradeForm() {
    elements.gradeForm.reset();
    delete elements.gradeForm.dataset.gradeId;
    elements.gradeCourse.disabled = !state.courses.length;
    elements.gradeFormTitle.textContent = "Add grade";
    elements.gradeSubmitButton.textContent = "Create grade";
    elements.gradeCancelButton.hidden = true;
    delete elements.gradeSubmitButton.dataset.originalText;
    clearFormErrors(elements.gradeForm);
    renderCourseOptions(elements.gradeCourse, elements.gradeForm, "Create a course first");
    syncFormSubmitState(elements.gradeForm);
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
    if (!error || typeof error !== "object") {
        return "";
    }

    const fieldMessages = error.fieldErrors && typeof error.fieldErrors === "object"
        ? Object.values(error.fieldErrors).filter(Boolean)
        : [];

    if (fieldMessages.length) {
        return fieldMessages.join(" ");
    }

    if (typeof error.message === "string" && error.message.trim() && error.message !== "Validation failed") {
        return error.message.trim();
    }

    return "";
}

function setFormSubmitting(form, isSubmitting, loadingLabel = "") {
    if (!form) {
        return;
    }

    const submitButton = getFormSubmitButton(form);
    if (!submitButton) {
        return;
    }

    if (isSubmitting) {
        form.dataset.submitting = "true";
        if (!submitButton.dataset.originalText) {
            submitButton.dataset.originalText = submitButton.textContent;
        }
        if (loadingLabel) {
            submitButton.textContent = loadingLabel;
        }
        submitButton.disabled = true;
        return;
    }

    form.dataset.submitting = "false";
    if (submitButton.dataset.originalText) {
        submitButton.textContent = submitButton.dataset.originalText;
        delete submitButton.dataset.originalText;
    }
    syncFormSubmitState(form);
}

function setCanvasActionPending(activeButton, isPending, loadingLabel = "") {
    const buttons = [
        elements.canvasSyncButton,
        elements.canvasTestButton,
        elements.canvasMockSyncButton,
    ];

    if (isPending) {
        buttons.forEach((button) => {
            button.disabled = true;
        });
        if (activeButton && loadingLabel) {
            if (!activeButton.dataset.originalText) {
                activeButton.dataset.originalText = activeButton.textContent;
            }
            activeButton.textContent = loadingLabel;
        }
        return;
    }

    buttons.forEach((button) => {
        button.disabled = false;
    });
    if (activeButton?.dataset.originalText) {
        activeButton.textContent = activeButton.dataset.originalText;
        delete activeButton.dataset.originalText;
    }
}

function getFormSubmitButton(form) {
    return form.querySelector("button[type='submit']");
}

function isFormSubmitting(form) {
    return form.dataset.submitting === "true";
}

function syncFormSubmitState(form) {
    if (!form) {
        return;
    }

    const submitButton = getFormSubmitButton(form);
    if (!submitButton) {
        return;
    }

    if (isFormSubmitting(form)) {
        submitButton.disabled = true;
        return;
    }

    if ((form === elements.taskForm || form === elements.gradeForm) && !state.courses.length) {
        submitButton.disabled = true;
        return;
    }

    submitButton.disabled = false;
}

function validateAndDisplay(form) {
    const errors = collectValidationErrors(form);

    if (!Object.keys(errors).length) {
        return true;
    }

    Object.entries(errors).forEach(([fieldName, message]) => {
        setFieldError(form, fieldName, message);
    });

    const firstFieldName = Object.keys(errors)[0];
    const firstField = form.elements.namedItem(firstFieldName);
    if (firstField && typeof firstField.focus === "function") {
        firstField.focus();
    }

    return false;
}

function collectValidationErrors(form) {
    switch (form.id) {
        case "loginForm":
            return validateLoginForm();
        case "registerForm":
            return validateRegisterForm();
        case "courseForm":
            return validateCourseForm();
        case "taskForm":
            return validateTaskForm();
        case "gradeForm":
            return validateGradeForm();
        case "canvasSettingsForm":
            return validateCanvasSettingsForm();
        default:
            return {};
    }
}

function validateLoginForm() {
    const errors = {};
    const email = elements.loginForm.email.value.trim();
    const password = elements.loginForm.password.value;

    if (!email) {
        errors.email = "Email is required.";
    } else if (!isValidEmail(email)) {
        errors.email = "Enter a valid email address.";
    }

    if (!password) {
        errors.password = "Password is required.";
    } else if (password.length < 8) {
        errors.password = "Password must be at least 8 characters.";
    }

    return errors;
}

function validateRegisterForm() {
    const errors = {};
    const name = elements.registerForm.name.value.trim();
    const email = elements.registerForm.email.value.trim();
    const password = elements.registerForm.password.value;

    if (!name) {
        errors.name = "Full name is required.";
    } else if (name.length > 120) {
        errors.name = "Full name must be 120 characters or fewer.";
    }

    if (!email) {
        errors.email = "Email is required.";
    } else if (!isValidEmail(email)) {
        errors.email = "Enter a valid email address.";
    }

    if (!password) {
        errors.password = "Password is required.";
    } else if (password.length < 8 || password.length > 72) {
        errors.password = "Password must be between 8 and 72 characters.";
    }

    return errors;
}

function validateCourseForm() {
    const errors = {};
    const name = elements.courseForm.name.value.trim();
    const description = elements.courseForm.description.value.trim();

    if (!name) {
        errors.name = "Course name is required.";
    } else if (name.length > 120) {
        errors.name = "Course name must be 120 characters or fewer.";
    }

    if (description.length > 1000) {
        errors.description = "Description must be 1000 characters or fewer.";
    }

    return errors;
}

function validateTaskForm() {
    const errors = {};
    const courseId = elements.taskForm.courseId.value;
    const title = elements.taskForm.title.value.trim();
    const description = elements.taskForm.description.value.trim();
    const dueDate = elements.taskForm.dueDate.value;
    const priority = elements.taskForm.priority.value;
    const status = elements.taskForm.status.value;

    if (!courseId) {
        errors.courseId = "Select a course.";
    }

    if (!title) {
        errors.title = "Task title is required.";
    } else if (title.length > 160) {
        errors.title = "Task title must be 160 characters or fewer.";
    }

    if (description.length > 2000) {
        errors.description = "Description must be 2000 characters or fewer.";
    }

    if (!dueDate) {
        errors.dueDate = "Due date is required.";
    } else if (dueDate < getToday()) {
        errors.dueDate = "Due date must be today or later.";
    }

    if (!priority) {
        errors.priority = "Priority is required.";
    }

    if (!status) {
        errors.status = "Status is required.";
    }

    return errors;
}

function validateGradeForm() {
    const errors = {};
    const courseId = elements.gradeForm.courseId.value;
    const assignmentName = elements.gradeForm.assignmentName.value.trim();
    const scoreRaw = elements.gradeForm.score.value;
    const maxScoreRaw = elements.gradeForm.maxScore.value;
    const weightRaw = elements.gradeForm.weight.value;

    const score = Number(scoreRaw);
    const maxScore = Number(maxScoreRaw);
    const weight = Number(weightRaw);

    if (!courseId) {
        errors.courseId = "Select a course.";
    }

    if (!assignmentName) {
        errors.assignmentName = "Assignment name is required.";
    } else if (assignmentName.length > 160) {
        errors.assignmentName = "Assignment name must be 160 characters or fewer.";
    }

    if (scoreRaw === "") {
        errors.score = "Score is required.";
    } else if (Number.isNaN(score) || score < 0) {
        errors.score = "Score must be zero or greater.";
    }

    if (maxScoreRaw === "") {
        errors.maxScore = "Max score is required.";
    } else if (Number.isNaN(maxScore) || maxScore <= 0) {
        errors.maxScore = "Max score must be greater than zero.";
    }

    if (weightRaw === "") {
        errors.weight = "Weight is required.";
    } else if (Number.isNaN(weight) || weight < 0 || weight > 100) {
        errors.weight = "Weight must be between 0 and 100.";
    }

    if (scoreRaw !== "" && maxScoreRaw !== "" && !Number.isNaN(score) && !Number.isNaN(maxScore) && score > maxScore) {
        errors.score = "Score must be less than or equal to max score.";
    }

    return errors;
}

function validateCanvasSettingsForm() {
    const errors = {};
    const baseUrl = elements.canvasSettingsForm.baseUrl.value.trim();
    const accessToken = elements.canvasSettingsForm.accessToken.value.trim();

    if (baseUrl && !/^https?:\/\/[^\s/$.?#].[^\s]*$/i.test(baseUrl)) {
        errors.baseUrl = "Enter a valid Canvas URL, such as https://school.instructure.com.";
    }

    if (baseUrl.length > 500) {
        errors.baseUrl = "Canvas URL must be 500 characters or fewer.";
    }

    if (accessToken.length > 500) {
        errors.accessToken = "Canvas token must be 500 characters or fewer.";
    }

    return errors;
}

function applyApiFieldErrors(form, fieldErrors = {}) {
    if (!fieldErrors || typeof fieldErrors !== "object") {
        return;
    }

    Object.entries(fieldErrors).forEach(([fieldName, message]) => {
        if (form.elements.namedItem(fieldName)) {
            setFieldError(form, fieldName, message);
        }
    });
}

function buildFormErrorMessage(error) {
    const hasFieldErrors = Boolean(error?.fieldErrors && Object.keys(error.fieldErrors).length);
    if (hasFieldErrors) {
        return FORM_VALIDATION_SUMMARY;
    }

    return error?.message || DEFAULT_API_ERROR_MESSAGE;
}

function setFieldError(form, fieldName, message) {
    const field = form.elements.namedItem(fieldName);
    if (!field) {
        return;
    }

    const errorElement = form.querySelector(`.field-error[data-field-error='${fieldName}']`);
    if (errorElement) {
        errorElement.textContent = message;
    }

    field.classList.add("field-invalid");
    field.setAttribute("aria-invalid", "true");
}

function clearFieldError(form, fieldName) {
    const field = form.elements.namedItem(fieldName);
    if (!field) {
        return;
    }

    const errorElement = form.querySelector(`.field-error[data-field-error='${fieldName}']`);
    if (errorElement) {
        errorElement.textContent = "";
    }

    field.classList.remove("field-invalid");
    field.removeAttribute("aria-invalid");
}

function clearFormErrors(form) {
    if (!form) {
        return;
    }

    const errorElements = form.querySelectorAll(".field-error");
    errorElements.forEach((errorElement) => {
        errorElement.textContent = "";
    });

    const invalidFields = form.querySelectorAll(".field-invalid");
    invalidFields.forEach((field) => {
        field.classList.remove("field-invalid");
        field.removeAttribute("aria-invalid");
    });
}

function isValidEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
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

function formatCanvasStatus(value) {
    return formatStatus(value);
}

function formatBucketLabel(value) {
    return formatStatus(value || "ALL");
}

function priorityClass(value) {
    return value.toLowerCase();
}

function priorityScoreClass(score) {
    if (score >= 70) {
        return "high";
    }
    if (score >= 40) {
        return "medium";
    }
    return "low";
}

function canvasStatusClass(value) {
    if (value === "COMPLETED") {
        return "done";
    }
    if (value === "OVERDUE" || value === "MISSING") {
        return "high";
    }
    if (value === "NO_DUE_DATE") {
        return "medium";
    }
    return "";
}

function formatNumber(value) {
    return Number(value).toLocaleString(undefined, {
        maximumFractionDigits: 2,
    });
}

function formatDateTime(value) {
    return new Date(value).toLocaleString(undefined, {
        month: "short",
        day: "numeric",
        year: "numeric",
        hour: "numeric",
        minute: "2-digit",
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

function buildCanvasTaskQuery() {
    const params = new URLSearchParams();
    params.set("bucket", state.canvas.bucket);
    return `/api/canvas/tasks?${params.toString()}`;
}

function buildCanvasSettingsPayload() {
    return {
        baseUrl: elements.canvasSettingsForm.baseUrl.value.trim(),
        accessToken: elements.canvasSettingsForm.accessToken.value.trim(),
    };
}

function formatSyncResult(result) {
    const warningText = result.warnings?.length ? ` ${result.warnings.join(" ")}` : "";
    return `Canvas sync saved ${result.tasksUpserted} tasks from ${result.assignmentsFetched} assignments. ${result.missingFromTodoCount} assignment(s) were missing from Canvas To-do.${warningText}`;
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
