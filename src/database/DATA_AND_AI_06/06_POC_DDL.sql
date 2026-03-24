--This Script must be run in the respective ORDER
-- USERS, WORDS, GAME_SESSIONS, CELL

CREATE TABLE IF NOT EXISTS users (
     user_id INT CONSTRAINT pk_user_id PRIMARY KEY,
     username VARCHAR(255) NOT NULL,
     gender VARCHAR(255) NOT NULL,
     email VARCHAR(255) NOT NULL,
     birthdate DATE NOT NULL
);



CREATE TABLE IF NOT EXISTS words (
     word_id INT CONSTRAINT pk_word_id PRIMARY KEY,
     word_to_guess VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS game_sessions (
     game_session_id INT,
     user_id INT,
     duration INT NOT NULL,
     time_stamp TIMESTAMP NOT NULL,
     word_id INT CONSTRAINT fk_word_id REFERENCES words(word_id),
     attempts_num INT,
     score INT,
     constraint pk_game_session_id PRIMARY KEY (game_session_id, user_id),
     constraint fk_user_id FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS cell (
    cell_id SERIAL PRIMARY KEY,
    game_session_id INT NOT NULL,
    user_id INT NOT NULL,
    position INT NOT NULL,
    character VARCHAR(1) NOT NULL,
    status VARCHAR(20),
    CONSTRAINT fk_game_session FOREIGN KEY (game_session_id, user_id) REFERENCES game_sessions(game_session_id, user_id)
);