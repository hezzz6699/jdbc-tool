package com.example.jdbc;

import com.example.jdbc.entity.College;
import com.example.jdbc.entity.Student;
import com.example.jdbc.util.DBUtil;
import com.example.jdbc.util.JDBCTool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 演示程序：使用 {@link JDBCTool} 对 DateTest 数据库中的 student（学生表）
 * 与 college（学院表）完成「增、删、改、查」的完整操作，并把运行过程打印到控制台。
 */
public class DemoMain {

    public static void main(String[] args) throws Exception {
        printBanner();

        // ---------- 0. 初始化数据库与数据表 ----------
        section("准备", "初始化数据库 DateTest 与数据表（college / student）");
        DBUtil.initDatabase();
        System.out.println("  [OK] 表 college、student 创建完成（CREATE TABLE IF NOT EXISTS）");
        System.out.println();

        try (Connection connection = DBUtil.getConnection()) {

            // ---------- 1. save()：新增 ----------
            section("演示 1 / save()", "使用反射把对象保存到数据库（INSERT）");

            List<College> colleges = Arrays.asList(
                    new College(null, "计算机学院", "CS"),
                    new College(null, "外国语学院", "FL"),
                    new College(null, "数学与统计学院", "MA"));
            for (College college : colleges) {
                int rows = JDBCTool.save(college, connection);
                System.out.printf("  %s   影响行数=%d%n", college, rows);
            }

            List<Student> students = Arrays.asList(
                    new Student(null, "张三", "软件工程", 20, LocalDate.of(2022, 9, 1), false, 5800.00),
                    new Student(null, "李四", "计算机科学与技术", 21, LocalDate.of(2021, 9, 1), true, 6000.00),
                    new Student(null, "王五", "网络工程", 19, LocalDate.of(2023, 9, 1), false, 5500.00),
                    new Student(null, "赵六", "人工智能", 22, LocalDate.of(2020, 9, 1), true, 7200.00),
                    new Student(null, "钱七", "数据科学", 20, LocalDate.of(2022, 9, 1), false, 6500.00));
            for (Student student : students) {
                int rows = JDBCTool.save(student, connection);
                System.out.printf("  %s   影响行数=%d%n", student, rows);
            }
            System.out.println();

            // ---------- 2. resultSetToList()：查询列表 ----------
            section("演示 2 / resultSetToList()", "把 ResultSet 转换为对象列表（SELECT）");

            printColleges(queryColleges(connection));
            System.out.println();
            printStudents(queryStudents(connection));
            System.out.println();

            // ---------- 3. getOneById()：按主键查询单个对象 ----------
            section("演示 3 / getOneById()", "根据主键查询单个对象");
            System.out.println("  getOneById(\"1\", College.class) => " + JDBCTool.getOneById("1", College.class, connection));
            System.out.println("  getOneById(\"2\", Student.class) => " + JDBCTool.getOneById("2", Student.class, connection));
            System.out.println();

            // ---------- 4. update()：更新 ----------
            section("演示 4 / update()", "根据主键更新对象（只更新非空字段，动态 SQL）");

            Student before = JDBCTool.getOneById("2", Student.class, connection);
            System.out.println("  更新前 => " + before);

            Student edit = new Student();
            edit.setId(2);                     // 主键：定位记录
            edit.setAge(23);                   // 只改年龄
            edit.setGraduated(true);           // 只改是否毕业
            edit.setTuition(6800.00);          // 只改学费
            int updated = JDBCTool.update(edit, connection);
            System.out.println("  JDBCTool.update(...) 影响行数=" + updated);
            System.out.println("  更新后 => " + JDBCTool.getOneById("2", Student.class, connection));
            System.out.println();

            // ---------- 5. delete()：删除 ----------
            section("演示 5 / delete()", "根据主键删除对象（DELETE）");

            int countBefore = queryStudents(connection).size();
            System.out.println("  删除前学生总数 = " + countBefore);

            Student toDelete = new Student();
            toDelete.setId(3);                 // 主键：待删除记录
            int deleted = JDBCTool.delete(toDelete, connection);
            System.out.println("  JDBCTool.delete(id=3) 影响行数=" + deleted);
            System.out.println("  删除后学生总数 = " + queryStudents(connection).size());
            System.out.println();

            // ---------- 6. 最终数据快照 ----------
            section("结果", "操作完成后 student 表的最终数据");
            printStudents(queryStudents(connection));
        }

        System.out.println();
        System.out.println("==================== 演示结束 ====================");
    }

    /** 查询全部学院 */
    private static List<College> queryColleges(Connection connection) throws Exception {
        String sql = "SELECT * FROM college ORDER BY id";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return JDBCTool.resultSetToList(rs, College.class);
        }
    }

    /** 查询全部学生 */
    private static List<Student> queryStudents(Connection connection) throws Exception {
        String sql = "SELECT * FROM student ORDER BY id";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return JDBCTool.resultSetToList(rs, Student.class);
        }
    }

    // ===================== 控制台美化输出 =====================

    private static void printBanner() {
        System.out.println("================================================================");
        System.out.println("         JDBC 工具类 JDBCTool 功能演示（数据库：DateTest）");
        System.out.println("----------------------------------------------------------------");
        System.out.println("  驱动：" + DBUtil.getDriver());
        System.out.println("  地址：" + DBUtil.getUrl());
        System.out.println("================================================================");
        System.out.println();
    }

    private static void section(String title, String subtitle) {
        System.out.println("--------------------------------------------------------------");
        System.out.println("【" + title + "】" + subtitle);
        System.out.println("--------------------------------------------------------------");
    }

    private static void printColleges(List<College> list) {
        List<String> headers = Arrays.asList("id", "name", "code");
        List<List<String>> rows = new ArrayList<>();
        for (College c : list) {
            rows.add(Arrays.asList(str(c.getId()), c.getName(), c.getCode()));
        }
        printTable("college 表（学院表）", headers, rows);
    }

    private static void printStudents(List<Student> list) {
        List<String> headers = Arrays.asList("id", "name", "major", "age", "enroll_time", "graduated", "tuition");
        List<List<String>> rows = new ArrayList<>();
        for (Student s : list) {
            rows.add(Arrays.asList(
                    str(s.getId()),
                    s.getName(),
                    s.getMajor(),
                    str(s.getAge()),
                    str(s.getEnrollTime()),
                    str(s.getGraduated()),
                    str(s.getTuition())));
        }
        printTable("student 表（学生表）", headers, rows);
    }

    private static void printTable(String title, List<String> headers, List<List<String>> rows) {
        int[] widths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            widths[i] = width(headers.get(i));
        }
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                widths[i] = Math.max(widths[i], width(row.get(i)));
            }
        }

        System.out.println("  " + title + "（共 " + rows.size() + " 条记录）");
        System.out.println("  " + border(widths));
        System.out.println("  |" + renderRow(headers, widths));
        System.out.println("  " + border(widths));
        for (List<String> row : rows) {
            System.out.println("  |" + renderRow(row, widths));
        }
        System.out.println("  " + border(widths));
    }

    private static String border(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        return sb.toString();
    }

    private static String renderRow(List<String> cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            String cell = cells.get(i) == null ? "" : cells.get(i);
            sb.append(" ").append(cell).append(" ".repeat(Math.max(0, widths[i] - width(cell)))).append(" |");
        }
        return sb.toString();
    }

    /** 计算字符串的显示宽度：中文等全角字符按 2 个宽度计算，保证表格对齐 */
    private static int width(String s) {
        if (s == null) {
            return 0;
        }
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += s.charAt(i) > 0x2E7F ? 2 : 1;
        }
        return w;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
