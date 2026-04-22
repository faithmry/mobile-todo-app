package com.example.to_dolistapp

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.LocalDateTime

class TaskViewModel : ViewModel() {
    private val _tasks = mutableStateOf<List<Task>>(emptyList())
    val tasks: State<List<Task>> = _tasks

    private val _sortOrder = mutableStateOf(SortOrder.DEADLINE)
    val sortOrder: State<SortOrder> = _sortOrder

    private val _viewMode = mutableStateOf(ViewMode.ALL)
    val viewMode: State<ViewMode> = _viewMode

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    enum class SortOrder {
        DEADLINE, STATUS
    }

    enum class ViewMode {
        ALL, DAILY
    }

    init {
        _tasks.value = listOf(
            Task(
                title = "Mengerjakan Projek Android",
                deadline = LocalDateTime.now().plusHours(2),
                isCompleted = false
            ),
            Task(
                title = "Beli Kopi Susu",
                deadline = LocalDateTime.now().plusDays(1),
                isCompleted = false
            ),
            Task(
                title = "Olahraga ",
                deadline = LocalDateTime.now().minusDays(1),
                isCompleted = true
            ),
            Task(
                title = "Kelas Mobile Programming",
                deadline = LocalDateTime.now().plusHours(5),
                isCompleted = false
            ),
            Task(
                title = "Belajar Jetpack Compose",
                deadline = LocalDateTime.now().plusDays(2),
                isCompleted = false
            )
        )
        sortTasks() 
    }

    fun addTask(title: String, deadline: LocalDateTime) {
        val newTask = Task(title = title, deadline = deadline)
        _tasks.value = _tasks.value + newTask
        sortTasks()
    }

    fun updateTask(id: String, title: String, deadline: LocalDateTime) {
        _tasks.value = _tasks.value.map {
            if (it.id == id) it.copy(title = title, deadline = deadline) else it
        }
        sortTasks()
    }

    fun removeTask(task: Task) {
        _tasks.value = _tasks.value.filter { it.id != task.id }
    }

    fun toggleTaskCompletion(task: Task) {
        _tasks.value = _tasks.value.map {
            if (it.id == task.id) it.copy(isCompleted = !it.isCompleted) else it
        }
        sortTasks()
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        sortTasks()
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    private fun sortTasks() {
        _tasks.value = when (_sortOrder.value) {
            SortOrder.DEADLINE -> _tasks.value.sortedBy { it.deadline }
            SortOrder.STATUS -> _tasks.value.sortedWith(compareBy({ it.isCompleted }, { it.deadline }))
        }
    }
}