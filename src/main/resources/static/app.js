const state = {
    courses: [],
    tasks: [],
    grades: [],
};

const elements = {
    tabs: document.querySelectorAll(".tab-button"),
    sections: document.querySelectorAll(".dashboard-section"),
    message: document.querySelector("#message"),
    courseForm: document.querySelector("#courseForm"),
    taskForm: document.querySelector("#taskForm"),
    gradeForm: document.querySelector("#gradeForm"),
    courseList: document.querySelector("#courseList"),
    taskList: document.querySelector("#taskList"),
    gradeList: document.querySelector("#gradeList"),
    taskCourse: document.querySelector("#taskCourse"),
    gradeCourse: document.querySelector("#gradeCourse"),
    gradeFormTitle: document.querySelector("#gradeFormTitle"),
    gradeSubmitButton: document.querySelector("#gradeSubmitButton"),
    gradeCancelButton: document.querySelector("#gradeCancelButton"),
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

async function loadDashboard() {
    try {
        showMessage("Loading dashboard...");
        state.courses = await request("/api/courses");
        const [tasks, grades] = await Promise.all([
            loadTasksForCourses(state.courses),
            loadGradesForCourses(state.courses),
        ]);
        state.tasks = tasks;
        state.grades = grades;
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

async function loadGradesForCourses(courses) {
    const gradeGroups = await Promise.all(
        courses.map((course) => request(`/api/courses/${course.id}/grades`))
    );
    return gradeGroups.flat();
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
    renderCourseOptions(elements.taskCourse, elements.taskForm, "Create a course first");
    renderCourseOptions(elements.gradeCourse, elements.gradeForm, "Create a course first");
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
            const taskCount = state.tasks.filter((task) => task.courseId === course.id).length;
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
                        <span class="badge">User ${course.userId}</span>
                        <span class="badge">${escapeHtml(course.userName || "Student")}</span>
                        <span class="badge">${gradeCount} grades</span>
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
