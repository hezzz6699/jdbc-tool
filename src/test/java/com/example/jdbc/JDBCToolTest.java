package com.example.jdbc;

import com.example.jdbc.entity.College;
import com.example.jdbc.entity.Student;
import com.example.jdbc.util.DBUtil;
import com.example.jdbc.util.JDBCTool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JDBCTool 五个核心方法的单元测试。
 */
class JDBCToolTest {

    @BeforeAll
    static void initDatabase() {
        DBUtil.initDatabase();
    }

    @BeforeEach
    void cleanTables() throws Exception {
        try (Connection connection = DBUtil.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM student");
            statement.execute("DELETE FROM college");
        }
    }

    @Test
    @DisplayName("save(): 保存对象并回填自增主键，可按主键查询到")
    void testSaveAndGetOneById() throws Exception {
        try (Connection connection = DBUtil.getConnection()) {
            College college = new College(null, "计算机学院", "CS");
            assertEquals(1, JDBCTool.save(college, connection));
            assertNotNull(college.getId(), "保存后应回填自增主键");

            College found = JDBCTool.getOneById(String.valueOf(college.getId()), College.class, connection);
            assertNotNull(found);
            assertEquals("计算机学院", found.getName());
            assertEquals("CS", found.getCode());

            assertNull(JDBCTool.getOneById("99999", College.class, connection));
        }
    }

    @Test
    @DisplayName("resultSetToList(): 结果集正确映射为对象列表")
    void testResultSetToList() throws Exception {
        try (Connection connection = DBUtil.getConnection()) {
            JDBCTool.save(new Student(null, "张三", "软件工程", 20,
                    LocalDate.of(2022, 9, 1), false, 5800.00), connection);
            JDBCTool.save(new Student(null, "李四", "计算机科学与技术", 21,
                    LocalDate.of(2021, 9, 1), true, 6000.00), connection);

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT * FROM student ORDER BY id")) {
                List<Student> students = JDBCTool.resultSetToList(rs, Student.class);
                assertEquals(2, students.size());

                Student first = students.get(0);
                assertEquals("张三", first.getName());
                assertEquals("软件工程", first.getMajor());
                assertEquals(20, first.getAge());
                assertEquals(LocalDate.of(2022, 9, 1), first.getEnrollTime());
                assertEquals(Boolean.FALSE, first.getGraduated());
                assertEquals(5800.00, first.getTuition(), 0.001);
            }
        }
    }

    @Test
    @DisplayName("update(): 只更新非空字段")
    void testUpdate() throws Exception {
        try (Connection connection = DBUtil.getConnection()) {
            Student student = new Student(null, "王五", "网络工程", 19,
                    LocalDate.of(2023, 9, 1), false, 5500.00);
            JDBCTool.save(student, connection);
            Integer id = student.getId();

            Student edit = new Student();
            edit.setId(id);
            edit.setAge(21);
            edit.setGraduated(true);
            assertEquals(1, JDBCTool.update(edit, connection));

            Student found = JDBCTool.getOneById(String.valueOf(id), Student.class, connection);
            assertEquals(21, found.getAge());
            assertTrue(found.getGraduated());
            // 未被赋值的字段保持原值
            assertEquals("王五", found.getName());
            assertEquals(5500.00, found.getTuition(), 0.001);
        }
    }

    @Test
    @DisplayName("delete(): 按主键删除对象")
    void testDelete() throws Exception {
        try (Connection connection = DBUtil.getConnection()) {
            College first = new College(null, "外国语学院", "FL");
            College second = new College(null, "数学与统计学院", "MA");
            JDBCTool.save(first, connection);
            JDBCTool.save(second, connection);

            College toDelete = new College();
            toDelete.setId(first.getId());
            assertEquals(1, JDBCTool.delete(toDelete, connection));

            assertNull(JDBCTool.getOneById(String.valueOf(first.getId()), College.class, connection));
            assertNotNull(JDBCTool.getOneById(String.valueOf(second.getId()), College.class, connection));
        }
    }
}
