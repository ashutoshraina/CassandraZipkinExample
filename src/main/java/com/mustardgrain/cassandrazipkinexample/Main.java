package com.mustardgrain.cassandrazipkinexample;

import com.datastax.driver.core.*;
import com.github.kristofa.brave.*;
import com.github.kristofa.brave.http.HttpSpanCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class Main {

  private final static Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
   // insert();
    long startTime = System.currentTimeMillis();
    read();
    long estimatedTime = System.currentTimeMillis() - startTime;
    System.out.println(estimatedTime);
  }

  private static void read() throws InterruptedException, ExecutionException {

    try (Cluster cluster = Cluster.builder().addContactPoint("localhost").build()) {
      try (Session session = cluster.newSession()) {
        try (AbstractSpanCollector spanCollector = HttpSpanCollector.create("http://localhost:9411", new EmptySpanCollectorMetricsHandler())) {
          Brave brave = new Brave.Builder("Main").spanCollector(spanCollector).traceSampler(Sampler.create(1)).build();
          ClientTracer clientTracer = brave.clientTracer();
          String cql = "select * from example.users limit 20";
          PreparedStatement ps = session.prepare(cql).enableTracing();
          SpanId spanId = clientTracer.startNewSpan(cql);

          ByteBuffer traceHeaders = ByteBuffer.allocate(16);
          traceHeaders.putLong(spanId.getTraceId());
          traceHeaders.putLong(spanId.getSpanId());

          ps.setOutgoingPayload(Collections.singletonMap("zipkin", (ByteBuffer) traceHeaders.rewind()));
          clientTracer.setCurrentClientServiceName("Cassandra Zipkin Example");
          clientTracer.setClientSent();
          ArrayList<ResultSetFuture> futures = new ArrayList<>();
          for (int i = 0; i < 20; i++) {
            ResultSetFuture resultSetFuture = session.executeAsync(new BoundStatement(ps));
            futures.add(resultSetFuture);
            clientTracer.setClientReceived();
          }

          for (int i = 0; i < 20; i++) {
            ResultSetFuture resultSetFuture = futures.get(i);
            if (resultSetFuture.get().wasApplied())
              logger.info("Successfully read " + resultSetFuture.get());
            else
              logger.info("Couldn't read ");
          }

          spanCollector.flush();
        }
      }
    }
  }

  private static void insert() throws InterruptedException, ExecutionException {
    String email = "shel@ucsc.edu";
    String name = "Sheldon Finkelstein";
    String[] likes = new String[]{"Postgres", "System R"};

    try (Cluster cluster = Cluster.builder().addContactPoint("localhost").build()) {
      try (Session session = cluster.newSession()) {
        try (AbstractSpanCollector spanCollector = HttpSpanCollector.create("http://localhost:9411", new EmptySpanCollectorMetricsHandler())) {
          Brave brave = new Brave.Builder("Main").spanCollector(spanCollector).traceSampler(Sampler.create(1)).build();
          ClientTracer clientTracer = brave.clientTracer();
          String cql = "INSERT INTO example.users (id, email, name, likes, petname) VALUES (?, ?, ?, ?, ?) IF NOT EXISTS";
          PreparedStatement ps = session.prepare(cql).enableTracing();
          SpanId spanId = clientTracer.startNewSpan(cql);

          ByteBuffer traceHeaders = ByteBuffer.allocate(16);
          traceHeaders.putLong(spanId.getTraceId());
          traceHeaders.putLong(spanId.getSpanId());

          ps.setOutgoingPayload(Collections.singletonMap("zipkin", (ByteBuffer) traceHeaders.rewind()));
          clientTracer.setCurrentClientServiceName("Cassandra Zipkin Example");
          clientTracer.setClientSent();
          ArrayList<ResultSetFuture> futures = new ArrayList<>();
          for (int i = 0; i < 20; i++) {
            ResultSetFuture resultSetFuture = session.executeAsync(new BoundStatement(ps).bind(i, email, name + i, likes[i % 2], name.substring(0, 5)));
            futures.add(resultSetFuture);
            clientTracer.setClientReceived();
          }

          for (int i = 0; i < 20; i++) {
            ResultSetFuture resultSetFuture = futures.get(i);
            if (resultSetFuture.get().wasApplied())
              logger.info("Successfully inserted " + email);
            else
              logger.info("Duplicate detected for " + email);
          }

          spanCollector.flush();
        }
      }
    }
  }

}
