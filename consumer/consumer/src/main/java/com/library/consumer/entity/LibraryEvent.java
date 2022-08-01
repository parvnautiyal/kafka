package com.library.consumer.entity;

import lombok.*;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
@Entity
public class LibraryEvent {
    @Id
    @GeneratedValue
    private Integer id;
    @Enumerated(EnumType.STRING)
    private LibraryEventType libraryEventType;
    @NotNull
    @Valid
    @OneToOne(mappedBy = "libraryEvent" , cascade = {CascadeType.ALL})
    @ToString.Exclude
    private Book book;
}
