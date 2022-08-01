package com.library.producer.domain;

import com.library.producer.entity.Book;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class LibraryEvent {
    private Integer id;
    private LibraryEventType libraryEventType;
    @NotNull
    @Valid
    private Book book;
}
