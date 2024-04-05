package com.example.petlocation.model;

import java.util.HashMap;
import java.util.Objects;

public class User {
    private String uuid,email;

    public User() {
    }

    public User(String uuid, String email) {
        this.uuid = uuid;
        this.email = email;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public HashMap<String, Object> convertHashMap(){
        HashMap<String,Object> u=new HashMap<>();
        u.put("uuid",uuid);
        u.put("email",email);
        return u;
    }
}
