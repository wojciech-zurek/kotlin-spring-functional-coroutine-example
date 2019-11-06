package eu.wojciechzurek.example.kotlinspringfunctionalcoroutineexample

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.core.*
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.data.relational.core.mapping.Table
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import java.net.URI
import javax.validation.constraints.NotBlank

@SpringBootApplication
@EnableR2dbcRepositories
class ExampleApplication {

    @Bean
    fun routes(employeeHandler: EmployeeHandler) = coRouter {
        GET("/employees", employeeHandler::findAll)
        GET("/employees/{id}", employeeHandler::findById)
        POST("/employees", employeeHandler::new)
        PUT("/employees/{id}", employeeHandler::update)
        DELETE("/employees/{id}", employeeHandler::delete)
    }
}

fun main(args: Array<String>) {
    runApplication<ExampleApplication>(*args)
}

@Component
class InitRunner(
        private val client: DatabaseClient,
        private val employeeRepository: EmployeeRepository
) : InitializingBean {

    override fun afterPropertiesSet() {

        runBlocking {
            client
                    .execute("CREATE TABLE IF NOT EXISTS employees ( id SERIAL PRIMARY KEY, name VARCHAR(100) NOT NULL);")
                    .fetch()
                    .flow()
                    .collect { println(it) }

            employeeRepository.deleteAll()

            listOf("wojtek", "admin", "test")
                    .map {
                        Employee(name = it)
                    }
                    .map { employeeRepository.new(it) }

            employeeRepository
                    .findAll()
                    .collect {
                        println(it)
                    }
        }
    }
}

@Component
class EmployeeHandler(private val repository: EmployeeRepository) {
    suspend fun findAll(request: ServerRequest): ServerResponse = ok().bodyAndAwait(repository.findAll())

    suspend fun findById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLong()

        return repository
                .findById(id)?.let {
                    ok().bodyValueAndAwait(it)
                } ?: notFound().buildAndAwait()
    }

    suspend fun new(request: ServerRequest): ServerResponse {
        val employeeRequest = request.awaitBody<EmployeeRequest>()
        val id = repository.new(Employee(name = employeeRequest.name))?.get("ID")

        return created(URI.create("/api/user/$id")).buildAndAwait()
    }

    suspend fun update(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLong()
        val employeeRequest = request.awaitBody<EmployeeRequest>()

        return repository.findById(id)?.let {
            val employee = Employee(it.id, employeeRequest.name)
            repository.update(employee)
            ok().bodyValueAndAwait(employee)
        } ?: notFound().buildAndAwait()
    }

    suspend fun delete(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLong()

        return repository.findById(id)?.let {
            repository.deleteById(it.id!!)
            noContent().buildAndAwait()
        } ?: notFound().buildAndAwait()
    }
}

@Component
class EmployeeRepository(private val client: DatabaseClient) {

    suspend fun findAll() =
            client
                    .select()
                    .from("employees")
                    .asType<Employee>()
                    .fetch()
                    .flow()

    suspend fun findById(id: Long): Employee? = client
            .execute("SELECT * FROM employees WHERE id = :id")
            .bind("id", id)
            .asType<Employee>()
            .fetch()
            .awaitOneOrNull()

    suspend fun new(employee: Employee): MutableMap<String, Any>? =
            client.insert()
                    .into<Employee>()
                    .table("employees")
                    .using(employee)
                    .fetch()
                    .awaitFirst()

    suspend fun update(employee: Employee) =
            client.update()
                    .table(Employee::class.java)
                    .using(employee)
                    .fetch()
                    .rowsUpdated()
                    .awaitFirstOrNull()

    suspend fun deleteById(id: Long) {
        client
                .execute("DELETE FROM employees WHERE id = :id")
                .bind("id", id)
                .fetch()
                .awaitFirstOrNull()
    }

    suspend fun deleteAll() = client.execute("DELETE FROM employees")
            .fetch()
            .flow()
}

data class EmployeeRequest(@NotBlank val name: String)

@Table("employees")
data class Employee(@Id val id: Long? = null, @NotBlank val name: String)
