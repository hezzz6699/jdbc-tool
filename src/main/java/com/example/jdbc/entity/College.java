package com.example.jdbc.entity;

import com.example.jdbc.annotation.Column;
import com.example.jdbc.annotation.Id;
import com.example.jdbc.annotation.Table;

import java.util.StringJoiner;

/**
 * 学院实体类，映射数据库表 college（学院表）。
 * 字段：id、name（学院名称）、code（学院代码）。
 */
@Table(name = "college")
public class College {

    /** 主键 */
    @Id
    @Column(name = "id")
    private Integer id;

    /** 学院名称 */
    @Column(name = "name")
    private String name;

    /** 学院代码 */
    @Column(name = "code")
    private String code;

    public College() {
    }

    public College(Integer id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "College{", "}")
                .add("id=" + id)
                .add("name='" + name + "'")
                .add("code='" + code + "'")
                .toString();
    }
}
