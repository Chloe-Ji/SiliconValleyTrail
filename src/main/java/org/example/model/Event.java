package org.example.model;

public record Event(String description, String choice1, String choice2,
                    Effects choice1Effects, Effects choice2Effects) {}
