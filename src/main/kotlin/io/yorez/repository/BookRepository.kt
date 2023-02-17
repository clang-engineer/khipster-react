package io.yorez.repository

import io.yorez.domain.Book
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for the Book entity.
 */
@Suppress("unused")
@Repository
interface BookRepository : JpaRepository<Book, Long>
