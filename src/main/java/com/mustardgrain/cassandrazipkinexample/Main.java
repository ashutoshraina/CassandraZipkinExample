package com.mustardgrain.cassandrazipkinexample;

import com.datastax.driver.core.*;
import com.github.kristofa.brave.*;
import com.github.kristofa.brave.http.HttpSpanCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collections;

public class Main {

    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        String email = "kirk@mustardgrain.com";
        String name = "Kirk True";

        try (Cluster cluster = Cluster.builder().addContactPoint("localhost").build()) {
            try (Session session = cluster.newSession()) {
                try (AbstractSpanCollector spanCollector = HttpSpanCollector.create("http://localhost:9411", new EmptySpanCollectorMetricsHandler())) {
                    Brave brave = new Brave.Builder("Main").spanCollector(spanCollector).traceSampler(Sampler.create(1)).build();
                    ClientTracer clientTracer = brave.clientTracer();
                    String cql = "INSERT INTO cassandra_zipkin_example.users (email, name) VALUES (?, ?) IF NOT EXISTS";
                    PreparedStatement ps = session.prepare(cql).enableTracing();
                    SpanId spanId = clientTracer.startNewSpan(cql);

                    ByteBuffer traceHeaders = ByteBuffer.allocate(16);
                    traceHeaders.putLong(spanId.getTraceId());
                    traceHeaders.putLong(spanId.getSpanId());

                    ps.setOutgoingPayload(Collections.singletonMap("zipkin", (ByteBuffer) traceHeaders.rewind()));
                    clientTracer.setCurrentClientServiceName("Cassandra Zipkin Example");
                    clientTracer.setClientSent();
                    ResultSet rs = session.execute(new BoundStatement(ps).bind(email, name));
                    clientTracer.setClientReceived();

                    if (rs.wasApplied())
                        logger.info("Successfully inserted " + email);
                    else
                        logger.info("Duplicate detected for " + email);

                    spanCollector.flush();
                }
            }
        }
    }

}
