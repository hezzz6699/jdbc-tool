package com.example.jdbc.entity;

import com.example.jdbc.annotation.Column;
import com.example.jdbc.annotation.Id;
import com.example.jdbc.annotation.Table;

import java.time.LocalDate;
import java.util.StringJoiner;

/**
 * 学生实体类，映射数据库表 student（学生表）。
 * 字段：Id、name（姓名）、major（专业）、age（年龄）、入学时间、是否毕业、学费。
 */
@Table(name = "student")
public class Student {

    /** 主键 */
    @Id
    @Column(name = "id")
    private Integer id;

    /** 姓名 */
    @Column(name = "name")
    private String name;

    /** 专业 */
    @Column(name = "major")
    private String major;

    /** 年龄 */
    @Column(name = "age")
    private Integer age;

    /** 入学时间 */
    @Column(name = "enroll_time")
    private LocalDate enrollTime;

    /** 是否毕业 */
    @Column(name = "graduated")
    private Boolean graduated;

    /** 学费 */
    @Column(name = "tuition")
    private Double tuition;

    public Student() {
    }

    public Student(Integer id, String name, String major, Integer age,
                   LocalDate enrollTime, Boolean graduated, Double tuition) {
        this.id = id;
        this.name = name;
        this.major = major;
        this.age = age;
        this.enrollTime = enrollTime;
        this.graduated = graduated;
        this.tuition = tuition;
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

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDate getEnrollTime() {
        return enrollTime;
    }

    public void setEnrollTime(LocalDate enrollTime) {
        this.enrollTime = enrollTime;
    }

    public Boolean getGraduated() {
        return graduated;
    }

    public void setGraduated(Boolean graduated) {
        this.graduated = graduated;
    }

    public Double getTuition() {
        return tuition;
    }

    public void setTuition(Double tuition) {
        this.tuition = tuition;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "Student{", "}")
                .add("id=" + id)
                .add("name='" + name + "'")
                .add("major='" + major + "'")
                .add("age=" + age)
                .add("enrollTime=" + enrollTime)
                .add("graduated=" + graduated)
                .add("tuition=" + tuition)
                .toString();
    }
}
