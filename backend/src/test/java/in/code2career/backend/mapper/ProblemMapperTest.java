package in.code2career.backend.mapper;

import in.code2career.backend.dto.ProblemDto;
import in.code2career.backend.dto.ProblemResponseDto;
import in.code2career.backend.entity.Problem;
import in.code2career.backend.entity.Topic;
import in.code2career.backend.entity.User;
import in.code2career.backend.enums.Difficulty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProblemMapperTest {

    @Test
    void mapsProblemRequestAndResponse() {
        ProblemDto request = new ProblemDto();
        request.setTitle("Two Sum");
        request.setDescription("Find two numbers that add to a target.");
        request.setDifficulty(Difficulty.EASY);
        request.setConstraints("2 <= nums.length <= 10^4");
        request.setTopicId(10L);

        Topic topic = Topic.builder().id(10L).build();
        User author = User.builder().id(20L).build();

        Problem problem = ProblemMapper.mapToEntity(request, topic, author);
        problem.setId(30L);

        ProblemResponseDto response = ProblemMapper.mapToResponseDto(problem);

        assertEquals(Difficulty.EASY, problem.getDifficulty());
        assertEquals(30L, response.id());
        assertEquals(10L, response.topicId());
        assertEquals(20L, response.addedByUserId());
    }
}
