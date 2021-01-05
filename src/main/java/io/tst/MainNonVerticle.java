package io.tst;

import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.pgclient.PgConnection;
import io.vertx.pgclient.impl.PgConnectionImpl;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.SqlConnection;
import io.vertx.sqlclient.PoolOptions;

import java.lang.reflect.Constructor;


public class MainNonVerticle {
	public static void main(String[] args) {
		PgConnectOptions connectOptions = new PgConnectOptions().setPort(5432).setHost("localhost").setPort(5432)
		                                                        .setPassword("password").setDatabase("postgres").setUser("postgres");
		PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
		PgPool client = PgPool.pool(connectOptions, poolOptions);
		// same thing happens with rxGetConnection. just using this one
		client.getConnection(connRes -> {
			SqlConnection sqlConnection = connRes.result();
			System.out.println("To string: " + sqlConnection);	// io.vertx.pgclient.impl.PgConnectionImpl
			Constructor<?>[] constructors = sqlConnection.getClass().getConstructors();
			System.out.println("Constructors: ");
			for (int i = 0; i < constructors.length; i++) {
				// 	io.vertx.reactivex.sqlclient.SqlConnection
				// 	io.vertx.reactivex.sqlclient.SqlConnection
				System.out.println(constructors[i].getName());
			}
			System.out.println("Simple name:" + sqlConnection.getClass().getSimpleName()); // SqlConnection
			System.out.println("Name:" + sqlConnection.getClass().getName());	// io.vertx.reactivex.sqlclient.SqlConnection
			System.out.println("Class: " + sqlConnection.getClass()); // io.vertx.reactivex.sqlclient.SqlConnection
			System.out.println("Conn name: " + sqlConnection.getClass().getCanonicalName()); // io.vertx.reactivex.sqlclient.SqlConnection
			// the bottom 2 work:
			try {
				// just showing it doesnt extend this either
				io.vertx.pgclient.PgConnection pgConnection = (io.vertx.pgclient.PgConnection) sqlConnection.getDelegate();
				System.out.println("Worked.");
			} catch (Throwable e) {
				// fails
				e.printStackTrace();
			}
			try {
				io.vertx.reactivex.pgclient.PgConnection pgConnection2 = io.vertx.reactivex.pgclient.PgConnection.newInstance((io.vertx.pgclient.PgConnection) sqlConnection.getDelegate());
				System.out.println("Rxified. Worked.");
			} catch (Throwable e) {
				e.printStackTrace();
			}

			// fails
			try {
				io.vertx.reactivex.pgclient.PgConnection pgConn = (io.vertx.reactivex.pgclient.PgConnection) sqlConnection;
			} catch (Throwable e) {
				// fails
				System.out.println(e.getMessage());
//				e.printStackTrace();
			}
			// fails
			try {
				io.vertx.pgclient.PgConnection pgConn = (io.vertx.pgclient.PgConnection) sqlConnection;
			} catch (Throwable e) {
				System.out.println(e.getMessage());
//				e.printStackTrace();
			}
			// fails
			try {
				io.vertx.reactivex.pgclient.PgConnection pgConn = (io.vertx.reactivex.pgclient.PgConnection) sqlConnection.getDelegate();
			} catch (Exception e) {
				System.out.println(e.getMessage());
//				e.printStackTrace();
			}

			client.close();
		});
	}

}
