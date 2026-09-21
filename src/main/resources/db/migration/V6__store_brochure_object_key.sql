ALTER TABLE courses RENAME COLUMN brochure_url TO brochure_object_key;

UPDATE courses SET brochure_object_key = NULL WHERE brochure_object_key LIKE 'http%';