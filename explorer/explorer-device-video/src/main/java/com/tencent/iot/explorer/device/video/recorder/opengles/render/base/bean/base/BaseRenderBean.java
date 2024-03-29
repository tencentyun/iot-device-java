package com.example.ivdemo.recorder.opengles.render.base.bean.base;

import java.io.Serializable;

public class BaseRenderBean implements Serializable {
    private int type;
    private String name;

    public BaseRenderBean(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "BaseRenderBean{" +
                "type=" + type +
                ", name='" + name + '\'' +
                '}';
    }
}
