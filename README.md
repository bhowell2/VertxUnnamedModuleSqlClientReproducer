# Rxified Casting Issue
Trying to cast the rxified getConnection (or rxGetConnection) to PgConnection results in a ClassCastException: 

`java.lang.ClassCastException: class io.vertx.reactivex.sqlclient.SqlConnection cannot be cast to class io.vertx.pgclient.PgConnection (io.vertx.reactivex.sqlclient.SqlConnection and io.vertx.pgclient.PgConnection are in unnamed module of loader 'app')`

## Setup:
Start postgres with password in the main class.
`docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=password postgres:13.1-alpine`
Run
`./gradlew clean shadowJar`
Run
`java -jar ./build/libs/VertxUnnamedModuleSqlReproducer-fat.jar`

Also, I'm using java 11.