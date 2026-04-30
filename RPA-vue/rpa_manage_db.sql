# Host: 127.0.0.1  (Version 5.7.30-log)
# Date: 2026-03-23 11:50:35
# Generator: MySQL-Front 6.1  (Build 1.26)


#
# Structure for table "rpa_process"
#

CREATE TABLE `rpa_process` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `process_code` varchar(50) NOT NULL,
  `process_name` varchar(100) NOT NULL COMMENT 'Process name',
  `description` varchar(255) DEFAULT NULL COMMENT 'Description',
  `step_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'Step count',
  `status` int(11) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rpa_process_code` (`process_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RPA process';

#
# Data for table "rpa_process"
#


#
# Structure for table "rpa_process_step"
#

CREATE TABLE `rpa_process_step` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `process_id` bigint(20) unsigned NOT NULL COMMENT 'Process id',
  `step_no` int(10) unsigned NOT NULL COMMENT 'Step sequence',
  `step_name` varchar(100) NOT NULL,
  `step_type` varchar(50) NOT NULL,
  `script_lang` varchar(30) NOT NULL,
  `script_content` longtext NOT NULL COMMENT 'Script content',
  `config_json` json DEFAULT NULL COMMENT 'Step config',
  `remark` varchar(255) DEFAULT NULL COMMENT 'Remark',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rpa_process_step_no` (`process_id`,`step_no`),
  CONSTRAINT `fk_rpa_process_step_process` FOREIGN KEY (`process_id`) REFERENCES `rpa_process` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RPA process steps';

#
# Data for table "rpa_process_step"
#


#
# Structure for table "rpa_robot"
#

CREATE TABLE `rpa_robot` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `robot_code` varchar(50) NOT NULL,
  `robot_name` varchar(100) NOT NULL,
  `robot_type` varchar(50) NOT NULL,
  `status` int(11) NOT NULL,
  `current_task_code` varchar(64) DEFAULT NULL COMMENT 'Current task code',
  `heartbeat_time` datetime DEFAULT NULL COMMENT 'Heartbeat time',
  `description` varchar(255) DEFAULT NULL COMMENT 'Description',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `current_task_id` bigint(20) DEFAULT NULL,
  `last_heartbeat_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rpa_robot_code` (`robot_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RPA robots';

#
# Data for table "rpa_robot"
#


#
# Structure for table "sys_dict_data"
#

CREATE TABLE `sys_dict_data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `dict_label` varchar(50) NOT NULL,
  `dict_value` varchar(50) NOT NULL,
  `status` int(11) NOT NULL,
  `type_code` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

#
# Data for table "sys_dict_data"
#


#
# Structure for table "sys_dict_type"
#

CREATE TABLE `sys_dict_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `status` int(11) NOT NULL,
  `type_code` varchar(50) NOT NULL,
  `type_name` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKkh5ylbxvgfo0g4j02jyexnlkh` (`type_code`),
  UNIQUE KEY `UKfioreqqfpxxp6d2522ix15ovb` (`type_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

#
# Data for table "sys_dict_type"
#


#
# Structure for table "sys_resource"
#

CREATE TABLE `sys_resource` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `parent_id` bigint(20) unsigned DEFAULT NULL COMMENT 'Parent resource id',
  `resource_name` varchar(100) NOT NULL,
  `resource_code` varchar(50) NOT NULL COMMENT 'Resource code',
  `resource_type` int(11) NOT NULL,
  `path` varchar(120) DEFAULT NULL,
  `component` varchar(120) DEFAULT NULL,
  `permission_key` varchar(100) DEFAULT NULL COMMENT 'Permission key',
  `icon` varchar(64) DEFAULT NULL,
  `sort_no` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'Sort number',
  `status` int(11) NOT NULL,
  `remark` varchar(255) DEFAULT NULL COMMENT 'Remark',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `permission_code` varchar(120) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_resource_code` (`resource_code`),
  UNIQUE KEY `UKfytk18ba739yyvrn0o6wqyk1n` (`permission_code`),
  KEY `idx_sys_resource_parent_id` (`parent_id`),
  KEY `idx_sys_resource_permission_key` (`permission_key`),
  CONSTRAINT `fk_sys_resource_parent` FOREIGN KEY (`parent_id`) REFERENCES `sys_resource` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COMMENT='System resources';

#
# Data for table "sys_resource"
#

INSERT INTO `sys_resource` VALUES (28,NULL,'系统管理','SYSTEM',1,'/system','Layout','system','setting',1,1,'系统管理目录','2026-03-23 09:14:38','2026-03-23 09:14:38',X'323032362D30332D32332030393A31343A33382E333135303739',X'323032362D30332D32332030393A31343A33382E333135303739',NULL),(29,NULL,'仪表盘','DASHBOARD',1,'/dashboard','Layout','dashboard','dashboard',2,1,'仪表盘目录','2026-03-23 09:14:38','2026-03-23 09:14:38',X'323032362D30332D32332030393A31343A33382E333135303739',X'323032362D30332D32332030393A31343A33382E333135303739',NULL),(30,28,'用户管理','USER_MANAGE',2,'/system/users','system/users/index','system:user:view','user',1,1,'用户管理菜单','2026-03-23 09:14:38','2026-03-23 09:14:38',X'323032362D30332D32332030393A31343A33382E333135383535',X'323032362D30332D32332030393A31343A33382E333135383535','system:user:view'),(31,28,'角色管理','ROLE_MANAGE',2,'/system/roles','system/roles/index','system:role:view','peoples',2,1,'角色管理菜单','2026-03-23 09:14:38','2026-03-23 09:14:38',X'323032362D30332D32332030393A31343A33382E333138313335',X'323032362D30332D32332030393A31343A33382E333138313335','system:role:view'),(32,29,'工作台','WORKBENCH',2,'/dashboard/workbench','dashboard/index','dashboard:view','dashboard',1,1,'工作台菜单','2026-03-23 09:14:38','2026-03-23 09:14:38',X'323032362D30332D32332030393A31343A33382E333138393734',X'323032362D30332D32332030393A31343A33382E333138393734','dashboard:view'),(33,30,'用户分页','USER_PAGE',3,NULL,NULL,'system:user:page',NULL,1,1,'用户分页按钮','2026-03-23 09:14:38','2026-03-23 09:14:38',X'323032362D30332D32332030393A31343A33382E333139383137',X'323032362D30332D32332030393A31343A33382E333139383137','system:user:page'),(34,30,'新增用户','USER_CREATE',3,NULL,NULL,'system:user:create',NULL,2,1,'新增用户按钮','2026-03-23 09:14:38','2026-03-23 09:14:38',X'323032362D30332D32332030393A31343A33382E333230363237',X'323032362D30332D32332030393A31343A33382E333230363237','system:user:create'),(35,31,'角色分页','ROLE_PAGE',3,NULL,NULL,'system:role:page',NULL,1,1,'角色分页按钮','2026-03-23 09:14:38','2026-03-23 09:14:38',X'323032362D30332D32332030393A31343A33382E333231343235',X'323032362D30332D32332030393A31343A33382E333231343235','system:role:page'),(36,31,'新增角色','ROLE_CREATE',3,NULL,NULL,'system:role:create',NULL,2,1,'新增角色按钮','2026-03-23 09:14:38','2026-03-23 09:14:38',X'323032362D30332D32332030393A31343A33382E333232333433',X'323032362D30332D32332030393A31343A33382E333232333433','system:role:create');

#
# Structure for table "sys_role"
#

CREATE TABLE `sys_role` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `role_code` varchar(50) NOT NULL,
  `role_name` varchar(50) NOT NULL COMMENT 'Role name',
  `description` varchar(255) DEFAULT NULL COMMENT 'Role description',
  `status` int(11) NOT NULL,
  `remark` varchar(255) DEFAULT NULL COMMENT 'Remark',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`role_code`),
  UNIQUE KEY `uk_sys_role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COMMENT='System roles';

#
# Data for table "sys_role"
#

INSERT INTO `sys_role` VALUES (14,'ADMIN','管理员','系统管理员',1,'初始化管理员角色','2026-03-23 09:14:38','2026-03-23 09:14:38',X'323032362D30332D32332030393A31343A33382E333133373132',X'323032362D30332D32332030393A31343A33382E333133373132'),(15,'OPERATOR','操作员','普通操作员',1,'初始化操作员角色','2026-03-23 09:14:38','2026-03-23 09:14:38',X'323032362D30332D32332030393A31343A33382E333133373132',X'323032362D30332D32332030393A31343A33382E333133373132'),(16,'OPS_TEST','运维测试角色','给前端联调使用',1,'test-ui','2026-03-23 11:32:59','2026-03-23 11:32:59',X'323032362D30332D32332031313A33323A35392E373235323832',X'323032362D30332D32332031313A33323A35392E373235323832');

#
# Structure for table "sys_role_resource"
#

CREATE TABLE `sys_role_resource` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `role_id` bigint(20) unsigned NOT NULL COMMENT 'Role id',
  `resource_id` bigint(20) unsigned NOT NULL COMMENT 'Resource id',
  `grant_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Grant time',
  `remark` varchar(255) DEFAULT NULL COMMENT 'Remark',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_resource` (`role_id`,`resource_id`),
  KEY `idx_sys_role_resource_resource_id` (`resource_id`),
  CONSTRAINT `fk_sys_role_resource_resource` FOREIGN KEY (`resource_id`) REFERENCES `sys_resource` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_sys_role_resource_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COMMENT='Role-resource mapping';

#
# Data for table "sys_role_resource"
#

INSERT INTO `sys_role_resource` VALUES (23,14,28,'2026-03-23 09:14:38','管理员默认拥有全部资源',X'323032362D30332D32332030393A31343A33382E333234393239',X'323032362D30332D32332030393A31343A33382E333234393239'),(24,14,29,'2026-03-23 09:14:38','管理员默认拥有全部资源',X'323032362D30332D32332030393A31343A33382E333234393239',X'323032362D30332D32332030393A31343A33382E333234393239'),(25,14,30,'2026-03-23 09:14:38','管理员默认拥有全部资源',X'323032362D30332D32332030393A31343A33382E333234393239',X'323032362D30332D32332030393A31343A33382E333234393239'),(26,14,31,'2026-03-23 09:14:38','管理员默认拥有全部资源',X'323032362D30332D32332030393A31343A33382E333234393239',X'323032362D30332D32332030393A31343A33382E333234393239'),(27,14,32,'2026-03-23 09:14:38','管理员默认拥有全部资源',X'323032362D30332D32332030393A31343A33382E333234393239',X'323032362D30332D32332030393A31343A33382E333234393239'),(28,14,33,'2026-03-23 09:14:38','管理员默认拥有全部资源',X'323032362D30332D32332030393A31343A33382E333234393239',X'323032362D30332D32332030393A31343A33382E333234393239'),(29,14,34,'2026-03-23 09:14:38','管理员默认拥有全部资源',X'323032362D30332D32332030393A31343A33382E333234393239',X'323032362D30332D32332030393A31343A33382E333234393239'),(30,14,35,'2026-03-23 09:14:38','管理员默认拥有全部资源',X'323032362D30332D32332030393A31343A33382E333234393239',X'323032362D30332D32332030393A31343A33382E333234393239'),(31,14,36,'2026-03-23 09:14:38','管理员默认拥有全部资源',X'323032362D30332D32332030393A31343A33382E333234393239',X'323032362D30332D32332030393A31343A33382E333234393239'),(38,15,29,'2026-03-23 09:14:38','操作员默认查看权限',X'323032362D30332D32332030393A31343A33382E333237353635',X'323032362D30332D32332030393A31343A33382E333237353635'),(39,15,31,'2026-03-23 09:14:38','操作员默认查看权限',X'323032362D30332D32332030393A31343A33382E333237353635',X'323032362D30332D32332030393A31343A33382E333237353635'),(40,15,35,'2026-03-23 09:14:38','操作员默认查看权限',X'323032362D30332D32332030393A31343A33382E333237353635',X'323032362D30332D32332030393A31343A33382E333237353635'),(41,15,28,'2026-03-23 09:14:38','操作员默认查看权限',X'323032362D30332D32332030393A31343A33382E333237353635',X'323032362D30332D32332030393A31343A33382E333237353635'),(42,15,30,'2026-03-23 09:14:38','操作员默认查看权限',X'323032362D30332D32332030393A31343A33382E333237353635',X'323032362D30332D32332030393A31343A33382E333237353635'),(43,15,33,'2026-03-23 09:14:38','操作员默认查看权限',X'323032362D30332D32332030393A31343A33382E333237353635',X'323032362D30332D32332030393A31343A33382E333237353635'),(44,15,32,'2026-03-23 09:14:38','操作员默认查看权限',X'323032362D30332D32332030393A31343A33382E333237353635',X'323032362D30332D32332030393A31343A33382E333237353635');

#
# Structure for table "sys_user"
#

CREATE TABLE `sys_user` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `username` varchar(20) NOT NULL,
  `password_hash` varchar(255) NOT NULL COMMENT 'Password hash',
  `real_name` varchar(20) NOT NULL,
  `email` varchar(64) DEFAULT NULL,
  `mobile` varchar(11) NOT NULL,
  `avatar_url` varchar(255) DEFAULT NULL COMMENT 'Avatar URL',
  `status` int(11) NOT NULL,
  `last_login_time` datetime DEFAULT NULL COMMENT 'Last login time',
  `remark` varchar(255) DEFAULT NULL COMMENT 'Remark',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `password` varchar(100) NOT NULL,
  `role_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  UNIQUE KEY `uk_sys_user_mobile` (`mobile`),
  UNIQUE KEY `uk_sys_user_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='System users';

#
# Data for table "sys_user"
#

INSERT INTO `sys_user` VALUES (3,'admin','$2a$10$rCkiT4rctgDvpkNuFFAgWO14Tx4qAXXAocipgP/2BcuWKnkiGp1ve','雪','3111471949@qq.com','13617618489',NULL,1,'2026-03-23 11:43:08','初始化管理员账号','2026-03-23 09:14:38','2026-03-23 11:43:08',X'323032362D30332D32332030393A31343A33382E333233323032',X'323032362D30332D32332031313A34333A30382E333436393639','$2a$10$rCkiT4rctgDvpkNuFFAgWO14Tx4qAXXAocipgP/2BcuWKnkiGp1ve',14),(4,'operator01','$2a$10$xeLcxIq8UWmTb5gFbwfY5ejhknfPDQismKsrra9uzPKYsMdjF4A8y','操作员甲','operator01@rpa.com','13800138001',NULL,1,NULL,'初始化操作员账号','2026-03-23 09:14:38','2026-03-23 09:14:38',X'323032362D30332D32332030393A31343A33382E333234313134',X'323032362D30332D32332030393A31343A33382E333234313134','$2a$10$xeLcxIq8UWmTb5gFbwfY5ejhknfPDQismKsrra9uzPKYsMdjF4A8y',15),(5,'tester02','$2a$10$BFhxasZDt09cDCZ.HPS0TebDR3KXPaxMZIePoRWQCksvhQQBhzRe6','测试用户2','tester02@rpa.com','13800138002',NULL,0,NULL,NULL,'2026-03-23 11:34:49','2026-03-23 11:45:20',X'323032362D30332D32332031313A33343A34392E373330303239',X'323032362D30332D32332031313A34353A32302E383937373930','$2a$10$BFhxasZDt09cDCZ.HPS0TebDR3KXPaxMZIePoRWQCksvhQQBhzRe6',14);

#
# Structure for table "rpa_task"
#

CREATE TABLE `rpa_task` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `task_code` varchar(50) NOT NULL,
  `task_name` varchar(100) NOT NULL COMMENT 'Task name',
  `taxpayer_id_no` varchar(30) NOT NULL,
  `enterprise_name` varchar(120) NOT NULL,
  `process_id` bigint(20) unsigned NOT NULL COMMENT 'Bound process id',
  `robot_id` bigint(20) unsigned NOT NULL COMMENT 'Bound robot id',
  `status` int(11) NOT NULL,
  `start_time` datetime DEFAULT NULL COMMENT 'Last start time',
  `end_time` datetime DEFAULT NULL COMMENT 'Last end time',
  `creator_user_id` bigint(20) unsigned DEFAULT NULL COMMENT 'Creator user id',
  `remark` varchar(255) DEFAULT NULL COMMENT 'Remark',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rpa_task_code` (`task_code`),
  KEY `idx_rpa_task_taxpayer_id_no` (`taxpayer_id_no`),
  KEY `idx_rpa_task_enterprise_name` (`enterprise_name`),
  KEY `idx_rpa_task_process_id` (`process_id`),
  KEY `idx_rpa_task_robot_id` (`robot_id`),
  KEY `idx_rpa_task_creator_user_id` (`creator_user_id`),
  CONSTRAINT `fk_rpa_task_creator` FOREIGN KEY (`creator_user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_rpa_task_process` FOREIGN KEY (`process_id`) REFERENCES `rpa_process` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_rpa_task_robot` FOREIGN KEY (`robot_id`) REFERENCES `rpa_robot` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RPA tasks';

#
# Data for table "rpa_task"
#


#
# Structure for table "rpa_execution_record"
#

CREATE TABLE `rpa_execution_record` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `execution_code` varchar(50) NOT NULL,
  `task_id` bigint(20) unsigned NOT NULL COMMENT 'Task id',
  `process_id` bigint(20) unsigned NOT NULL COMMENT 'Process id',
  `robot_id` bigint(20) unsigned NOT NULL COMMENT 'Robot id',
  `execute_status` int(11) NOT NULL,
  `start_time` datetime NOT NULL COMMENT 'Start time',
  `end_time` datetime DEFAULT NULL COMMENT 'End time',
  `duration_seconds` int(10) unsigned DEFAULT NULL COMMENT 'Duration in seconds',
  `error_message` varchar(500) DEFAULT NULL COMMENT 'Error summary',
  `log_content` longtext COMMENT 'Execution log',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `execution_log` text,
  `remark` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rpa_execution_record_code` (`execution_code`),
  KEY `idx_rpa_execution_task_id` (`task_id`),
  KEY `idx_rpa_execution_process_id` (`process_id`),
  KEY `idx_rpa_execution_robot_id` (`robot_id`),
  CONSTRAINT `fk_rpa_execution_process` FOREIGN KEY (`process_id`) REFERENCES `rpa_process` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_rpa_execution_robot` FOREIGN KEY (`robot_id`) REFERENCES `rpa_robot` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_rpa_execution_task` FOREIGN KEY (`task_id`) REFERENCES `rpa_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RPA execution records';

#
# Data for table "rpa_execution_record"
#


#
# Structure for table "data_collection"
#

CREATE TABLE `data_collection` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `task_id` bigint(20) unsigned NOT NULL COMMENT 'Task id',
  `execution_id` bigint(20) unsigned NOT NULL COMMENT 'Execution record id',
  `taxpayer_id_no` varchar(30) NOT NULL,
  `enterprise_name` varchar(120) NOT NULL,
  `source_name` varchar(100) NOT NULL,
  `status` int(11) NOT NULL,
  `raw_data` json NOT NULL COMMENT 'Raw data',
  `error_message` varchar(500) DEFAULT NULL COMMENT 'Error message',
  `collect_time` datetime NOT NULL COMMENT 'Collect time',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `collection_time` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_collection_execution` (`execution_id`),
  KEY `idx_data_collection_task_id` (`task_id`),
  KEY `idx_data_collection_taxpayer_id_no` (`taxpayer_id_no`),
  KEY `idx_data_collection_enterprise_name` (`enterprise_name`),
  CONSTRAINT `fk_data_collection_execution` FOREIGN KEY (`execution_id`) REFERENCES `rpa_execution_record` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_data_collection_task` FOREIGN KEY (`task_id`) REFERENCES `rpa_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Collected raw data';

#
# Data for table "data_collection"
#


#
# Structure for table "data_analysis"
#

CREATE TABLE `data_analysis` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `task_id` bigint(20) unsigned NOT NULL COMMENT 'Task id',
  `execution_id` bigint(20) unsigned NOT NULL COMMENT 'Execution record id',
  `collection_id` bigint(20) unsigned NOT NULL COMMENT 'Collection record id',
  `taxpayer_id_no` varchar(32) NOT NULL COMMENT 'Taxpayer id',
  `enterprise_name` varchar(100) NOT NULL COMMENT 'Enterprise name',
  `status` int(11) NOT NULL,
  `extracted_field_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'Field count',
  `rule_name` varchar(100) DEFAULT NULL COMMENT 'Rule name',
  `parsed_data` json NOT NULL COMMENT 'Parsed data',
  `error_message` varchar(500) DEFAULT NULL COMMENT 'Error message',
  `analysis_time` datetime NOT NULL COMMENT 'Analysis time',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_analysis_collection` (`collection_id`),
  KEY `idx_data_analysis_task_id` (`task_id`),
  KEY `idx_data_analysis_execution_id` (`execution_id`),
  KEY `idx_data_analysis_taxpayer_id_no` (`taxpayer_id_no`),
  KEY `idx_data_analysis_enterprise_name` (`enterprise_name`),
  CONSTRAINT `fk_data_analysis_collection` FOREIGN KEY (`collection_id`) REFERENCES `data_collection` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_data_analysis_execution` FOREIGN KEY (`execution_id`) REFERENCES `rpa_execution_record` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_data_analysis_task` FOREIGN KEY (`task_id`) REFERENCES `rpa_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Parsed business data';

#
# Data for table "data_analysis"
#


#
# Structure for table "data_processing"
#

CREATE TABLE `data_processing` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `task_id` bigint(20) unsigned NOT NULL COMMENT 'Task id',
  `execution_id` bigint(20) unsigned NOT NULL COMMENT 'Execution record id',
  `analysis_id` bigint(20) unsigned NOT NULL COMMENT 'Analysis record id',
  `taxpayer_id_no` varchar(32) NOT NULL COMMENT 'Taxpayer id',
  `enterprise_name` varchar(100) NOT NULL COMMENT 'Enterprise name',
  `status` int(11) NOT NULL,
  `validation_result` longtext,
  `processed_data` json NOT NULL COMMENT 'Processed data',
  `validation_detail` json DEFAULT NULL COMMENT 'Validation detail',
  `error_message` varchar(500) DEFAULT NULL COMMENT 'Error message',
  `process_time` datetime NOT NULL COMMENT 'Process time',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `processing_time` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_processing_analysis` (`analysis_id`),
  KEY `idx_data_processing_task_id` (`task_id`),
  KEY `idx_data_processing_execution_id` (`execution_id`),
  KEY `idx_data_processing_taxpayer_id_no` (`taxpayer_id_no`),
  KEY `idx_data_processing_enterprise_name` (`enterprise_name`),
  CONSTRAINT `fk_data_processing_analysis` FOREIGN KEY (`analysis_id`) REFERENCES `data_analysis` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_data_processing_execution` FOREIGN KEY (`execution_id`) REFERENCES `rpa_execution_record` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_data_processing_task` FOREIGN KEY (`task_id`) REFERENCES `rpa_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Processed business data';

#
# Data for table "data_processing"
#


#
# Structure for table "data_business_final"
#

CREATE TABLE `data_business_final` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `task_id` bigint(20) unsigned NOT NULL COMMENT 'Task id',
  `execution_id` bigint(20) unsigned NOT NULL COMMENT 'Execution record id',
  `processing_id` bigint(20) unsigned NOT NULL COMMENT 'Processing record id',
  `taxpayer_id_no` varchar(30) NOT NULL,
  `enterprise_name` varchar(120) NOT NULL,
  `tax_area_id` varchar(30) DEFAULT NULL,
  `indicator_code` varchar(64) DEFAULT NULL COMMENT 'Indicator code',
  `data_status` int(11) NOT NULL,
  `business_data` json NOT NULL COMMENT 'Final business data',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `tax_area_name` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_business_final_processing` (`processing_id`),
  KEY `idx_data_business_final_task_id` (`task_id`),
  KEY `idx_data_business_final_execution_id` (`execution_id`),
  KEY `idx_data_business_final_taxpayer_id_no` (`taxpayer_id_no`),
  KEY `idx_data_business_final_enterprise_name` (`enterprise_name`),
  KEY `idx_data_business_final_tax_area_id` (`tax_area_id`),
  KEY `idx_data_business_final_indicator_code` (`indicator_code`),
  CONSTRAINT `fk_data_business_final_execution` FOREIGN KEY (`execution_id`) REFERENCES `rpa_execution_record` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_data_business_final_processing` FOREIGN KEY (`processing_id`) REFERENCES `data_processing` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_data_business_final_task` FOREIGN KEY (`task_id`) REFERENCES `rpa_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Final business query data';

#
# Data for table "data_business_final"
#


#
# Structure for table "sys_user_role"
#

CREATE TABLE `sys_user_role` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID，关联 sys_user(id)',
  `role_id` bigint(20) unsigned NOT NULL COMMENT '角色ID，关联 sys_role(id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

#
# Data for table "sys_user_role"
#

INSERT INTO `sys_user_role` VALUES (1,3,14,'2026-03-23 09:14:38'),(2,4,15,'2026-03-23 09:14:38'),(4,5,14,'2026-03-23 11:34:49');
