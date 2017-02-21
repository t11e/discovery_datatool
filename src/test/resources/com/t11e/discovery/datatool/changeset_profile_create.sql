create table changeset_profile (
  name varchar(255) not null unique,
  last_run timestamp
);
