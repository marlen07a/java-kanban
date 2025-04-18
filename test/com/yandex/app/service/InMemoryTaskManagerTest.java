package com.yandex.app.service;

import com.yandex.app.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest {
    private TaskManager taskManager;

    @BeforeEach
    void setUp() {
        taskManager = Managers.getDefault();
    }

    @Test
    void canAddAndFindTaskById() {
        Task task = new Task("Task 1", "Description 1", Status.NEW);
        int taskId = taskManager.addTask(task);
        Task received = taskManager.getTask(taskId);
        assertNotNull(received, "Task should be found by ID");
        assertEquals(task, received, "Received task should match added task");
    }

    @Test
    void canAddAndFindEpicById() {
        Epic epic = new Epic("Epic 1", "Description 1");
        int epicId = taskManager.addEpic(epic);
        Epic received = taskManager.getEpic(epicId);
        assertNotNull(received, "Epic should be found by ID");
        assertEquals(epic, received, "Received epic should match added epic");
    }

    @Test
    void canAddAndFindSubtaskById() {
        Epic epic = new Epic("Epic 1", "Description 1");
        int epicId = taskManager.addEpic(epic);
        Subtask subtask = new Subtask(epicId, "Subtask 1", "Description 1", Status.NEW);
        int subtaskId = taskManager.addSubtask(subtask);
        Subtask received = taskManager.getSubtask(subtaskId);
        assertNotNull(received, "Subtask should be found by ID");
        assertEquals(subtask, received, "Received subtask should match added subtask");
    }

    @Test
    void tasksWithGeneratedAndSetIdsDoNotConflict() {
        Task task1 = new Task("Task 1", "Description 1", Status.NEW);
        Task task2 = new Task(100, "Task 2", "Description 2", Status.NEW);
        int id1 = taskManager.addTask(task1);
        int id2 = taskManager.addTask(task2);
        assertNotEquals(id1, id2, "Generated and set ID should not conflict");
        assertEquals(taskManager.getTask(100), task2, "Task with set ID should be found and match added task");
    }

    @Test
    void taskShouldBeAddedWithoutChanging() {
        Task original = new Task("Task 1", "Description 1", Status.NEW);
        int id = taskManager.addTask(original);
        Task received = taskManager.getTask(id);
        assertEquals(original.getName(), received.getName(), "Name should remain unchanged");
        assertEquals(original.getDescription(), received.getDescription(), "Description should remain unchanged");
        assertEquals(original.getStatus(), received.getStatus(), "Status should remain unchanged");
    }

    @Test
    void epicCannotBeAddedAsItsOwnSubtask() {
        Epic epic = new Epic("Epic 1", "Description 1");
        int epicId = taskManager.addEpic(epic);
        Subtask subtask = new Subtask(epicId, epicId, "Subtask 1", "Description 1", Status.NEW);
        int subtaskId = taskManager.addSubtask(subtask);
        assertEquals(-1, subtaskId, "Adding a subtask with the same ID as its epic should return -1");
        assertFalse(epic.getSubtaskIds().contains(epicId), "Epic should not be added as its own subtask");
    }

    @Test
    void subtaskCannotBeItsOwnEpic() {
        Epic epic = new Epic("Epic 1", "Description 1");
        int epicId = taskManager.addEpic(epic);
        Subtask subtask = new Subtask(epicId, "Subtask 1", "Desc 1", Status.NEW);
        int subtaskId = taskManager.addSubtask(subtask);
        assertEquals(epicId, subtask.getEpicId(), "Subtask should reference its epic");
    }

    @Test
    void historyRecordsTasksCorrectly() {
        Task task = new Task("Task 1", "Description 1", Status.NEW);
        Epic epic = new Epic("Epic 1", "Description 1");
        int taskId = taskManager.addTask(task);
        int epicId = taskManager.addEpic(epic);
        taskManager.getTask(taskId);
        taskManager.getEpic(epicId);
        List<Task> history = taskManager.getHistory();
        assertEquals(2, history.size(), "History should contain two tasks");
        assertTrue(history.contains(task), "History should contain task");
        assertTrue(history.contains(epic), "History should contain epic");
    }

    @Test
    void epicStatusUpdatesCorrectly() {
        Epic epic = new Epic("Epic 1", "Description 1");
        int epicId = taskManager.addEpic(epic);
        Subtask subtask1 = new Subtask(epicId, "Subtask 1", "Description 1", Status.NEW);
        Subtask subtask2 = new Subtask(epicId, "Subtask 2", "Description 2", Status.DONE);
        taskManager.addSubtask(subtask1);
        taskManager.addSubtask(subtask2);
        assertEquals(Status.IN_PROGRESS, epic.getStatus(), "Epic status should be IN_PROGRESS");
    }

    @Test
    void clearSubtasksRemovesAllSubtasksAndClearsEpicSubtaskIds() {

        Epic epic = new Epic("Epic 1", "Description 1");
        int epicId = taskManager.addEpic(epic);
        Subtask subtask1 = new Subtask(epicId, "Subtask 1", "Description 1", Status.NEW);
        Subtask subtask2 = new Subtask(epicId, "Subtask 2", "Description 2", Status.DONE);
        int subtaskId1 = taskManager.addSubtask(subtask1);
        int subtaskId2 = taskManager.addSubtask(subtask2);

        assertEquals(2, taskManager.getAllSubtasks().size(), "should be 2 subtasks initially");
        assertEquals(2, epic.getSubtaskIds().size(), "Epic should have 2 subtask IDs initially");

        taskManager.clearSubtasks();

        assertTrue(taskManager.getAllSubtasks().isEmpty(), "Subtasks should be empty after clearSubtasks");
        assertTrue(epic.getSubtaskIds().isEmpty(), "Epic's subtask IDs should be empty after clearSubtasks");
        assertEquals(Status.NEW, epic.getStatus(), "Epic status should be NEW with no subtasks");
    }

    @Test
    void updateTaskReplacesExistingTask() {
        Task originalTask = new Task("Task 1", "Description 1", Status.NEW);
        int taskId = taskManager.addTask(originalTask);

        Task updatedTask = new Task(taskId, "Task 2", "Description 2", Status.IN_PROGRESS);

        taskManager.updateTask(updatedTask);

        Task receivedTask = taskManager.getTask(taskId);
        assertEquals(updatedTask, receivedTask, "Task should be updated to the new one");
        assertEquals("Task 2", receivedTask.getName(), "Task name should be updated");
        assertEquals("Description 2", receivedTask.getDescription(), "Task description should be updated");
        assertEquals(Status.IN_PROGRESS, receivedTask.getStatus(), "Task status should be updated");
    }

    @Test
    void updateEpicDoesNothingIfEpicIdNotFound() {
        Epic epic = new Epic("Epic 1", "Description 1");
        int epicId = taskManager.addEpic(epic);

        Epic randomEpic = new Epic(981, "random Epic", "Description random");

        taskManager.updateEpic(randomEpic);

        assertEquals(1, taskManager.getAllEpics().size(), "Number of epics should remain 1");
        assertNull(taskManager.getEpic(981), "random epic should not be added");
        assertEquals(epic, taskManager.getEpic(epicId), "Original epic should remain unchanged");
    }

    @Test
    void updateSubtaskReplacesExistingSubtaskAndUpdatesEpicStatus() {
        Epic epic = new Epic("Epic 1", "Description 1");
        int epicId = taskManager.addEpic(epic);
        Subtask originalSubtask = new Subtask(epicId, "Subtask 1", "Description 1", Status.NEW);
        int subtaskId = taskManager.addSubtask(originalSubtask);

        Subtask updatedSubtask = new Subtask(epicId, subtaskId, "Subtask 2", "Description 2", Status.DONE);

        taskManager.updateSubtask(updatedSubtask);

        Subtask receivedSubtask = taskManager.getSubtask(subtaskId);
        assertEquals(updatedSubtask, receivedSubtask, "Subtask should be updated to the new version");
        assertEquals("Subtask 2", receivedSubtask.getName(), "Subtask name should be updated");
        assertEquals("Description 2", receivedSubtask.getDescription(), "Subtask description should be updated");
        assertEquals(Status.DONE, receivedSubtask.getStatus(), "Subtask status should be updated");
        assertEquals(Status.DONE, epic.getStatus(), "Epic status should update to DONE with all subtasks DONE");
    }
}