CREATE TABLE tasks (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT       NOT NULL,
    assignee_id  BIGINT,
    title        VARCHAR(255) NOT NULL,
    description  VARCHAR(4000),
    status       VARCHAR(20)  NOT NULL,
    priority     VARCHAR(20)  NOT NULL,
    deadline     TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_project  FOREIGN KEY (project_id)  REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES users (id)
);

CREATE INDEX idx_tasks_project         ON tasks (project_id);
CREATE INDEX idx_tasks_assignee        ON tasks (assignee_id);
CREATE INDEX idx_tasks_status          ON tasks (status);
CREATE INDEX idx_tasks_priority        ON tasks (priority);
CREATE INDEX idx_tasks_assignee_status ON tasks (assignee_id, status);
CREATE INDEX idx_tasks_title           ON tasks (title);
