package com.library.producer.entity;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Book {
    @NotNull
    private String id;
    @NotBlank
    private String name;
    @NotBlank
    private String author;
}
