DO $$
DECLARE
    topic_arrays_id BIGINT;
    topic_math_id BIGINT;
    problem_twosum_id BIGINT;
    problem_palindrome_id BIGINT;
BEGIN
    -- 1. Insert Topics
    INSERT INTO topics (name, description) 
    VALUES ('Arrays', 'Problems related to array manipulation')
    ON CONFLICT (name) DO NOTHING;
    
    SELECT id INTO topic_arrays_id FROM topics WHERE name = 'Arrays';

    INSERT INTO topics (name, description) 
    VALUES ('Math', 'Mathematical operations and logic')
    ON CONFLICT (name) DO NOTHING;
    
    SELECT id INTO topic_math_id FROM topics WHERE name = 'Math';

    -- 2. Insert Problem: Two Sum
    INSERT INTO problems (title, description, difficulty, constraints, xp_reward, topic_id)
    VALUES (
        'Two Sum', 
        'Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.\n\nYou may assume that each input would have exactly one solution, and you may not use the same element twice.\n\nYou can return the answer in any order.',
        'EASY',
        '- 2 <= nums.length <= 10^4\n- -10^9 <= nums[i] <= 10^9\n- -10^9 <= target <= 10^9\n- Only one valid answer exists.',
        10,
        topic_arrays_id
    ) RETURNING id INTO problem_twosum_id;

    -- 3. Insert Problem Templates for Two Sum
    INSERT INTO problem_templates (problem_id, language, code_template) VALUES
    (problem_twosum_id, 'JAVA', 'import java.util.*;

class Solution {
    public int[] twoSum(int[] nums, int target) {
        // write your code here
        
    }
}'),
    (problem_twosum_id, 'PYTHON', 'class Solution:
    def twoSum(self, nums: List[int], target: int) -> List[int]:
        pass');

    -- 4. Insert Test Cases for Two Sum
    INSERT INTO test_cases (problem_id, input_data, expected_output, is_hidden) VALUES
    (problem_twosum_id, '[2,7,11,15]\n9', '[0,1]', false),
    (problem_twosum_id, '[3,2,4]\n6', '[1,2]', false),
    (problem_twosum_id, '[3,3]\n6', '[0,1]', false),
    (problem_twosum_id, '[2,5,5,11]\n10', '[1,2]', true);


    -- 5. Insert Problem: Palindrome Number
    INSERT INTO problems (title, description, difficulty, constraints, xp_reward, topic_id)
    VALUES (
        'Palindrome Number', 
        'Given an integer x, return true if x is a palindrome, and false otherwise.',
        'EASY',
        '- -2^31 <= x <= 2^31 - 1',
        10,
        topic_math_id
    ) RETURNING id INTO problem_palindrome_id;

    -- 6. Insert Problem Templates for Palindrome Number
    INSERT INTO problem_templates (problem_id, language, code_template) VALUES
    (problem_palindrome_id, 'JAVA', 'class Solution {
    public boolean isPalindrome(int x) {
        // write your code here
        return false;
    }
}'),
    (problem_palindrome_id, 'PYTHON', 'class Solution:
    def isPalindrome(self, x: int) -> bool:
        pass');

    -- 7. Insert Test Cases for Palindrome Number
    INSERT INTO test_cases (problem_id, input_data, expected_output, is_hidden) VALUES
    (problem_palindrome_id, '121', 'true', false),
    (problem_palindrome_id, '-121', 'false', false),
    (problem_palindrome_id, '10', 'false', false),
    (problem_palindrome_id, '1221', 'true', true);

END $$;
