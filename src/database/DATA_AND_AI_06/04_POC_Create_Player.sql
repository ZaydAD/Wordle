CREATE TABLE IF NOT EXISTS users (
     user_id INT CONSTRAINT pk_user_id PRIMARY KEY,
     username VARCHAR(255) NOT NULL,
     gender VARCHAR(255) NOT NULL,
     email VARCHAR(255) NOT NULL,
     birthdate DATE NOT NULL
);

