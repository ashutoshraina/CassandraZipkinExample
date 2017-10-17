DROP KEYSPACE IF EXISTS example;

CREATE KEYSPACE example
  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE example;

DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id int,
    email TEXT,
    name TEXT,
    likes TEXT,
    petname TEXT,
    PRIMARY KEY (id, name)
);

CREATE INDEX user_likes
   ON example.users (likes);
