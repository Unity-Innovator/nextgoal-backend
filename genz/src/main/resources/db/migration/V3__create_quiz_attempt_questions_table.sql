-- Migration V3: Create Learning module tables if missing, including quiz_attempt_questions join table

-- 1. Lessons
CREATE TABLE IF NOT EXISTS lessons (
    lesson_id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    content TEXT,
    difficulty VARCHAR(50) NOT NULL,
    points_reward INT,
    is_active BOOLEAN,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

-- 2. Lesson Questions
CREATE TABLE IF NOT EXISTS lesson_questions (
    question_id UUID PRIMARY KEY,
    lesson_id UUID NOT NULL REFERENCES lessons(lesson_id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    option_a VARCHAR(500) NOT NULL,
    option_b VARCHAR(500) NOT NULL,
    option_c VARCHAR(500) NOT NULL,
    option_d VARCHAR(500) NOT NULL,
    correct_answer VARCHAR(10) NOT NULL,
    order_index INT
);

-- 3. User Lessons
CREATE TABLE IF NOT EXISTS user_lessons (
    user_lesson_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lessons(lesson_id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    score INT,
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE
);

-- 4. Quiz Attempts
CREATE TABLE IF NOT EXISTS quiz_attempts (
    quiz_attempt_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    total_questions INT NOT NULL DEFAULT 5,
    correct_answers INT,
    score DOUBLE PRECISION,
    base_xp INT NOT NULL DEFAULT 0,
    bonus_xp INT NOT NULL DEFAULT 0,
    total_xp INT NOT NULL DEFAULT 0,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    started_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITHOUT TIME ZONE
);

-- 5. User Answers
CREATE TABLE IF NOT EXISTS user_answers (
    user_answer_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    quiz_attempt_id UUID REFERENCES quiz_attempts(quiz_attempt_id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES lesson_questions(question_id) ON DELETE CASCADE,
    selected_answer VARCHAR(50) NOT NULL,
    is_correct BOOLEAN,
    answered_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- 6. Quiz Attempt Questions Join Table
CREATE TABLE IF NOT EXISTS quiz_attempt_questions (
    quiz_attempt_id UUID NOT NULL REFERENCES quiz_attempts(quiz_attempt_id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES lesson_questions(question_id) ON DELETE CASCADE,
    PRIMARY KEY (quiz_attempt_id, question_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS ix_lesson_questions_lesson_id ON lesson_questions(lesson_id);
CREATE INDEX IF NOT EXISTS ix_user_lessons_user_id ON user_lessons(user_id);
CREATE INDEX IF NOT EXISTS ix_quiz_attempts_user_id ON quiz_attempts(user_id);
CREATE INDEX IF NOT EXISTS ix_user_answers_quiz_attempt_id ON user_answers(quiz_attempt_id);
CREATE INDEX IF NOT EXISTS ix_quiz_attempt_questions_question_id ON quiz_attempt_questions(question_id);
