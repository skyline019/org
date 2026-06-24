# Getting Started

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.1.0/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.1.0/maven-plugin/build-image.html)
* [Spring Data JDBC](https://docs.spring.io/spring-boot/4.1.0/reference/data/sql.html#data.sql.jdbc)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.1.0/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Data LDAP](https://docs.spring.io/spring-boot/4.1.0/reference/data/nosql.html#data.nosql.ldap)
* [Rest Repositories](https://docs.spring.io/spring-boot/4.1.0/how-to/data-access.html#howto.data-access.exposing-spring-data-repositories-as-rest)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/4.1.0/reference/using/devtools.html)
* [JDBC API](https://docs.spring.io/spring-boot/4.1.0/reference/data/sql.html)
* [LDAP](https://docs.spring.io/spring-boot/4.1.0/reference/data/nosql.html#data.nosql.ldap)
* [OAuth2 Authorization Server](https://docs.spring.io/spring-boot/4.1.0/reference/web/spring-security.html#web.security.oauth2.authorization-server)
* [OAuth2 Client](https://docs.spring.io/spring-boot/4.1.0/reference/web/spring-security.html#web.security.oauth2.client)
* [OAuth2 Resource Server](https://docs.spring.io/spring-boot/4.1.0/reference/web/spring-security.html#web.security.oauth2.server)
* [OTLP for metrics](https://docs.spring.io/spring-boot/4.1.0/reference/actuator/metrics.html#actuator.metrics.export.otlp)
* [Prometheus](https://docs.spring.io/spring-boot/4.1.0/reference/actuator/metrics.html#actuator.metrics.export.prometheus)
* [Spring Security](https://docs.spring.io/spring-boot/4.1.0/reference/web/spring-security.html)
* [Spring Session for JDBC](https://docs.spring.io/spring-session/reference/)
* [HTTP Client](https://docs.spring.io/spring-boot/4.1.0/reference/io/rest-client.html#io.rest-client.restclient)
* [Reactive HTTP Client](https://docs.spring.io/spring-boot/4.1.0/reference/io/rest-client.html#io.rest-client.webclient)
* [Thymeleaf](https://docs.spring.io/spring-boot/4.1.0/reference/web/servlet.html#web.servlet.spring-mvc.template-engines)
* [Embedded LDAP Server](https://docs.spring.io/spring-boot/4.1.0/reference/data/nosql.html#data.nosql.ldap.embedded)
* [Spring Web](https://docs.spring.io/spring-boot/4.1.0/reference/web/servlet.html)
* [Spring Web Services](https://docs.spring.io/spring-boot/4.1.0/reference/io/webservices.html)
* [Spring Reactive Web](https://docs.spring.io/spring-boot/4.1.0/reference/web/reactive.html)

### Guides

The following guides illustrate how to use some features concretely:

* [Using Spring Data JDBC](https://github.com/spring-projects/spring-data-examples/tree/main/jdbc/basics)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Accessing JPA Data with REST](https://spring.io/guides/gs/accessing-data-rest/)
* [Accessing Neo4j Data with REST](https://spring.io/guides/gs/accessing-neo4j-data-rest/)
* [Accessing MongoDB Data with REST](https://spring.io/guides/gs/accessing-mongodb-data-rest/)
* [Accessing Relational Data using JDBC with Spring](https://spring.io/guides/gs/relational-data-access/)
* [Managing Transactions](https://spring.io/guides/gs/managing-transactions/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Handling Form Submission](https://spring.io/guides/gs/handling-form-submission/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Producing a SOAP web service](https://spring.io/guides/gs/producing-web-service/)
* [Building a Reactive RESTful Web Service](https://spring.io/guides/gs/reactive-rest-service/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the
parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

