# Rxified Casting Issue
Trying to cast the rxified getConnection (or rxGetConnection) to PgConnection results in a ClassCastException: 

`class io.vertx.reactivex.sqlclient.SqlConnection cannot be cast to class io.vertx.reactivex.pgclient.PgConnection (io.vertx.reactivex.sqlclient.SqlConnection and io.vertx.reactivex.pgclient.PgConnection are in unnamed module of loader 'app')`

Non-rxified:
```java
// left out generics below

abstract class PoolBase extends SqlClientBase implements Pool {

  // PgPoolImpl will implement this, returning PgConnectionImpl (no problem non-rxified)
  protected abstract SqlConnectionImpl wrap(ContextInternal context, Connection conn);

  // relevant
   @Override
  public Future<SqlConnection> getConnection() {
    ContextInternal current = vertx.getOrCreateContext();
    Object metric;
    if (metrics != null) {
      metric = metrics.enqueueRequest();
    } else {
      metric = null;
    }
    Promise<Connection> promise = current.promise();
    acquire(promise);
    if (metrics != null) {
      promise.future().onComplete(ar -> {
        metrics.dequeueRequest(metric);
      });
    }
    return promise.future().map(conn -> {
      SqlConnectionImpl wrapper = wrap(current, conn);
      conn.init(wrapper);
      return wrapper;
    });
  }

}


class PgPoolImpl extends PoolBase implements PgPool {
  // wrap is called by getConnection of PoolBase and gives you the PgConnection
   @Override
  protected SqlConnectionImpl wrap(ContextInternal context, Connection conn) {
    return new PgConnectionImpl(factory, context, conn, tracer, metrics);
  }
}

```

RxVersion:
```java

@RxGen(io.vertx.sqlclient.Pool.class)
public class Pool extends io.vertx.reactivex.sqlclient.SqlClient {
  // this is where the error is. 
  // PgPool below extends this class and passes PgPool as the delegate 
  // delegate.getConnection() is called and returns PgConnectionImpl
  // So far so good.
  // HOWEVER, this is where it breaks down. now the returned PgConnectionImpl 
  // is wrapped with the rxified SqlConnection rather than the rxified PgConnection
  /**
   * Get a connection from the pool.
   * @param handler the handler that will get the connection result
   */
  public void getConnection(Handler<AsyncResult<io.vertx.reactivex.sqlclient.SqlConnection>> handler) { 
    delegate.getConnection(new Handler<AsyncResult<io.vertx.sqlclient.SqlConnection>>() {
      public void handle(AsyncResult<io.vertx.sqlclient.SqlConnection> ar) {
        if (ar.succeeded()) {
          handler.handle(io.vertx.core.Future.succeededFuture(io.vertx.reactivex.sqlclient.SqlConnection.newInstance((io.vertx.sqlclient.SqlConnection)ar.result())));
        } else {
          handler.handle(io.vertx.core.Future.failedFuture(ar.cause()));
        }
      }
    });
  }
}

@RxGen(io.vertx.pgclient.PgPool.class)
public class PgPool extends io.vertx.reactivex.sqlclient.Pool {
  
  private final io.vertx.pgclient.PgPool delegate;
  
  public PgPool(io.vertx.pgclient.PgPool delegate) {
    super(delegate);
    this.delegate = delegate;
  }

}

```

To get around this issue, you call:
```java
  // rxified
  io.vertx.reactivex.pgclient.PgPool.rxGetConnection(conn -> 
    io.vertx.reactivex.pgclient.PgPool.rxGetConnection.newInstance(
      (io.vertx.pgclient.PgConnection)conn.getDelegate()
    )
  )
  // non-rxified version:
  io.vertx.reactivex.pgclient.PgPool.rxGetConnection(conn -> 
    (io.vertx.pgclient.PgConnection)conn.getDelegate()
  )
```

## Setup:
Start postgres with password in the main class.
`docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=password postgres:13.1-alpine`
Run
`./gradlew clean shadowJar`
Run
`java -jar ./build/libs/VertxUnnamedModuleSqlReproducer-fat.jar`

Also, I'm using java 11.