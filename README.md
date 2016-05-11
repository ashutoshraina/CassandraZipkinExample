# cassandra-zipkin-example

Simple example to show integrating a client with Zipkin and Cassandra

# Running

We'll be creating several tabs/windows in which to run our example, one for
each of the following:

1. The [`cassandra-zipkin-example`](https://github.com/mustardgrain/cassandra-zipkin-example) repository
1. Cassandra
1. The [`cassandra-zipkin-tracing`](https://github.com/thelastpickle/cassandra-zipkin-tracing) repository
1. The [Zipkin Server](https://github.com/openzipkin/docker-zipkin-java)

We will keep our stuff in the `/tmp` directory.

### Step 1: Clone `cassandra-zipkin-example`

Open a new tab/window to use for setting up the
`cassandra-zipkin-example` code.

```bash
cd /tmp
git clone git@github.com:mustardgrain/cassandra-zipkin-example.git
cd cassandra-zipkin-example
```

### Step 2: Install Cassandra

Open a new tab/window to download and manage our Cassandra instance.
You will need to install a version of Cassandra >= 3.4:

```bash
CASSANDRA_URL=http://archive.apache.org/dist/cassandra/3.4/apache-cassandra-3.4-bin.tar.gz
cd /tmp
curl $CASSANDRA_URL | tar xz
cd apache-cassandra-3.4
```

### Step 3: Clone `cassandra-zipkin-tracing`

Next, open a new tab/window in which to use for setting up the
`cassandra-zipkin-tracing` code.

Clone the `cassandra-zipkin-tracing` repo, build it, and install the
requisite libraries in Cassandra:

```bash
CASSANDRA_HOME=/tmp/apache-cassandra-3.4

cd /tmp
git clone git@github.com:thelastpickle/cassandra-zipkin-tracing.git
cd cassandra-zipkin-tracing

mvn -q clean package

cp lib/brave* $CASSANDRA_HOME/lib
cp target/cassandra-zipkin-tracing-* $CASSANDRA_HOME/lib

cat << EOF >> $CASSANDRA_HOME/conf/jvm.options
-Dcassandra.custom_tracing_class=com.thelastpickle.cassandra.tracing.ZipkinTracing
-Dcassandra.custom_query_handler_class=org.apache.cassandra.cql3.CustomPayloadMirroringQueryHandler
EOF
```

### Step 4: Start the Zipkin Server

Next, open a new tab/window in which to get the Zipkin Server running:

```bash
cd /tmp
git clone git@github.com:openzipkin/docker-zipkin-java.git
cd docker-zipkin-java
docker-compose up --force-recreate
```

It may take a minute or two to pull the Docker images down and start the containers.

Afterward, you'll see a message in the console:

> `Started ZipkinServer in 2.526 seconds (JVM running for 2.739)`

You're good to go to bring up the Zipkin UI in your browser: http://localhost:9411/

### Step 5: Start Cassandra

Next, go to the tab/window in you installed Cassandra (from step 2) and
let's start Cassandra running:

```bash
./bin/cassandra -f
```

If everything was working, you should see this in your logs:

> `Using com.thelastpickle.cassandra.tracing.ZipkinTracing as tracing queries (as requested with -Dcassandra.custom_tracing_class)`

and:

> `Using org.apache.cassandra.cql3.CustomPayloadMirroringQueryHandler as query handler for native protocol queries (as requested with -Dcassandra.custom_query_handler_class)`

Those reflect the configuration changes we made in step 3.

### Step 6: Run the `cassandra-zipkin-example` Application

Next, go back to the original `cassandra-zipkin-example` tab/window
and create the sample schema, build the example application, and run it:

```bash
CASSANDRA_HOME=/tmp/apache-cassandra-3.4
cat src/main/resources/cassandra-schema.cql | $CASSANDRA_HOME/bin/cqlsh

mvn -q clean package

java -jar target/cassandra-zipkin-example-1.0.0.jar
```

If all is well you will see this output:

> `Successfully inserted kirk@mustardgrain.com`

### Step 7: View the Traces in the Zipkin UI

Load (or refresh) the Zipkin UI in your browser (http://localhost:9411/) and you
should hopefully now see two new entries for `cassandra zipkin example` and
`c*:test cluster:localhost`:

`<INSERT PICTURE HERE>`

Select `cassandra zipkin example` from the drop down and then click the "Find
Traces" button.

`<INSERT PICTURE HERE>`

You will then need to click on the

`<INSERT PICTURE HERE>`
