# kotlin-spring-functional-coroutine-example
kotlin coroutine plus spring functional endpoints and reactive repositories example


## Endpoints

Server: [http://localhost:8080](http://localhost:8080)

```kotlin
    GET("/employees", employeeHandler::findAll)
    GET("/employees/{id}", employeeHandler::findById)
    POST("/employees", employeeHandler::new)
    PUT("/employees/{id}", employeeHandler::update)
    DELETE("/employees/{id}", employeeHandler::delete)
```

## Download

```bash
    git clone git@github.com:wojciech-zurek/kotlin-spring-functional-coroutine-example.git
```

## Run with gradle

```bash
    cd kotlin-spring-functional-coroutine-example/
    ./gradlew bootRun
```

## Run as jar file

```bash
    cd kotlin-spring-functional-coroutine-example/
    ./gradlew bootJar
    java -jar build/libs/kotlin-spring-functional-coroutine-example-0.0.1-SNAPSHOT.jar r
```