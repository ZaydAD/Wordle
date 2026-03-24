

INSERT INTO users (user_id, username, gender, email, birthdate)
VALUES
    (1, 'test', 'M', 'test@gmail.com', '1990-01-01'),
    (2, 'john_doe', 'M', 'john.doe@example.com', '1992-02-02'),
    (3, 'jane_doe', 'F', 'jane.doe@example.com', '1993-03-03'),
    (4, 'player_one', 'M', 'player1@example.com', '1994-04-04'),
    (5, 'gamer_girl', 'F', 'gamer.girl@example.com', '1995-05-05'),
    (6, 'speedster', 'M', 'speedster@example.com', '1996-06-06'),
    (7, 'sharp_shooter', 'F', 'sharpshooter@example.com', '1997-07-07'),
    (8, 'stealth_fox', 'M', 'stealth.fox@example.com', '1998-08-08'),
    (9, 'agent99', 'F', 'agent99@example.com', '1999-09-09'),
    (10, 'dragon_slayer', 'M', 'dragonslayer@example.com', '2000-10-10'),
    (11, 'knight_rider', 'M', 'knightrider@example.com', '1989-11-11'),
    (12, 'cyber_wolf', 'F', 'cyberwolf@example.com', '1991-12-12'),
    (13, 'alien_hunter', 'M', 'alienhunter@example.com', '1988-01-13'),
    (14, 'phantom_ace', 'F', 'phantomace@example.com', '1987-02-14'),
    (15, 'master_mind', 'M', 'mastermind@example.com', '1986-03-15'),
    (16, 'shadow_blade', 'M', 'shadowblade@example.com', '1985-04-16');

INSERT INTO words (word_id, word_to_guess)
VALUES
    (101, 'grape'),
    (102, 'peach'),
    (103, 'mango'),
    (104, 'berry'),
    (105, 'lemon'),
    (106, 'plums'),
    (107, 'charm'),
    (108, 'dream'),
    (109, 'flame'),
    (110, 'glory'),
    (111, 'trace'),
    (112, 'bliss'),
    (113, 'spark'),
    (114, 'shine'),
    (115, 'cloud'),
    (116, 'stone');

INSERT INTO game_sessions (game_session_id, user_id, duration, time_stamp, word_id, attempts_num, score)
VALUES
    (1, 1, 120, '2024-12-01 10:00:00', 101, 3, 50),
    (2, 2, 180, '2024-12-01 11:00:00', 102, 2, 80),
    (3, 3, 90, '2024-12-01 12:00:00', 103, 1, 100),
    (4, 4, 200, '2024-12-02 09:30:00', 104, 4, 30),
    (5, 5, 150, '2024-12-02 10:15:00', 105, 3, 60),
    (6, 6, 300, '2024-12-02 11:45:00', 106, 5, 20),
    (7, 7, 220, '2024-12-03 14:00:00', 107, 3, 70),
    (8, 8, 180, '2024-12-03 15:30:00', 108, 2, 90),
    (9, 9, 240, '2024-12-03 16:00:00', 109, 3, 80),
    (10, 10, 60, '2024-12-04 08:00:00', 110, 1, 100),
    (11, 11, 180, '2024-12-04 09:00:00', 111, 2, 85),
    (12, 12, 120, '2024-12-04 10:30:00', 112, 3, 75),
    (13, 13, 200, '2024-12-05 13:00:00', 113, 4, 50),
    (14, 14, 150, '2024-12-05 14:30:00', 114, 2, 95),
    (15, 15, 90, '2024-12-05 15:45:00', 115, 1, 100),
    (16, 16, 300, '2024-12-06 17:00:00', 116, 5, 20);