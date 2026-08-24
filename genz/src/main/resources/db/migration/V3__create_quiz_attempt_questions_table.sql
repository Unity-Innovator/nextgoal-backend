-- Create join table for QuizAttempt and LessonQuestion
CREATE TABLE IF NOT EXISTS quiz_attempt_questions (
    quiz_attempt_id UUID NOT NULL REFERENCES quiz_attempts(quiz_attempt_id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES lesson_questions(question_id) ON DELETE CASCADE,
    PRIMARY KEY (quiz_attempt_id, question_id)
);

CREATE INDEX IF NOT EXISTS ix_quiz_attempt_questions_question_id
    ON quiz_attempt_questions (question_id);
