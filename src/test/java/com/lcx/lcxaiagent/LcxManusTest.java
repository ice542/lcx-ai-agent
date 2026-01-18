package com.lcx.lcxaiagent;

import com.lcx.lcxaiagent.agent.LcxManus;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LcxManusTest {
  
    @Resource
    private LcxManus lcxManus;
  
    @Test
    void run() {  
        String userPrompt = """  
                我的另一半住在潮汕，潮汕这个地区的女生怎么样""";
        String answer = lcxManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }  
}
