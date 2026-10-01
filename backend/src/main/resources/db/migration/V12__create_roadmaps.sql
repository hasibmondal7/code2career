CREATE TABLE IF NOT EXISTS roadmaps (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    icon VARCHAR(20) NOT NULL,
    accent VARCHAR(30) NOT NULL,
    display_order INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS roadmap_steps (
    id BIGSERIAL PRIMARY KEY,
    roadmap_id BIGINT NOT NULL REFERENCES roadmaps(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    topic_id BIGINT REFERENCES topics(id),
    problem_id BIGINT REFERENCES problems(id),
    xp_reward INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_roadmap_step_order UNIQUE (roadmap_id, step_order)
);

CREATE INDEX IF NOT EXISTS idx_roadmap_step_problem ON roadmap_steps(problem_id);
CREATE INDEX IF NOT EXISTS idx_roadmap_step_topic ON roadmap_steps(topic_id);

CREATE TABLE IF NOT EXISTS roadmap_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    roadmap_step_id BIGINT NOT NULL REFERENCES roadmap_steps(id) ON DELETE CASCADE,
    completed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_roadmap_progress_user_step UNIQUE (user_id, roadmap_step_id)
);

CREATE INDEX IF NOT EXISTS idx_roadmap_progress_user ON roadmap_progress(user_id);

INSERT INTO topics (name, description)
VALUES
    ('Data Structures & Algorithms', 'Build the problem-solving instincts that power every great engineer.'),
    ('Backend Engineering', 'Design APIs, databases and production-ready services.')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roadmaps (slug, title, description, icon, accent, display_order)
VALUES
    ('data-structures-algorithms', 'Data Structures & Algorithms',
     'Build the problem-solving instincts that power every great engineer.', '⌁', 'purple', 2),
    ('backend-engineering', 'Backend Engineering',
     'Design APIs, databases and production-ready services.', '⌘', 'cyan', 3)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO roadmap_steps (roadmap_id, step_order, title, description, topic_id, xp_reward)
SELECT r.id, steps.step_order, steps.title, steps.description, t.id, steps.xp_reward
FROM roadmaps r
JOIN topics t ON t.name = 'Data Structures & Algorithms'
JOIN (
    VALUES
        (1, 'Arrays & Strings', 'Master traversal, indexing and common string patterns.', 25),
        (2, 'Hash Maps & Sets', 'Use constant-time lookups to simplify problem solving.', 25),
        (3, 'Two Pointers', 'Turn nested scans into focused linear solutions.', 25),
        (4, 'Sliding Window', 'Solve contiguous range problems with moving windows.', 25),
        (5, 'Stacks & Queues', 'Model ordering, history and breadth-first work queues.', 25),
        (6, 'Linked Lists', 'Manipulate pointer-based sequences with confidence.', 25),
        (7, 'Recursion', 'Break problems into smaller, composable subproblems.', 25),
        (8, 'Binary Search', 'Search sorted spaces efficiently and safely.', 25),
        (9, 'Trees', 'Traverse and reason about hierarchical data.', 25),
        (10, 'Binary Search Trees', 'Maintain ordering while navigating tree structures.', 25),
        (11, 'Heaps & Priority Queues', 'Prioritize work and maintain running extremes.', 25),
        (12, 'Graphs', 'Represent relationships and explore connected systems.', 25),
        (13, 'Breadth-First Search', 'Explore graph layers and shortest unweighted paths.', 25),
        (14, 'Depth-First Search', 'Navigate connected structures with recursive exploration.', 25),
        (15, 'Sorting', 'Choose and implement the right ordering strategy.', 25),
        (16, 'Greedy Algorithms', 'Make locally optimal choices with global guarantees.', 25),
        (17, 'Dynamic Programming', 'Reuse overlapping work to solve hard problems.', 25),
        (18, 'Backtracking', 'Explore choices systematically and prune dead ends.', 25),
        (19, 'Interview Patterns', 'Combine your tools into a repeatable problem-solving process.', 25)
) AS steps(step_order, title, description, xp_reward) ON TRUE
WHERE r.slug = 'data-structures-algorithms'
  AND NOT EXISTS (
      SELECT 1 FROM roadmap_steps existing
      WHERE existing.roadmap_id = r.id AND existing.step_order = steps.step_order
  );

INSERT INTO roadmap_steps (roadmap_id, step_order, title, description, topic_id, xp_reward)
SELECT r.id, steps.step_order, steps.title, steps.description, t.id, steps.xp_reward
FROM roadmaps r
JOIN topics t ON t.name = 'Backend Engineering'
JOIN (
    VALUES
        (1, 'HTTP & REST', 'Understand the contracts that connect clients and services.', 30),
        (2, 'Spring Boot Fundamentals', 'Build maintainable applications with Spring Boot.', 30),
        (3, 'API Design', 'Shape predictable, useful and evolvable endpoints.', 30),
        (4, 'Validation & Error Handling', 'Make failures safe, clear and actionable.', 30),
        (5, 'Authentication', 'Protect resources with reliable identity and access controls.', 30),
        (6, 'Relational Databases', 'Model data and write queries that scale with your product.', 30),
        (7, 'JPA & Transactions', 'Persist domain behavior without losing consistency.', 30),
        (8, 'Caching', 'Reduce repeated work while preserving correct responses.', 30),
        (9, 'Async Processing', 'Move slow work off request threads safely.', 30),
        (10, 'Testing Services', 'Verify business behavior with focused automated tests.', 30),
        (11, 'Observability', 'Make production behavior visible through logs and metrics.', 30),
        (12, 'Production Readiness', 'Ship services that are secure, resilient and maintainable.', 30)
) AS steps(step_order, title, description, xp_reward) ON TRUE
WHERE r.slug = 'backend-engineering'
  AND NOT EXISTS (
      SELECT 1 FROM roadmap_steps existing
      WHERE existing.roadmap_id = r.id AND existing.step_order = steps.step_order
  );
