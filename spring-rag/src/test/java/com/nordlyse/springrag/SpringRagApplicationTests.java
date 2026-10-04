package com.nordlyse.springrag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
        "spring-rag.directory-listener=false"
})
class SpringRagApplicationTests {

    @MockitoBean
    @SuppressWarnings("unused")
    private VectorStore vectorStore;

    @Test
    void contextLoads() {
    }
}
