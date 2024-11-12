-- Add up migration script here
CREATE TABLE tokens (
  id INTEGER NOT NULL PRIMARY KEY,
  account TEXT NOT NULL,
  service TEXT,
  secret TEXT NOT NULL,
  algorithm TEXT NOT NULL,
  digits INTEGER NOT NULL,
  period INTEGER NOT NULL
);
