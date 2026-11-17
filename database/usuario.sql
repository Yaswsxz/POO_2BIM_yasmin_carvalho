-- Cria um usuário local com permissão SÓ no schema sacm_db (passo 5 do enunciado).
-- Execute como root, DEPOIS do schema.sql. Troque a senha antes de rodar
-- e NÃO faça commit da senha real.

CREATE USER IF NOT EXISTS 'sacm_app'@'localhost' IDENTIFIED BY 'sacm2026';
GRANT SELECT, INSERT, UPDATE, DELETE ON sacm_db.* TO 'sacm_app'@'localhost';
FLUSH PRIVILEGES;