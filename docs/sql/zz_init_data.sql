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

-- 管理接口按钮权限
INSERT INTO sys_permission (id, parent_id, name, type, code, sort, hidden)
SELECT id, parent_id, name, 'BUTTON'::permission_type, code, sort, TRUE
FROM (VALUES
    (10101, 101, '用户查询列表', 'system:user:list', 1),
    (10102, 101, '用户查询详情', 'system:user:query', 2),
    (10103, 101, '用户新增', 'user:add', 3),
    (10104, 101, '用户修改', 'system:user:edit', 4),
    (10105, 101, '用户删除', 'system:user:remove', 5),
    (10106, 101, '用户重置密码', 'system:user:password', 6),
    (10107, 101, '用户分配角色', 'system:user:roles', 7),
    (10108, 101, '用户分配部门', 'system:user:depts', 8),
    (10109, 101, '用户修改状态', 'system:user:status', 9),
    (10201, 102, '角色查询列表', 'system:role:list', 1),
    (10202, 102, '角色查询详情', 'system:role:query', 2),
    (10203, 102, '角色新增', 'system:role:add', 3),
    (10204, 102, '角色修改', 'system:role:edit', 4),
    (10205, 102, '角色删除', 'system:role:remove', 5),
    (10206, 102, '角色修改状态', 'system:role:status', 6),
    (10401, 104, '权限查询列表', 'system:permission:list', 1),
    (10402, 104, '权限查询详情', 'system:permission:query', 2),
    (10403, 104, '权限新增', 'system:permission:add', 3),
    (10404, 104, '权限修改', 'system:permission:edit', 4),
    (10405, 104, '权限删除', 'system:permission:remove', 5),
    (10207, 102, '角色分配权限', 'system:role:permission', 7),
    (10406, 104, '权限清理缓存', 'system:permission:cache', 6),
    (10301, 103, '部门查询列表', 'system:dept:list', 1),
    (10302, 103, '部门查询详情', 'system:dept:query', 2),
    (10303, 103, '部门新增', 'system:dept:add', 3),
    (10304, 103, '部门修改', 'system:dept:edit', 4),
    (10305, 103, '部门删除', 'system:dept:remove', 5),
    (10306, 103, '部门清理缓存', 'system:dept:cache', 6),
    (10501, 105, '字典类型查询列表', 'system:dict-type:list', 1),
    (10502, 105, '字典类型查询详情', 'system:dict-type:query', 2),
    (10503, 105, '字典类型新增', 'system:dict-type:add', 3),
    (10504, 105, '字典类型修改', 'system:dict-type:edit', 4),
    (10505, 105, '字典类型删除', 'system:dict-type:remove', 5),
    (10521, 105, '字典数据查询列表', 'system:dict-data:list', 1),
    (10522, 105, '字典数据查询详情', 'system:dict-data:query', 2),
    (10523, 105, '字典数据新增', 'system:dict-data:add', 3),
    (10524, 105, '字典数据修改', 'system:dict-data:edit', 4),
    (10525, 105, '字典数据删除', 'system:dict-data:remove', 5),
    (10601, 106, '参数新增', 'system:param:add', 1),
    (10602, 106, '参数修改', 'system:param:edit', 2),
    (10603, 106, '参数删除', 'system:param:remove', 3),
    (10604, 106, '参数查询详情', 'system:param:query', 4),
    (10605, 106, '参数查询列表', 'system:param:list', 5),
    (10606, 106, '参数读取明文', 'system:param:value', 6),
    (10801, 100, '文件上传', 'system:file:upload', 1),
    (10802, 100, '文件下载', 'system:file:download', 2),
    (10803, 100, '文件查询详情', 'system:file:query', 3),
    (10804, 100, '文件删除', 'system:file:remove', 4),
    (10805, 100, '文件绑定管理', 'system:file:manage', 5),
    (10901, 100, '操作日志查询列表', 'system:oper-log:list', 1),
    (10902, 100, '操作日志查询详情', 'system:oper-log:query', 2),
    (10903, 100, '操作日志清理', 'system:oper-log:clean', 3),
    (20001, 200, '任务查询列表', 'schedule:job:list', 1),
    (20002, 200, '任务查询详情', 'schedule:job:query', 2),
    (20003, 200, '任务新增', 'schedule:job:add', 3),
    (20004, 200, '任务修改', 'schedule:job:edit', 4),
    (20005, 200, '任务删除', 'schedule:job:remove', 5),
    (20006, 200, '任务修改状态', 'schedule:job:status', 6),
    (20007, 200, '任务立即执行', 'schedule:job:trigger', 7),
    (20101, 200, '任务日志查询列表', 'schedule:job-log:list', 1),
    (20102, 200, '任务日志查询详情', 'schedule:job-log:query', 2),
    (20103, 200, '任务日志清理', 'schedule:job-log:clean', 3)
) AS permissions(id, parent_id, name, code, sort);

-- 默认按应用时区每日凌晨执行；周日附加孤儿扫描。
INSERT INTO sys_job (id, job_name, job_group, bean_name, method_name, cron_expression, enabled, remark)
VALUES (9001, '文件生命周期清理', 'SYSTEM', 'fileCleanupTask', 'execute', '0 0 3 * * *', TRUE,
        '逻辑删除不可恢复；未绑定24小时、零引用3天、孤儿7天');
