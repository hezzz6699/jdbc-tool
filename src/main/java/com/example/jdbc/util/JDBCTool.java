package com.example.jdbc.util;

import com.example.jdbc.annotation.Column;
import com.example.jdbc.annotation.Id;
import com.example.jdbc.annotation.Table;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>JDBC 通用工具类（ORM 思想 + 反射实现）</h2>
 *
 * <p>该类把「数据库中的一张表 / 一行记录」与「Java 中的一个类 / 一个对象」建立起映射关系，
 * 即 ORM（Object Relational Mapping，对象关系映射）：
 * <ul>
 *     <li>类  ——  表（由 {@link Table} 指定）</li>
 *     <li>字段 —— 列（由 {@link Column} 指定，默认「驼峰转下划线」）</li>
 *     <li>对象 —— 一行记录</li>
 *     <li>标注 {@link Id} 的字段 —— 主键</li>
 * </ul>
 *
 * <p>核心能力（五个方法）：
 * <ol>
 *     <li>{@link #resultSetToList(ResultSet, Class)}：结果集 → 对象列表</li>
 *     <li>{@link #save(Object, Connection)}：新增一条记录</li>
 *     <li>{@link #update(Object, Connection)}：根据主键更新记录</li>
 *     <li>{@link #delete(Object, Connection)}：根据主键删除记录</li>
 *     <li>{@link #getOneById(String, Class, Connection)}：根据主键查询单个对象</li>
 * </ol>
 *
 * @author student
 * @version 1.0
 */
public final class JDBCTool {

    private JDBCTool() {
    }

    /** 缓存：类 -> 该类（含父类）的全部实例字段，避免每次反射重复解析 */
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    /** 缓存：类 -> （列名小写 -> 字段），用于结果集列名与字段的快速匹配 */
    private static final Map<Class<?>, Map<String, Field>> COLUMN_FIELD_CACHE = new ConcurrentHashMap<>();

    // ==================================================================================
    // 方法 1：static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz)
    // ==================================================================================

    /**
     * 将 {@link ResultSet} 转换为指定类型的对象列表。
     *
     * <p>实现原理：遍历结果集的每一行，通过反射调用目标类的无参构造方法创建对象，
     * 再把「结果集中的列」与「对象中的字段」按名称一一对应后通过反射赋值，最终返回对象列表。
     *
     * @param rs    已经执行查询得到的结果集
     * @param clazz 目标类的 Class 对象
     * @param <T>   目标类型
     * @return 对象列表（无记录时返回空列表，永不返回 null）
     */
    public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz) throws SQLException {
        List<T> list = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            // 1) 反射创建对象
            T instance = newInstance(clazz);
            // 2) 逐列取值并反射赋值
            for (int i = 1; i <= columnCount; i++) {
                String columnLabel = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                Field field = findFieldByColumn(clazz, columnLabel);
                if (field != null) {
                    setFieldValue(instance, field, value);
                }
            }
            list.add(instance);
        }
        return list;
    }

    // ==================================================================================
    // 方法 2：static <T> int save(T obj, Connection connection)
    // ==================================================================================

    /**
     * 保存（新增）一个对象到数据库（INSERT）。
     *
     * <p>表名由类上的 {@link Table} 注解决定；只插入「值不为 null 且不是主键」的字段，
     * 主键交由数据库自增生成。插入成功后，会把数据库生成的自增主键值回填到对象的
     * 主键字段中。所有参数使用预编译占位符（?），防止 SQL 注入。
     *
     * @param obj        待保存的对象
     * @param connection 数据库连接
     * @return 受影响的行数
     */
    public static <T> int save(T obj, Connection connection) throws SQLException {
        if (obj == null) {
            throw new IllegalArgumentException("保存的对象不能为 null");
        }
        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);
        Field idField = getPrimaryKeyField(clazz);

        // 收集需要插入的字段（排除 null 值与主键字段）
        List<Field> insertFields = new ArrayList<>();
        for (Field field : getFields(clazz)) {
            Object value = getFieldValue(obj, field);
            if (value == null) {
                continue;
            }
            if (idField != null && field.equals(idField)) {
                continue; // 主键为空时由数据库自增
            }
            insertFields.add(field);
        }
        if (insertFields.isEmpty()) {
            throw new IllegalStateException("对象 [" + clazz.getSimpleName() + "] 没有可插入的非空字段");
        }

        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < insertFields.size(); i++) {
            if (i > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(getColumnName(insertFields.get(i)));
            placeholders.append("?");
        }

        String sql = "INSERT INTO " + tableName
                + " (" + columns + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < insertFields.size(); i++) {
                ps.setObject(i + 1, toSqlValue(getFieldValue(obj, insertFields.get(i))));
            }
            int rows = ps.executeUpdate();
            // 把数据库生成的自增主键回填到对象的主键字段
            if (rows > 0 && idField != null) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        setFieldValue(obj, idField, keys.getObject(1));
                    }
                }
            }
            return rows;
        }
    }

    // ==================================================================================
    // 方法 3：public static <T> int update(T obj, Connection connection)
    // ==================================================================================

    /**
     * 根据主键更新一个对象（UPDATE）。
     *
     * <p>只更新「值不为 null 且不是主键」的字段，即按需更新（动态 SQL）。
     *
     * @param obj        待更新的对象（主键字段必须有值）
     * @param connection 数据库连接
     * @return 受影响的行数
     */
    public static <T> int update(T obj, Connection connection) throws SQLException {
        if (obj == null) {
            throw new IllegalArgumentException("更新的对象不能为 null");
        }
        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);
        Field idField = getPrimaryKeyField(clazz);
        if (idField == null) {
            throw new IllegalStateException("类 [" + clazz.getSimpleName() + "] 未找到主键字段（请使用 @Id 标注）");
        }
        Object idValue = getFieldValue(obj, idField);
        if (idValue == null) {
            throw new IllegalStateException("更新时必须为主键字段 [" + idField.getName() + "] 指定值");
        }

        List<Field> setFields = new ArrayList<>();
        for (Field field : getFields(clazz)) {
            if (field.equals(idField)) {
                continue;
            }
            if (getFieldValue(obj, field) == null) {
                continue;
            }
            setFields.add(field);
        }
        if (setFields.isEmpty()) {
            return 0; // 没有需要更新的字段
        }

        StringBuilder setClause = new StringBuilder();
        for (int i = 0; i < setFields.size(); i++) {
            if (i > 0) {
                setClause.append(", ");
            }
            setClause.append(getColumnName(setFields.get(i))).append(" = ?");
        }

        String sql = "UPDATE " + tableName + " SET " + setClause
                + " WHERE " + getColumnName(idField) + " = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            for (Field field : setFields) {
                ps.setObject(index++, toSqlValue(getFieldValue(obj, field)));
            }
            ps.setObject(index, toSqlValue(idValue));
            return ps.executeUpdate();
        }
    }

    // ==================================================================================
    // 方法 4：public static <T> int delete(T obj, Connection connection)
    // ==================================================================================

    /**
     * 根据主键删除一个对象（DELETE）。
     *
     * @param obj        待删除的对象（主键字段必须有值）
     * @param connection 数据库连接
     * @return 受影响的行数
     */
    public static <T> int delete(T obj, Connection connection) throws SQLException {
        if (obj == null) {
            throw new IllegalArgumentException("删除的对象不能为 null");
        }
        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);
        Field idField = getPrimaryKeyField(clazz);
        if (idField == null) {
            throw new IllegalStateException("类 [" + clazz.getSimpleName() + "] 未找到主键字段（请使用 @Id 标注）");
        }
        Object idValue = getFieldValue(obj, idField);
        if (idValue == null) {
            throw new IllegalStateException("删除时必须为主键字段 [" + idField.getName() + "] 指定值");
        }

        String sql = "DELETE FROM " + tableName + " WHERE " + getColumnName(idField) + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, toSqlValue(idValue));
            return ps.executeUpdate();
        }
    }

    // ==================================================================================
    // 方法 5：public static <T> T getOneById(String id, Class<T> clazz, Connection connection)
    // ==================================================================================

    /**
     * 根据主键查询单个对象（SELECT ... WHERE 主键 = ?）。
     *
     * @param id         主键值（字符串形式，内部会自动转换为字段类型）
     * @param clazz      目标类的 Class 对象
     * @param connection 数据库连接
     * @param <T>        目标类型
     * @return 查询到的对象；不存在时返回 null
     */
    public static <T> T getOneById(String id, Class<T> clazz, Connection connection) throws SQLException {
        String tableName = getTableName(clazz);
        Field idField = getPrimaryKeyField(clazz);
        if (idField == null) {
            throw new IllegalStateException("类 [" + clazz.getSimpleName() + "] 未找到主键字段（请使用 @Id 标注）");
        }

        String sql = "SELECT * FROM " + tableName + " WHERE " + getColumnName(idField) + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, convert(id, idField.getType()));
            try (ResultSet rs = ps.executeQuery()) {
                List<T> list = resultSetToList(rs, clazz);
                return list.isEmpty() ? null : list.get(0);
            }
        }
    }

    // ==================================================================================
    // 以下为支撑上述五个方法的辅助方法
    // ==================================================================================

    /** 通过无参构造方法反射创建对象实例 */
    private static <T> T newInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "无法通过无参构造方法创建对象：" + clazz.getName()
                            + "，请确保该实体类存在公开或无参构造方法", e);
        }
    }

    /** 获取类的表名：优先使用 @Table，否则使用类名驼峰转下划线小写 */
    public static String getTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return camelToSnake(clazz.getSimpleName());
    }

    /** 获取字段的列名：优先使用 @Column，否则使用字段名驼峰转下划线小写 */
    public static String getColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isEmpty()) {
            return column.name();
        }
        return camelToSnake(field.getName());
    }

    /** 获取类（含父类）所有非静态、非瞬态字段，并设置可访问 */
    private static List<Field> getFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            List<Field> fields = new ArrayList<>();
            Class<?> current = c;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                        continue;
                    }
                    field.setAccessible(true);
                    fields.add(field);
                }
                current = current.getSuperclass();
            }
            return fields;
        });
    }

    /** 获取主键字段：优先 @Id 标注，其次取名为 id 的字段 */
    private static Field getPrimaryKeyField(Class<?> clazz) {
        List<Field> fields = getFields(clazz);
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        for (Field field : fields) {
            if ("id".equalsIgnoreCase(field.getName())) {
                return field;
            }
        }
        return null;
    }

    /** 根据结果集的列名查找对应的字段（大小写不敏感，并兼容直接用字段名匹配） */
    private static Field findFieldByColumn(Class<?> clazz, String columnLabel) {
        if (columnLabel == null) {
            return null;
        }
        Map<String, Field> mapping = COLUMN_FIELD_CACHE.computeIfAbsent(clazz, c -> {
            Map<String, Field> map = new LinkedHashMap<>();
            for (Field field : getFields(c)) {
                map.put(getColumnName(field).toLowerCase(), field);
                map.put(field.getName().toLowerCase(), field);
            }
            return map;
        });
        return mapping.get(columnLabel.toLowerCase());
    }

    /** 通过反射读取字段的值 */
    private static Object getFieldValue(Object obj, Field field) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("读取字段 [" + field.getName() + "] 失败", e);
        }
    }

    /** 通过反射为字段赋值，并进行必要的类型转换 */
    private static void setFieldValue(Object obj, Field field, Object value) {
        Class<?> targetType = field.getType();
        if (value == null) {
            if (targetType.isPrimitive()) {
                return; // 基本类型不能赋 null，跳过
            }
            try {
                field.set(obj, null);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("为字段 [" + field.getName() + "] 赋值 null 失败", e);
            }
            return;
        }
        try {
            if (targetType.isInstance(value)) {
                field.set(obj, value);
            } else {
                field.set(obj, convert(value, targetType));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "为字段 [" + field.getName() + "] 赋值失败，值=" + value, e);
        }
    }

    /**
     * 把数据库返回的值转换为字段所需的类型。
     * 支持 String / int / long / short / byte / double / float / BigDecimal / boolean / 日期类型。
     */
    private static Object convert(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType == String.class || targetType == Object.class) {
            return value.toString();
        }
        if (targetType == Integer.class || targetType == int.class) {
            return value instanceof Number ? ((Number) value).intValue() : Integer.valueOf(value.toString().trim());
        }
        if (targetType == Long.class || targetType == long.class) {
            return value instanceof Number ? ((Number) value).longValue() : Long.valueOf(value.toString().trim());
        }
        if (targetType == Short.class || targetType == short.class) {
            return value instanceof Number ? ((Number) value).shortValue() : Short.valueOf(value.toString().trim());
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return value instanceof Number ? ((Number) value).byteValue() : Byte.valueOf(value.toString().trim());
        }
        if (targetType == Double.class || targetType == double.class) {
            return value instanceof Number ? ((Number) value).doubleValue() : Double.valueOf(value.toString().trim());
        }
        if (targetType == Float.class || targetType == float.class) {
            return value instanceof Number ? ((Number) value).floatValue() : Float.valueOf(value.toString().trim());
        }
        if (targetType == BigDecimal.class) {
            return value instanceof BigDecimal ? value : new BigDecimal(value.toString().trim());
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
            String s = value.toString().trim();
            return "1".equals(s) || Boolean.parseBoolean(s);
        }
        if (targetType == LocalDate.class) {
            if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate();
            }
            if (value instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) value).toLocalDateTime().toLocalDate();
            }
            if (value instanceof java.util.Date) {
                return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
            }
            return LocalDate.parse(value.toString().trim());
        }
        if (targetType == LocalDateTime.class) {
            if (value instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) value).toLocalDateTime();
            }
            if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate().atStartOfDay();
            }
            return LocalDateTime.parse(value.toString().trim().replace(' ', 'T'));
        }
        if (targetType == java.util.Date.class) {
            if (value instanceof java.util.Date) {
                return value;
            }
            return java.sql.Timestamp.valueOf(value.toString().trim());
        }
        return value;
    }

    /** 将 Java 8 时间类型转换为 JDBC 支持的 java.sql 类型 */
    private static Object toSqlValue(Object value) {
        if (value instanceof LocalDate) {
            return java.sql.Date.valueOf((LocalDate) value);
        }
        if (value instanceof LocalDateTime) {
            return java.sql.Timestamp.valueOf((LocalDateTime) value);
        }
        return value;
    }

    /** 驼峰命名转下划线命名：enrollTime -> enroll_time */
    private static String camelToSnake(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
