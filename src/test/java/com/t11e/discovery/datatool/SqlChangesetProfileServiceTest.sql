create table profile_table (
  name varchar(20) not null,
  last_run timestamp
);
insert into profile_table (name, last_run) values
  ('test', NULL),
  ('other', NULL);
