package com.studyflow.canvas;

import com.studyflow.canvas.model.CanvasAssignment;
import com.studyflow.canvas.model.CanvasPlannerItem;
import com.studyflow.canvas.model.CanvasTodoItem;
import com.studyflow.canvas.model.Course;
import java.util.List;

public interface CanvasApiClient {
    boolean testConnection();

    List<Course> getCourses();

    List<CanvasAssignment> getAssignments(Long courseId);

    List<CanvasTodoItem> getTodoItems();

    List<CanvasPlannerItem> getPlannerItems();
}
