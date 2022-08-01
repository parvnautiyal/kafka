package com.library.producer.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.producer.controller.LibraryController;
import com.library.producer.domain.LibraryEvent;
import com.library.producer.entity.Book;
import com.library.producer.producer.LibraryEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LibraryController.class)
@AutoConfigureMockMvc
class LibraryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    LibraryEventProducer libraryEventProducer;

    @Test
    void postLibraryEventTest() throws Exception {

        Book book = Book.builder()
                .id("Book1")
                .name("Harry Potter")
                .author("J.K Rowling")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .id(null)
                .book(book)
                .build();

        when(libraryEventProducer.sendLibraryEventApproachTwo(isA(LibraryEvent.class))).thenReturn(null);

        mockMvc.perform(post("/api/v1.0/libraryEvent")
                .content(objectMapper.writeValueAsString(libraryEvent))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void postLibraryEvent4xxTest() throws Exception {

        Book book = Book.builder()
                .id(null)
                .name(null)
                .author(null)
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .id(null)
                .book(book)
                .build();

        when(libraryEventProducer.sendLibraryEventApproachTwo(isA(LibraryEvent.class))).thenReturn(null);

        String expectedErrorMessge = "book.author-must not be blank,book.id-must not be null,book.name-must not be blank";

        mockMvc.perform(post("/api/v1.0/libraryEvent")
                        .content(objectMapper.writeValueAsString(libraryEvent))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(expectedErrorMessge));
    }

    @Test
    void putLibraryEventTest() throws Exception {

        Book book = Book.builder()
                .id("Book-1")
                .name("Harry Potter")
                .author("J.K Rowling")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .id(123)
                .book(book)
                .build();

        when(libraryEventProducer.sendLibraryEventApproachTwo(isA(LibraryEvent.class))).thenReturn(null);

        mockMvc.perform(put("/api/v1.0/libraryEvent")
                .content(objectMapper.writeValueAsString(libraryEvent))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void putLibraryEvent4xxTest() throws Exception {

        Book book = Book.builder()
                .id("Book-1")
                .name("Harry Potter")
                .author("J.K Rowling")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .id(null)
                .book(book)
                .build();

        when(libraryEventProducer.sendLibraryEventApproachTwo(isA(LibraryEvent.class))).thenReturn(null);

        mockMvc.perform(put("/api/v1.0/libraryEvent")
                        .content(objectMapper.writeValueAsString(libraryEvent))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("please pass the id"));
    }
}
