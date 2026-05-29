package ru.fa.miniregister;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тест REST-контроллера через MockMvc без запуска реального HTTP-сервера.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecordRestControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void createThenList() throws Exception {
        String json = """
                {"title":"Бронь B-201","description":"Переговорная на 2 этаже","status":"NEW"}
                """;

        mvc.perform(post("/api/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/records/")));

        mvc.perform(get("/api/records"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Бронь B-201")));
    }

    @Test
    void getNotFound() throws Exception {
        mvc.perform(get("/api/records/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithBlankTitleReturns400() throws Exception {
        String json = """
                {"title":"","description":"D","status":"NEW"}
                """;

        mvc.perform(post("/api/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
