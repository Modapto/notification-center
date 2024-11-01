# modapto-notification-center

## Overview

Notification center is responsible for notifying users in MODAPTO regarding events from MODAPTO modules or Smart Services and assignments that can be created between two users.

Notification Center is connected with User Inteface, Production Knowledge Base and Message Bus to facilitate such operations.

It is based on Java Spring Boot framework utilizing Java 21. At the moment all endpoints require no authentication - JWT tokens / CSRF token.

## Table of Contents

1. [Installation](#installation)
2. [Usage](#usage)
3. [Deployment](#deployment)
4. [License](#license)
5. [Contributors](#contributors)

### Installation

1. Clone the repository:

    ```sh
    git clone https://github.com/Modapto/notification-center.git
    cd notification-center
    ```

2. Install the dependencies:

    ```sh
    mvn install
    ```

3. Add an application-local.properties file with the following variables:

    ```sh
    environment.name=local
    spring.elasticsearch.uris=
    spring.elasticsearch.username=
    spring.elasticsearch.password=

    spring.kafka.bootstrap-servers=

    spring.security.oauth2.resourceserver.jwt.issuer-uri=
    keycloak.client=
    keycloak.client.secret=
    keycloak.token-uri=
    user.manager.component.url=
    ```

### Usage

1. Run the application after Keycloak is deployed:

    ```sh
    mvn spring-boot:run
    ```

2. The application will start on `http://localhost:8091`.

3. Access the OpenAPI documentation at `http://localhost:8091/api/notification-center/swagger-ui/index.html`.

### Deployment

For local deployment Docker containers can be utilized to deploy the microservice with the following procedure:

1. Ensure Docker is installed and running.

2. Build the maven project:

    ```sh
    mvn package
    ```

3. Build the Docker container:

    ```sh
    docker build -t modapto-notification-center .
    ```

4. Run the Docker container including the environmental variables:

    ```sh
    docker run -d -p 8091:8091 --name modapto-notification-center modapto-notification-center
    ```

5. To stop container run:

    ```sh
   docker stop modapto-notification-center
    ```

## License

TThis project has received funding from the European Union's Horizon 2022 research and innovation programm, under Grant Agreement 101091996.

For more details about the licence, see the [LICENSE](LICENSE) file.

## Contributors

- Alkis Aznavouridis (<a.aznavouridis@atc.gr>)
