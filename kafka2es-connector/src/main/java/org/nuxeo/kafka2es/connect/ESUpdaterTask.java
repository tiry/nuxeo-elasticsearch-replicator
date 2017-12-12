package org.nuxeo.kafka2es.connect;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ESUpdaterTask extends SinkTask {

	private static final Logger log = LoggerFactory.getLogger(ESUpdaterTask.class);

	protected RestClient lowLevelRestClient;
	protected RestHighLevelClient client;

	@Override
	public void start(Map<String, String> props) {
		lowLevelRestClient = RestClient.builder(
				new HttpHost(props.get(ESSinkConnector.ES_HOST), Integer.parseInt(props.get(ESSinkConnector.ES_PORT))))
				.build();
		client = new RestHighLevelClient(lowLevelRestClient);
	}

	@Override
	public void put(Collection<SinkRecord> records) {
		for (SinkRecord record : records) {

			JsonNode node = decodeRecodeASJson(record);
			log.info("Processing " + node.toString());

			String cmd = node.get("cmd").asText();
			log.info("Cmd " + cmd);
			try {
				if ("bulk".equals(cmd)) {
					client.bulk(execBulk(node));
				} else if ("index".equals(cmd)) {
					client.index(execIndex(node));
				} else if ("delete".equals(cmd)) {
					client.delete(execDelete(node));
				} else if ("update".equals(cmd)) {
					client.update(execUpdate(node));
				} else {
					log.warn("Unhandled command " + cmd);
				}
			} catch (IOException e) {
				log.error("Unable to execute request against ES", e);
			}
		}
	}

	protected BulkRequest execBulk(JsonNode node) {

		BulkRequest bulkRequest = new BulkRequest();
		JsonNode requests = node.get("requests");
		for (JsonNode request : requests) {
			String opType = request.get("opType").asText();
			log.info("bulk -> opType " + opType);
			if ("INDEX".equals(opType)) {
				bulkRequest.add(execIndex(request));
			} else if ("DELETE".equals(opType)) {
				bulkRequest.add(execDelete(request));
			} else if ("UPDATE".equals(opType)) {
				bulkRequest.add(execDelete(request));
			} else {
				log.warn("Unhandled command im batch" + opType);
			}
		}
		return bulkRequest;
	}

	protected IndexRequest execIndex(JsonNode node) {
		return new IndexRequest(node.get("index").asText(), node.get("type").asText(),
				String.valueOf(node.get("id").asText())).source(node.get("source").asText());
	}

	protected DeleteRequest execDelete(JsonNode node) {
		return new DeleteRequest(node.get("index").asText(), node.get("type").asText(),
				String.valueOf(node.get("id").asText()));
	}

	protected UpdateRequest execUpdate(JsonNode node) {
		return new UpdateRequest(node.get("index").asText(), node.get("type").asText(),
				String.valueOf(node.get("id").asText())).doc(node.get("source").asText());
	}

	protected JsonNode decodeRecodeASJson(SinkRecord record) {
		ObjectMapper mapper = new ObjectMapper();
		String data = decodeRecord(record);
		if (data != null) {
			try {
				return mapper.readTree(data);
			} catch (IOException e) {
				log.error("Unable to parse JSON", e);
			}
		}
		return null;
	}

	protected String decodeRecord(SinkRecord record) {
		// the serializer between nuxeo-stream (Kafka Client0 and kafka connect do not
		// match
		// therefore, the bytearry decoding contains some non ascii chars
		// => doing rough cleanup before we can fix the underlying issue
		byte[] binary = (byte[]) record.value();
		try {
			String jsonData = "";
			int startIdx = 3;
			byte cVal = binary[startIdx];
			while (cVal != 123) {
				startIdx++;
				cVal = binary[startIdx];
			}
			int idx = startIdx;
			while (idx < binary.length) {
				if (binary[idx] < 32) {
					if (idx > startIdx) {
						jsonData = jsonData + new String(binary, startIdx, idx - startIdx - 1, "UTF-8");
					}
					startIdx = idx + 1;
				}
				idx++;
			}
			jsonData = jsonData + new String(binary, startIdx, idx - startIdx - 1, "UTF-8");
			int end = jsonData.lastIndexOf("}");
			return jsonData.substring(0, end + 1);
		} catch (UnsupportedEncodingException e) {
			log.error("Unable to decode", e);
			return null;
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
