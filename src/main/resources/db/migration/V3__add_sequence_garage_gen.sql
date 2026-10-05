ALTER TABLE garage_slots ALTER COLUMN id DROP IDENTITY;
CREATE SEQUENCE garage_slots_id_seq START WITH 1 INCREMENT BY 50;
ALTER SEQUENCE garage_slots_id_seq OWNED BY garage_slots.id;
ALTER TABLE garage_slots ALTER COLUMN id SET DEFAULT nextval('garage_slots_id_seq');
SELECT setval('garage_slots_id_seq', COALESCE(MAX(id), 1)) FROM garage_slots;