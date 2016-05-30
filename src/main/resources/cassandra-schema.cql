DROP KEYSPACE IF EXISTS cassandra_zipkin_example;

CREATE KEYSPACE cassandra_zipkin_example
  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE cassandra_zipkin_example;

DROP TABLE IF EXISTS users;

CREATE TABLE users (
    email TEXT,
    name TEXT,
    PRIMARY KEY (email)
);
