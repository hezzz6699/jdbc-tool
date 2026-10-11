-- ============================================================
--  数据库 DateTest 建表脚本
--  默认运行在 H2(MySQL 兼容模式) 或 MySQL 上均可执行
-- ============================================================

-- 学院表 college：id、name、code
CREATE TABLE IF NOT EXISTS college (
    id   INT AUTO_INCREMENT PRIMARY KEY,   -- 主键，自增
    name VARCHAR(50) NOT NULL,             -- 学院名称
    code VARCHAR(20) NOT NULL              -- 学院代码
);

-- 学生表 student：Id、name、major、age、入学时间、是否毕业、学费
CREATE TABLE IF NOT EXISTS student (
    id          INT AUTO_INCREMENT PRIMARY KEY,  -- 主键，自增
    name        VARCHAR(50)  NOT NULL,           -- 姓名
    major       VARCHAR(50),                     -- 专业
    age         INT,                             -- 年龄
    enroll_time DATE,                            -- 入学时间
    graduated   BOOLEAN,                         -- 是否毕业
    tuition     DECIMAL(10, 2)                   -- 学费
);
