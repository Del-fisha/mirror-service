MERGE INTO backup_config (id, backup_path, db_host, db_port, db_name, db_user, db_password, backup_interval_minutes, backup_enabled) KEY (id)
VALUES (1, './backups', 'localhost', 5432, 'postgres', 'postgres', '', 5, false);
