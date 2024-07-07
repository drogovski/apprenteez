CREATE TABLE users (
  email text NOT NULL,
  hashedPassword text NOT NULL,
  firstName text,
  lastName text,
  company text,
  role text NOT NULL
);

ALTER TABLE users
ADD CONSTRAINT pk_users PRIMARY KEY (email);

INSERT INTO users (
  email,
  hashedPassword,
  firstName,
  lastName,
  company,
  role
) VALUES (
  'daniel@rockthejvm.com',
  '$2a$10$j6Y44..0CQhQxqiGD6YXEOMARgxkuH1AlC58tDre7cWZGNv43zXl.',
  'Daniel',
  'Ciocirlan',
  'Rock the JVM',
  'ADMIN'
);

INSERT INTO users (
  email,
  hashedPassword,
  firstName,
  lastName,
  company,
  role
) VALUES (
  'riccardo@rockthejvm.com',
  '$2a$10$VaXKC4.KJZAvmn1/KfPe3Oek86.hgKpD/FdDbzZLzlMQBpi62HNfC',
  'Riccardo',
  'Cardin',
  'Rock the JVM',
  'RECRUITER'
);