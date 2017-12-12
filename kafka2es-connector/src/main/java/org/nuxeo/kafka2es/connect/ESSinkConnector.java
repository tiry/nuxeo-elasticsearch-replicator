package org.nuxeo.kafka2es.connect;
        
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.utils.AppInfoParser;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ESSinkConnector extends SinkConnector {

	public static final String ES_HOST = "es.host";
	public static final String ES_PORT = "es.port";

	protected String esHost;
	protected String esPort;

	@Override
	public String version() {
		return AppInfoParser.getVersion();
	}

	@Override
	public void start(Map<String, String> props) {		
		esHost = props.get(ES_HOST);
		esPort = props.get(ES_PORT);
	}

	@Override
	public Class<? extends Task> taskClass() {
		return ESUpdaterTask.class;
	}

	@Override
	public List<Map<String, String>> taskConfigs(int maxTasks) {

		ArrayList<Map<String, String>> configs = new ArrayList<>();
		for (int i = 0; i < maxTasks; i++) {
			Map<String, String> config = new HashMap<>();
			if (esHost != null)
				config.put(ES_HOST, esHost);
			if (esPort != null)
				config.put(ES_PORT, esPort);
			configs.add(config);
		}
		return configs;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}

	@Override
	public ConfigDef config() {		
		ConfigDef config = new ConfigDef();				
		config.define(ES_HOST, Type.STRING,Importance.HIGH,"Elasticsearch host");
		config.define(ES_PORT, Type.STRING,Importance.HIGH,"Elasticsearch port");		
		return config;
	}
}
