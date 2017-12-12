/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteRequest.OpType;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

/**
 * @since 9.3
 */
public class ESDuplicatorClient extends ESRestClient {

	private static final Log log = LogFactory.getLog(ESDuplicatorClient.class);

	protected final String ES_OPLOG_CONFIG_PROP = "nuxeo.stream.es.oplog.config";

	protected final String DEFAULT_OPLOG_CONFIG_PROP = "esoplog";

	public static final String STREAM_NAME = "esoplog";

	protected final AtomicLong counter = new AtomicLong();

	protected LogManager oplogManager = null;

	protected LogAppender<Record> appender = null;

	public ESDuplicatorClient(RestClient lowLevelRestClient, RestHighLevelClient client) {
		super(lowLevelRestClient, client);
	}

	protected LogAppender<Record> getOpLogAppender() {
		if (appender == null) {
			synchronized (this) {
				if (appender == null) {
					StreamService service = Framework.getService(StreamService.class);
					LogManager oplogManager = service.getLogManager(getStreamConfig());
					oplogManager.createIfNotExists(STREAM_NAME, 1);
					appender = oplogManager.getAppender(STREAM_NAME);
				}
			}
		}
		return appender;
	}

	protected String getStreamConfig() {
		return Framework.getProperty(ES_OPLOG_CONFIG_PROP, DEFAULT_OPLOG_CONFIG_PROP);
	}

	protected void addOpToLog(String jsonPayLoad) {
		try {

			String key = System.currentTimeMillis() + "-" + counter.incrementAndGet();
			getOpLogAppender().append(0, Record.of(key, jsonPayLoad.getBytes(StandardCharsets.UTF_8)));

			log.debug("added to oplog:" + jsonPayLoad);
		} catch (Exception e) {
			log.error("Unable to add stream entry:" + jsonPayLoad, e);
		}
	}

	protected String asJson(Map<String, Serializable> map) {
		try {
			return new ObjectMapper().writeValueAsString(map);
		} catch (IOException e) {
			log.warn("Unable to translate entry into json: " + e.getMessage(), e);
			return null;
		}
	}

	protected void writeOp(String cmd, String indexName, Map<String, Serializable> map) {
		if (map == null) {
			map = new HashMap<>();
		}
		if (cmd != null) {
			map.put("cmd", cmd);
		}
		if (indexName != null) {
			map.put("index", indexName);
		}
		addOpToLog(asJson(map));
	}

	@Override
	public void deleteIndex(String indexName, int timeoutSecond) {
		super.deleteIndex(indexName, timeoutSecond);
		Map<String, Serializable> map = new HashMap<>();
		map.put("timeoutSecond", Integer.toString(timeoutSecond));
		writeOp("deleteIndex", indexName, map);
	}

	@Override
	public void createIndex(String indexName, String jsonSettings) {
		super.createIndex(indexName, jsonSettings);
		Map<String, Serializable> map = new HashMap<>();
		map.put("jsonSettings", jsonSettings);
		writeOp("createIndex", indexName, map);
	}

	@Override
	public void createMapping(String indexName, String type, String jsonMapping) {
		super.createMapping(indexName, type, jsonMapping);
		Map<String, Serializable> map = new HashMap<>();
		map.put("type", type);
		map.put("jsonMapping", jsonMapping);
		writeOp("createMapping", indexName, map);
	}

	@Override
	public void updateAlias(String aliasName, String indexName) {
		// TODO do this in a single call to make it atomically
		if (aliasExists(aliasName)) {
			deleteAlias(aliasName);
		}
		if (indexExists(aliasName)) {
			throw new NuxeoException("Can create an alias because an index with the same name exists: " + aliasName);
		}
		createAlias(aliasName, indexName);
	}

	protected void deleteAlias(String aliasName) {
		super.deleteAlias(aliasName);
		Map<String, Serializable> map = new HashMap<>();
		map.put("aliasName", aliasName);
		writeOp("deleteAlias", null, map);
	}

	protected void createAlias(String aliasName, String indexName) {
		super.createAlias(aliasName, indexName);
		Map<String, Serializable> map = new HashMap<>();
		map.put("aliasName", aliasName);
		writeOp("createAlias", indexName, map);
	}

	@Override
	public BulkResponse bulk(BulkRequest request) {
		BulkResponse response = super.bulk(request);

		ArrayList<Map<String, Serializable>> dataList = new ArrayList<>();

		for (DocWriteRequest<?> req : request.requests()) {

			Map<String, Serializable> data = new HashMap<>();

			if (req.opType() == OpType.INDEX) {
				data = extractMetaData((IndexRequest) req);
			} else if (req.opType() == OpType.UPDATE) {
				data = extractMetaData((UpdateRequest) req);
			} else if (req.opType() == OpType.DELETE) {
				data = extractMetaData((DeleteRequest) req);
			} else {
				data.put("opType", req.opType().name());
			}

			data.put("index", req.index());
			dataList.add(data);
		}

		Map<String, Serializable> map = new HashMap<>();
		map.put("requests", dataList);
		writeOp("bulk", null, map);
		return response;
	}

	@Override
	public DeleteResponse delete(DeleteRequest request) {
		DeleteResponse response = super.delete(request);
		Map<String, Serializable> map = extractMetaData(request);
		writeOp("delete", request.index(), map);
		return response;
	}

	@Override
	public IndexResponse index(IndexRequest request) {
		IndexResponse response = super.index(request);
		Map<String, Serializable> data = extractMetaData(request);
		writeOp("index", request.index(), data);
		return response;
	}

	protected Map<String, Serializable> extractMetaData(IndexRequest request) {
		Map<String, Serializable> data = new HashMap<>();
		data.put("type", request.type());
		data.put("index", request.index());
		data.put("opType", request.opType().name());
		data.put("id", request.id());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			request.source().writeTo(baos);
			data.put("source", new String(baos.toByteArray(), "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}

	protected Map<String, Serializable> extractMetaData(UpdateRequest request) {
		Map<String, Serializable> data = new HashMap<>();
		data.put("type", request.type());
		data.put("index", request.index());
		data.put("opType", request.opType().name());
		data.put("id", request.id());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			request.doc().source().writeTo(baos);
			data.put("source", new String(baos.toByteArray(), "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}

	protected Map<String, Serializable> extractMetaData(DeleteRequest request) {
		Map<String, Serializable> data = new HashMap<>();
		data.put("type", request.type());
		data.put("index", request.index());
		data.put("opType", request.opType().name());
		data.put("id", request.id());
		return data;
	}

}
