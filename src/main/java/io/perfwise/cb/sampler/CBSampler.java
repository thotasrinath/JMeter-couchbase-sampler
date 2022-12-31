package io.perfwise.cb.sampler;

import com.couchbase.client.core.error.CouchbaseException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.query.QueryResult;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.util.ConfigMergabilityIndicator;
import org.apache.jmeter.gui.Searchable;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;

import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.couchbase.client.java.query.QueryOptions.queryOptions;


public class CBSampler extends AbstractTestElement implements Sampler, TestBean, ConfigMergabilityIndicator, TestStateListener, TestElement, Serializable, Searchable {

	private static final long serialVersionUID = 9112846706008433268L;
	private static Logger LOGGER = LoggerFactory.getLogger(CBSampler.class);

	private static final Set<String> APPLIABLE_CONFIG_CLASSES = new HashSet<>(
			Arrays.asList("org.apache.jmeter.config.gui.SimpleConfigGui"));

	private Cluster clusterObject;
	private Bucket bucket;
	private Scope scopeObject;
	private Collection collectionObject;
	private String bucketObject;
	private String scope;
	private String collection;
	private String queryTypeValue;
	private String query;
	private String parameters;
	private final int operationInt = CBSamplerBeanInfo.getQueryTypeValueAsInt(getQueryTypeValue());

	@Override
	public SampleResult sample(Entry e) {

		if (operationInt == 0 && this.clusterObject == null) {
			this.clusterObject = (Cluster) JMeterContextService.getContext().getVariables()
					.getObject("clusterObject");
			LOGGER.info("Cluster object ::: " + clusterObject);
		}
		if (operationInt > 0 && this.bucket == null) {
			this.bucket = (Bucket) JMeterContextService.getContext().getVariables()
					.getObject(getBucketObject());
			LOGGER.info("Bucket object ::: " + bucket);
			scopeObject = bucket.scope(getScope());
			collectionObject = (Collection) scopeObject.collection(getCollection());
		}

		SampleResult result = new SampleResult();
		result.setSampleLabel(getName());
		result.setSamplerData(requestBody());
		result.setDataType(SampleResult.TEXT);
		result.setContentType("text/plain");
		result.setDataEncoding(StandardCharsets.UTF_8.name());
		//Starting the measurement
		result.sampleStart();
		if(operationInt == 0){
			QueryResult res = this.queryOperations(getQuery());
			result.setResponseData(result.toString(), StandardCharsets.UTF_8.name());
		}else{
			MutationResult res = this.dataOperations(operationInt, getQuery());
			result.setResponseData(result.toString(), StandardCharsets.UTF_8.name());
		}
		result.sampleEnd();
		return result;
	}

	private String requestBody() {
		return null;
	}

	private SampleResult handleException(SampleResult result, Exception ex) {
		result.setResponseMessage("Message Publish Error");
		result.setResponseCode("500");
		result.setResponseData(
				String.format("Error in publishing message to PubSub topic : %s", ex.toString()).getBytes());
		result.setSuccessful(false);
		return result;
	}

	@Override
	public void testStarted() {
		int queryTypeInt = CBSamplerBeanInfo.getQueryTypeValueAsInt(getQueryTypeValue());
	}

	@Override
	public void testStarted(String host) {
	}

	@Override
	public void testEnded() {
	}

	@Override
	public void testEnded(String host) {
	}

	@Override
	public boolean applies(ConfigTestElement configElement) {
		String guiClass = configElement.getProperty(TestElement.GUI_CLASS).getStringValue();
		return APPLIABLE_CONFIG_CLASSES.contains(guiClass);
	}

	public MutationResult dataOperations(int type, String data){
		JsonObject content = JsonObject.fromJson(data);
		MutationResult result = null;

		switch (type){
			case CBSamplerBeanInfo.INSERT:
				try{
//                    result = collectionObject.insert(key, json);
				}catch (DocumentNotFoundException ex){
					LOGGER.info("Document not found");
				}
				break;
			case CBSamplerBeanInfo.GET:
				GetResult getResult = collectionObject.get(content.getString("key"));
//                result = getResult.contentAsObject();
				break;
			case CBSamplerBeanInfo.UPSERT:
//                result = collectionObject.upsert(key, json);
				break;
			case CBSamplerBeanInfo.REMOVE:
				try{
					collectionObject.remove(content.getString("key"));
				}catch (DocumentNotFoundException dnfe){
					LOGGER.error("Document not found exception " + dnfe);
				}
				break;
			default:
				LOGGER.info("Invalid operation Selected - Please check the sampler operation selection");
				LOGGER.info("Aborting Test..");
                testEnded();
		}
		return result;
	}

	public QueryResult queryOperations(String data){
		QueryResult result = null;
		try{
			result = clusterObject.query(data, queryOptions().metrics(true));
		}catch (CouchbaseException ce){
			LOGGER.info("Couchbase exception occurred while w=executing N1ql query ");
			ce.printStackTrace();
		}
		return result;
	}

	//Getters & Setters
	public Cluster getClusterObject() {
		return clusterObject;
	}

	public void setClusterObject(Cluster clusterObject) {
		this.clusterObject = clusterObject;
	}

	public String getBucketObject() {
		return bucketObject;
	}

	public void setBucketObject(String bucketObject) {
		this.bucketObject = bucketObject;
	}

	public Scope getScopeObject() {
		return scopeObject;
	}

	public void setScopeObject(Scope scopeObject) {
		this.scopeObject = scopeObject;
	}

	public Collection getCollectionObject() {
		return collectionObject;
	}

	public void setCollectionObject(Collection collectionObject) {
		this.collectionObject = collectionObject;
	}

	public Bucket getBucket() {
		return bucket;
	}

	public void setBucket(Bucket bucket) {
		this.bucket = bucket;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getCollection() {
		return collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	public String getQueryTypeValue() {
		return queryTypeValue;
	}

	public void setQueryTypeValue(String queryTypeValue) {
		this.queryTypeValue = queryTypeValue;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}
}



//		if (isGzipCompression()) {
//			byteMsg = createEventCompressed(getMessage());
//		} else {
//			byteMsg = ByteString.copyFromUtf8(getMessage()).toByteArray();
//		}
//		result.sampleStart();
//				try {
//			template = createPubsubMessage(byteMsg, attributes);
//			publish(template, result);
//				} catch (Exception ex) {
//				LOGGER.info("Exception occurred while publishing message");
//				result = handleException(result, ex);
//				} finally {
//				result.sampleEnd();
//				}
//		if (this.bucket == null) {
//			this.bucket = (Bucket) JMeterContextService.getContext().getVariables().getObject(getBucketObject());
//		}
