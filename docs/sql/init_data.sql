-- ============================================================
-- 初始化数据
-- ============================================================

-- 默认管理员（密码: admin123）
INSERT INTO sys_user (id, username, password, nickname, email, gender, remark)
VALUES (1, 'admin', '$2b$10$70LszmiaWXHW/FkcO2e1tuz9Y7QAH2gl5KkCsAp/1c0uwNMhINDQ.', '管理员', 'admin@example.com', 'MALE', '系统管理员');

-- 角色
INSERT INTO sys_role (id, role_name, role_key, sort, remark)
VALUES (1, 'admin', 'admin', 1, '超级管理员');

-- 用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 字典类型
INSERT INTO sys_dict_type (id, dict_name, dict_type) VALUES (1, '业务类型', 'biz_type');
INSERT INTO sys_dict_type (id, dict_name, dict_type) VALUES (2, '任务分组', 'job_group');

-- 字典数据：业务类型
INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort) VALUES (1, 'biz_type', '通用', '0', 0);
INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort) VALUES (2, 'biz_type', '系统管理', '1', 1);
-- 字典数据：任务分组
INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort) VALUES (3, 'job_group', '系统任务', 'system', 0);

-- 系统参数
INSERT INTO sys_param (id, param_key, param_value, param_type, biz_type, encrypted, description)
VALUES (1, 'mail.host', 'smtp.example.com', 'STRING', 0, FALSE, 'SMTP 服务器地址'),
       (2, 'mail.port', '587', 'INTEGER', 0, FALSE, 'SMTP 端口'),
       (3, 'mail.username', 'your-email@example.com', 'STRING', 0, FALSE, 'SMTP 用户名'),
       (4, 'mail.password', '', 'STRING', 0, FALSE, 'SMTP 密码'),
       (5, 'mail.from', 'Ray Boot <your-email@example.com>', 'STRING', 0, FALSE, '发件人地址');

-- 定时任务
INSERT INTO sys_job (id, job_name, job_group, bean_name, method_name, cron_expression, enabled, remark)
VALUES (1, 'Refresh Token清理', 'system', 'refreshTokenCleanTask', 'cleanExpiredTokens', '0/10 * * * * ?', FALSE, '定时清理refresh token');

-- ==================== 权限/菜单 ====================

-- 系统管理（目录）
INSERT INTO sys_permission (id, parent_id, name, type, url, redirect, icon, sort, description)
VALUES (100, 0, '系统管理', 'DIRECTORY', '/system', '/system/user', 'SettingsOutline', 1, '系统管理目录');

-- 管理员管理（菜单）
INSERT INTO sys_permission (id, parent_id, name, type, url, component, route_name, icon, sort, keep_alive, description)
VALUES (101, 100, '用户管理', 'MENU', 'user', 'system/user/index', 'UserPage', 'PeopleOutline', 1, TRUE, '管理员管理页面');

-- 角色管理（菜单）
INSERT INTO sys_permission (id, parent_id, name, type, url, component, route_name, icon, sort, keep_alive, description)
VALUES (102, 100, '角色管理', 'MENU', 'role', 'system/role/index', 'RolePage', 'ShieldOutline', 2, TRUE, '角色管理页面');

-- 部门管理（菜单）
INSERT INTO sys_permission (id, parent_id, name, type, url, component, route_name, icon, sort, keep_alive, description)
VALUES (103, 100, '部门管理', 'MENU', 'dept', 'system/dept/index', 'DeptPage', 'AppsOutline', 3, TRUE, '部门管理页面');

-- 菜单管理（菜单）
INSERT INTO sys_permission (id, parent_id, name, type, url, component, route_name, icon, sort, keep_alive, description)
VALUES (104, 100, '菜单管理', 'MENU', 'menu', 'system/menu/index', 'MenuPage', 'ListOutline', 4, TRUE, '菜单/权限管理页面');

-- 字典管理（菜单）
INSERT INTO sys_permission (id, parent_id, name, type, url, component, route_name, icon, sort, keep_alive, description)
VALUES (105, 100, '字典管理', 'MENU', 'dict', 'system/dict/index', 'DictPage', 'DocumentOutline', 5, TRUE, '字典管理页面');

-- 系统参数（菜单）
INSERT INTO sys_permission (id, parent_id, name, type, url, component, route_name, icon, sort, keep_alive, description)
VALUES (106, 100, '系统参数', 'MENU', 'param', 'system/param/index', 'ParamPage', 'SettingsOutline', 6, TRUE, '系统参数管理页面');

-- 在线用户（菜单）
INSERT INTO sys_permission (id, parent_id, name, type, url, component, route_name, icon, sort, description)
VALUES (107, 100, '在线用户', 'MENU', 'online', 'system/online/index', 'OnlinePage', 'PeopleOutline', 7, '在线用户管理页面');

-- 在线用户按钮权限
INSERT INTO sys_permission (id, parent_id, name, type, code, sort, hidden, description)
VALUES (10701, 107, '查询列表', 'BUTTON', 'system:online:list', 1, TRUE, '查询在线用户列表');
INSERT INTO sys_permission (id, parent_id, name, type, code, sort, hidden, description)
VALUES (10702, 107, '强制下线', 'BUTTON', 'system:online:kick', 2, TRUE, '强制下线在线用户');

-- 任务管理（菜单）
INSERT INTO sys_permission (id, parent_id, name, type, url, component, route_name, icon, sort, keep_alive, description)
VALUES (200, 0, '定时任务', 'MENU', '/job', 'schedule/job/index', 'ScheduleJobPage', 'CalendarOutline', 1, TRUE, '定时任务管理页面');
