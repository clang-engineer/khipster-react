package io.yorez.web.rest

import io.yorez.IntegrationTest
import io.yorez.domain.Book
import io.yorez.repository.BookRepository
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.Validator
import java.util.Random
import java.util.concurrent.atomic.AtomicLong
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

/**
 * Integration tests for the [BookResource] REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class BookResourceIT {
    @Autowired
    private lateinit var bookRepository: BookRepository

    @Autowired
    private lateinit var jacksonMessageConverter: MappingJackson2HttpMessageConverter

    @Autowired
    private lateinit var pageableArgumentResolver: PageableHandlerMethodArgumentResolver

    @Autowired
    private lateinit var validator: Validator

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restBookMockMvc: MockMvc

    private lateinit var book: Book

    @BeforeEach
    fun initTest() {
        book = createEntity(em)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createBook() {
        val databaseSizeBeforeCreate = bookRepository.findAll().size
        // Create the Book
        restBookMockMvc.perform(
            post(ENTITY_API_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(book))
        ).andExpect(status().isCreated)

        // Validate the Book in the database
        val bookList = bookRepository.findAll()
        assertThat(bookList).hasSize(databaseSizeBeforeCreate + 1)
        val testBook = bookList[bookList.size - 1]

        assertThat(testBook.title).isEqualTo(DEFAULT_TITLE)
        assertThat(testBook.description).isEqualTo(DEFAULT_DESCRIPTION)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createBookWithExistingId() {
        // Create the Book with an existing ID
        book.id = 1L

        val databaseSizeBeforeCreate = bookRepository.findAll().size
        // An entity with an existing ID cannot be created, so this API call must fail
        restBookMockMvc.perform(
            post(ENTITY_API_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(book))
        ).andExpect(status().isBadRequest)

        // Validate the Book in the database
        val bookList = bookRepository.findAll()
        assertThat(bookList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun checkTitleIsRequired() {
        val databaseSizeBeforeTest = bookRepository.findAll().size
        // set the field null
        book.title = null

        // Create the Book, which fails.

        restBookMockMvc.perform(
            post(ENTITY_API_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(book))
        ).andExpect(status().isBadRequest)

        val bookList = bookRepository.findAll()
        assertThat(bookList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllBooks() {
        // Initialize the database
        bookRepository.saveAndFlush(book)

        // Get all the bookList
        restBookMockMvc.perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(book.id?.toInt())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getBook() {
        // Initialize the database
        bookRepository.saveAndFlush(book)

        val id = book.id
        assertNotNull(id)

        // Get the book
        restBookMockMvc.perform(get(ENTITY_API_URL_ID, book.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(book.id?.toInt()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
    }
    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingBook() {
        // Get the book
        restBookMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }
    @Test
    @Transactional
    fun putExistingBook() {
        // Initialize the database
        bookRepository.saveAndFlush(book)

        val databaseSizeBeforeUpdate = bookRepository.findAll().size

        // Update the book
        val updatedBook = bookRepository.findById(book.id).get()
        // Disconnect from session so that the updates on updatedBook are not directly saved in db
        em.detach(updatedBook)
        updatedBook.title = UPDATED_TITLE
        updatedBook.description = UPDATED_DESCRIPTION

        restBookMockMvc.perform(
            put(ENTITY_API_URL_ID, updatedBook.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(updatedBook))
        ).andExpect(status().isOk)

        // Validate the Book in the database
        val bookList = bookRepository.findAll()
        assertThat(bookList).hasSize(databaseSizeBeforeUpdate)
        val testBook = bookList[bookList.size - 1]
        assertThat(testBook.title).isEqualTo(UPDATED_TITLE)
        assertThat(testBook.description).isEqualTo(UPDATED_DESCRIPTION)
    }

    @Test
    @Transactional
    fun putNonExistingBook() {
        val databaseSizeBeforeUpdate = bookRepository.findAll().size
        book.id = count.incrementAndGet()

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restBookMockMvc.perform(
            put(ENTITY_API_URL_ID, book.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(book))
        )
            .andExpect(status().isBadRequest)

        // Validate the Book in the database
        val bookList = bookRepository.findAll()
        assertThat(bookList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun putWithIdMismatchBook() {
        val databaseSizeBeforeUpdate = bookRepository.findAll().size
        book.id = count.incrementAndGet()

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBookMockMvc.perform(
            put(ENTITY_API_URL_ID, count.incrementAndGet())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(book))
        ).andExpect(status().isBadRequest)

        // Validate the Book in the database
        val bookList = bookRepository.findAll()
        assertThat(bookList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun putWithMissingIdPathParamBook() {
        val databaseSizeBeforeUpdate = bookRepository.findAll().size
        book.id = count.incrementAndGet()

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBookMockMvc.perform(
            put(ENTITY_API_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(book))
        )
            .andExpect(status().isMethodNotAllowed)

        // Validate the Book in the database
        val bookList = bookRepository.findAll()
        assertThat(bookList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun partialUpdateBookWithPatch() {
        bookRepository.saveAndFlush(book)

        val databaseSizeBeforeUpdate = bookRepository.findAll().size

// Update the book using partial update
        val partialUpdatedBook = Book().apply {
            id = book.id

            title = UPDATED_TITLE
            description = UPDATED_DESCRIPTION
        }

        restBookMockMvc.perform(
            patch(ENTITY_API_URL_ID, partialUpdatedBook.id)
                .contentType("application/merge-patch+json")
                .content(convertObjectToJsonBytes(partialUpdatedBook))
        )
            .andExpect(status().isOk)

// Validate the Book in the database
        val bookList = bookRepository.findAll()
        assertThat(bookList).hasSize(databaseSizeBeforeUpdate)
        val testBook = bookList.last()
        assertThat(testBook.title).isEqualTo(UPDATED_TITLE)
        assertThat(testBook.description).isEqualTo(UPDATED_DESCRIPTION)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun fullUpdateBookWithPatch() {
        bookRepository.saveAndFlush(book)

        val databaseSizeBeforeUpdate = bookRepository.findAll().size

// Update the book using partial update
        val partialUpdatedBook = Book().apply {
            id = book.id

            title = UPDATED_TITLE
            description = UPDATED_DESCRIPTION
        }

        restBookMockMvc.perform(
            patch(ENTITY_API_URL_ID, partialUpdatedBook.id)
                .contentType("application/merge-patch+json")
                .content(convertObjectToJsonBytes(partialUpdatedBook))
        )
            .andExpect(status().isOk)

// Validate the Book in the database
        val bookList = bookRepository.findAll()
        assertThat(bookList).hasSize(databaseSizeBeforeUpdate)
        val testBook = bookList.last()
        assertThat(testBook.title).isEqualTo(UPDATED_TITLE)
        assertThat(testBook.description).isEqualTo(UPDATED_DESCRIPTION)
    }

    @Throws(Exception::class)
    fun patchNonExistingBook() {
        val databaseSizeBeforeUpdate = bookRepository.findAll().size
        book.id = count.incrementAndGet()

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restBookMockMvc.perform(
            patch(ENTITY_API_URL_ID, book.id)
                .contentType("application/merge-patch+json")
                .content(convertObjectToJsonBytes(book))
        )
            .andExpect(status().isBadRequest)

        // Validate the Book in the database
        val bookList = bookRepository.findAll()
        assertThat(bookList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun patchWithIdMismatchBook() {
        val databaseSizeBeforeUpdate = bookRepository.findAll().size
        book.id = count.incrementAndGet()

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBookMockMvc.perform(
            patch(ENTITY_API_URL_ID, count.incrementAndGet())
                .contentType("application/merge-patch+json")
                .content(convertObjectToJsonBytes(book))
        )
            .andExpect(status().isBadRequest)

        // Validate the Book in the database
        val bookList = bookRepository.findAll()
        assertThat(bookList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun patchWithMissingIdPathParamBook() {
        val databaseSizeBeforeUpdate = bookRepository.findAll().size
        book.id = count.incrementAndGet()

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBookMockMvc.perform(
            patch(ENTITY_API_URL)
                .contentType("application/merge-patch+json")
                .content(convertObjectToJsonBytes(book))
        )
            .andExpect(status().isMethodNotAllowed)

        // Validate the Book in the database
        val bookList = bookRepository.findAll()
        assertThat(bookList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteBook() {
        // Initialize the database
        bookRepository.saveAndFlush(book)
        val databaseSizeBeforeDelete = bookRepository.findAll().size
        // Delete the book
        restBookMockMvc.perform(
            delete(ENTITY_API_URL_ID, book.id)
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        // Validate the database contains one less item
        val bookList = bookRepository.findAll()
        assertThat(bookList).hasSize(databaseSizeBeforeDelete - 1)
    }

    companion object {

        private const val DEFAULT_TITLE = "AAAAAAAAAA"
        private const val UPDATED_TITLE = "BBBBBBBBBB"

        private const val DEFAULT_DESCRIPTION = "AAAAAAAAAA"
        private const val UPDATED_DESCRIPTION = "BBBBBBBBBB"

        private val ENTITY_API_URL: String = "/api/books"
        private val ENTITY_API_URL_ID: String = ENTITY_API_URL + "/{id}"

        private val random: Random = Random()
        private val count: AtomicLong = AtomicLong(random.nextInt().toLong() + (2 * Integer.MAX_VALUE))

        /**
         * Create an entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createEntity(em: EntityManager): Book {
            val book = Book(
                title = DEFAULT_TITLE,

                description = DEFAULT_DESCRIPTION

            )

            return book
        }

        /**
         * Create an updated entity for this test.
         *
         * This is a static method, as tests for other entities might also need it,
         * if they test an entity which requires the current entity.
         */
        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Book {
            val book = Book(
                title = UPDATED_TITLE,

                description = UPDATED_DESCRIPTION

            )

            return book
        }
    }
}
