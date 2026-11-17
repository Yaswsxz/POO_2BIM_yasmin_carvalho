-- SELECTs para os prints do relatório (comprovam a persistência - RN07/RN08).
USE sacm_db;

-- 1) Médicos com status e disponibilidade
SELECT p.id, p.nome, p.cpf, m.crm, m.especialidade, p.status, m.disponibilidade, m.valor_consulta
FROM medico m
JOIN pessoa p ON p.id = m.id
ORDER BY p.id;

-- 2) Pacientes com status e prontuário
SELECT p.id, p.nome, p.cpf, pa.numero_prontuario, pa.convenio, p.status
FROM paciente pa
JOIN pessoa p ON p.id = pa.id
ORDER BY p.id;

-- 3) Consultas com nome do médico e do paciente
SELECT c.id,
       pm.nome  AS medico,
       pp.nome  AS paciente,
       DATE_FORMAT(c.data_hora, '%d/%m/%Y %H:%i') AS data_hora,
       c.motivo,
       c.status,
       c.observacao
FROM consulta c
JOIN pessoa pm ON pm.id = c.medico_id
JOIN pessoa pp ON pp.id = c.paciente_id
ORDER BY c.data_hora, c.id;

-- 4) Prova de que o horário cancelado foi reaproveitado:
--    mesmo médico e mesma data/hora, uma CANCELADA e outra AGENDADA.
SELECT medico_id, data_hora, GROUP_CONCAT(CONCAT(id, ':', status) ORDER BY id) AS consultas
FROM consulta
GROUP BY medico_id, data_hora
HAVING COUNT(*) > 1;
