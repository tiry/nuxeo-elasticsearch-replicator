package org.nuxeo.kafka2es.connect;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESUpdaterTask extends SinkTask {

	private static final Logger log = LoggerFactory.getLogger(ESUpdaterTask.class);

	protected RestClient lowLevelRestClient;
	protected RestHighLevelClient client;

	@Override
	public void start(Map<String, String> props) {

		System.out.println("STARTING TASK");

		lowLevelRestClient = RestClient.builder(
				new HttpHost(props.get(ESSinkConnector.ES_HOST), Integer.parseInt(props.get(ESSinkConnector.ES_PORT))))
				.build();
		client = new RestHighLevelClient(lowLevelRestClient);
		
		System.out.println("TASK STARTED!");
	}

	@Override
	public void put(Collection<SinkRecord> records) {
		for (SinkRecord record : records) {

			log.info("Processing {}", record.value());
			log.info(record.value().getClass().toString());

			
		}
	}

	@Override
	public void flush(Map<TopicPartition, OffsetAndMetadata> offsets) {
	}

	@Override
	public void stop() {
		try {
			lowLevelRestClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String version() {
		return new ESSinkConnector().version();
	}
}
