package com.scc.model;

public record User(int Id, String Username, String Password, String Roles, String Status) {
    @Override
    public String toString() {
        return String.format("[%d] %s (%s) - Status: %s", Id, Username, Roles, Status);
    }
}