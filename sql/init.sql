CREATE DATABASE apprenteez;
\c apprenteez;

CREATE TABLE jobs(
  id uuid DEFAULT gen_random_uuid(),
  date bigint NOT NULL,
  ownerEmail text NOT NULL,
  active BOOLEAN NOT NULL DEFAULT false,
  company text NOT NULL,
  title text NOT NULL,
  description text NOT NULL,
  externalUrl text NOT NULL,
  remote boolean NOT NULL DEFAULT false,
  location text,
  salaryLo integer,
  salaryHi integer,
  currency text,
  country text,
  tags text[],
  image text,
  seniority text,
  other text 
);

ALTER TABLE jobs
ADD CONSTRAINT pk_jobs PRIMARY KEY (id);