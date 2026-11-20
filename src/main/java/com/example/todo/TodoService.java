package com.example.todo;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TodoService {
    private final List<Todo> todos = new CopyOnWriteArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public List<Todo> findAll() {
        return List.copyOf(todos);
    }

    public Todo create(String title) {
        Todo todo = new Todo(nextId.getAndIncrement(), title, false);
        todos.add(todo);
        return todo;
    }
}
