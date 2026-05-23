const state = {
    courses: [],
    tasks: [],
};

const elements = {
    tabs: document.querySelectorAll(".tab-button"),
    sections: document.querySelectorAll(".dashboard-section"),
    message: document.querySelector("#message"),
    courseForm: document.querySelector("#courseForm"),
    taskForm: document.querySelector("#taskForm"),
    courseList: document.querySelector("#courseList"),
    taskList: document.querySelector("#taskList"),
    gradeList: document.querySelector("#gradeList"),
    taskCourse: document.querySelector("#taskCourse"),
    taskDueDate: document.querySelector("#taskDueDate"),
    courseCount: document.querySelector("#courseCount"),
    openTaskCount: document.querySelector("#openTaskCount"),
    completionRate: document.querySelector("#completionRate"),
};

document.addEventListener("DOMContentLoaded", () => {
    elements.taskDueDate.min = getToday();
    elements.taskDueDate.value = getToday();
    setupTabs();
    setupForms();
    loadDashboard();
});

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
            userId: Number(elements.courseForm.userId.value),
        };

        await request("/api/courses", {
            method: "POST",
            body: JSON.stringify(payload),
        });

        elements.courseForm.reset();
        elements.courseForm.userId.value = "1";
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
}

async function loadDashboard() {
    try {
        showMessage("Loading dashboard...");
        state.courses = await request("/api/courses");
        state.tasks = await loadTasksForCourses(state.courses);
        render();
        showMessage("");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function loadTasksForCourses(courses) {
    const taskGroups = await Promise.all(
        courses.map((course) => request(`/api/courses/${course.id}/tasks`))
    );
    return taskGroups.flat();
}

async function request(url, options = {}) {
    const response = await fetch(url, {
        headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
            ...options.headers,
        },
        ...options,
    });

    if (!response.ok) {
        const fallback = `${response.status} ${response.statusText}`;
        let detail = fallback;

        try {
            const error = await response.json();
            detail = formatApiError(error) || fallback;
        } catch {
            detail = fallback;
        }

        throw new Error(detail);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

function render() {
    renderMetrics();
    renderCourseOptions();
    renderCourses();
    renderTasks();
    renderGrades();
}

function renderMetrics() {
    const completed = state.tasks.filter((task) => task.status === "DONE").length;
    const open = state.tasks.length - completed;
    const completion = state.tasks.length ? Math.round((completed / state.tasks.length) * 100) : 0;

    elements.courseCount.textContent = state.courses.length;
    elements.openTaskCount.textContent = open;
    elements.completionRate.textContent = `${completion}%`;
}

function renderCourseOptions() {
    if (!state.courses.length) {
        elements.taskCourse.innerHTML = '<option value="">Create a course first</option>';
        elements.taskCourse.disabled = true;
        elements.taskForm.querySelector("button").disabled = true;
        return;
    }

    elements.taskCourse.disabled = false;
    elements.taskForm.querySelector("button").disabled = false;
    elements.taskCourse.innerHTML = state.courses
        .map((course) => `<option value="${course.id}">${escapeHtml(course.name)}</option>`)
        .join("");
}

function renderCourses() {
    if (!state.courses.length) {
        elements.courseList.innerHTML = emptyState("No courses yet", "Create your first course to start planning.");
        return;
    }

    elements.courseList.innerHTML = state.courses
        .map((course) => {
            const taskCount = state.tasks.filter((task) => task.courseId === course.id).length;
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
                        <span class="badge">User ${course.userId}</span>
                        <span class="badge">${escapeHtml(course.userName || "Student")}</span>
                    </div>
                </article>
            `;
        })
        .join("");
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
        elements.gradeList.innerHTML = emptyState("No grade overview yet", "Create courses and tasks to build a progress snapshot.");
        return;
    }

    elements.gradeList.innerHTML = state.courses
        .map((course) => {
            const tasks = state.tasks.filter((task) => task.courseId === course.id);
            const done = tasks.filter((task) => task.status === "DONE").length;
            const percent = tasks.length ? Math.round((done / tasks.length) * 100) : 0;
            const label = tasks.length ? `${done} of ${tasks.length} tasks complete` : "No tasks tracked";

            return `
                <article class="grade-card">
                    <h3>${escapeHtml(course.name)}</h3>
                    <p class="grade-number">${percent}%</p>
                    <div class="progress-track" aria-label="${percent}% complete">
                        <div class="progress-bar" style="width: ${percent}%"></div>
                    </div>
                    <p>${label}</p>
                </article>
            `;
        })
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

function showMessage(text, type = "") {
    elements.message.textContent = text;
    elements.message.className = `message ${type}`.trim();
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
