CREATE TABLE problem_templates (
    problem_id BIGINT NOT NULL,
    code_template TEXT,
    language VARCHAR(255) NOT NULL,
    PRIMARY KEY (problem_id, language),
    CONSTRAINT fk_problem_templates_problem_id FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE
);
