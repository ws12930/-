-- ============================================================
-- Fenglin Blog —— 回滚脚本
--
-- 危险：会删除整个 fenglin_blog 数据库，且不可恢复。
-- 执行前必须先备份：
--   mysqldump -u root -p fenglin_blog > fenglin_blog_backup.sql
--   mysql -u root -p < src/main/resources/db/rollback.sql
-- ============================================================

DROP DATABASE IF EXISTS fenglin_blog;
