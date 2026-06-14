-- V5: Allow title to be nullable (title is auto-generated on FE; duplicates are permitted)
ALTER TABLE exercises ALTER COLUMN title DROP NOT NULL;
