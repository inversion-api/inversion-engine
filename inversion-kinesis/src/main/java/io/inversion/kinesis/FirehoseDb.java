/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.kinesis;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;
import io.inversion.*;
import io.inversion.json.JSMap;
import io.inversion.rql.Term;
import io.inversion.json.JSNode;
import io.inversion.utils.Utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Posts records to a mapped AWS Kinesis Firehose stream.
 * <p>
 * When you PUT/POST a:
 * <ul>
 * <li>a JSON object - it is submitted as a single record
 * <li>a JSON array - each element in the array is submitted as a single record.
 * </ul>
 * <p>
 * Unless <code>jsonPrettyPrint</code> is set to <code>true</code> all JSON
 * records are stringified without return characters.
 * <p>
 * All records are always submitted in batches of up to <code>batchMax</code>.
 * You can submit more than <code>batchMax</code> to the handler and it will try to
 * send as many batches as required.
 * <p>
 * If <code>jsonSeparator</code> is not null (it is '\n' by default) and the
 * stringified record does not end in <code>separator</code>,
 * <code>separator</code> will be appended to the record.
 * <p>
 * If your firehose is Redshift, you probably want to leave <code>jsonLowercaseNames</code>
 * at its default which is true.  Redshift only matches to lowercase names on COPY.
 * <p>
 * The underlying Firehose stream is mapped to the collection name through
 * the FireshoseDb.includeStreams property.
 */
public class FirehoseDb extends Db<FirehoseDb> {
    /**
     * A CSV of pipe delimited collection name to table name pairs.
     * <p>
     * Example: firehosedb.includeStreams=impression|liftck-player9-impression
     * <p>
     * Or if the collection name is the name as the table name you can just send a the name
     * <p>
     * Example: firehosedb.includeStreams=liftck-player9-impression
     */
    protected String includeStreams = null;

    protected String awsAccessKey = null;
    protected String awsSecretKey = null;
    protected String awsRegion    = null;

    protected AmazonKinesisFirehose firehoseClient = null;

    protected int     batchMax           = 500;
    protected String  jsonSeparator      = "\n";
    protected boolean jsonPrettyPrint    = false;
    protected boolean jsonLowercaseNames = true;

    public FirehoseDb() {
        this.withType("firehose");
    }

    @Override
    protected void doStartup(Api api) {
        if (!Utils.empty(includeStreams)) {
            for (String part : Utils.explode(",", includeStreams)) {
                String[] arr            = part.split("\\|");
                String   collectionName = arr[0];
                String   streamName     = collectionName;
                if (arr.length > 1) {
                    streamName = arr[1];
                }

                Collection coll = new Collection(streamName);
                withCollection(coll);

                if (arr.length == 1)//a specific collection name was not supplied by the config
                    coll.withTableName(beautifyCollectionName(collectionName));
            }
        } else {
            throw ApiException.new500InternalServerError("FirehoseDb must have 'includeStreams' configured to be used");
        }
    }

    @Override
    public Results doSelect(Collection table, List<Term> columnMappedTerms) throws ApiException {
        throw ApiException.new400BadRequest("The Firehose handler only supports PUT/POST operations...GET and DELETE don't make sense.");
    }

    @Override
    public void doDelete(Collection table, List<Map<String, Object>> indexValues) throws ApiException {
        throw ApiException.new400BadRequest("The Firehose handler only supports PUT/POST operations...GET and DELETE don't make sense.");
    }

    @Override
    public List<String> doUpsert(Collection table, List<Map<String, Object>> rows) throws ApiException {
        List<Record> batch = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String string = new JSMap(rows.get(i)).toString(jsonPrettyPrint, jsonLowercaseNames);

            if (jsonSeparator != null && !string.endsWith(jsonSeparator))
                string += jsonSeparator;

            batch.add(new Record().withData(ByteBuffer.wrap(string.getBytes())));

            if (i > 0 && i % batchMax == 0) {
                getFirehoseClient().putRecordBatch(new PutRecordBatchRequest().withDeliveryStreamName(table.getTableName()).withRecords(batch));
                batch.clear();
            }
        }

        if (batch.size() > 0) {
            getFirehoseClient().putRecordBatch(new PutRecordBatchRequest().withDeliveryStreamName(table.getTableName()).withRecords(batch));
        }

        return Collections.emptyList();
    }

    public AmazonKinesisFirehose getFirehoseClient() {
        return getFirehoseClient(awsRegion, awsAccessKey, awsSecretKey);
    }

    public AmazonKinesisFirehose getFirehoseClient(String awsRegion, String awsAccessKey, String awsSecretKey) {
        if (this.firehoseClient == null) {
            synchronized (this) {
                if (this.firehoseClient == null) {
                    AmazonKinesisFirehoseClientBuilder builder = AmazonKinesisFirehoseClientBuilder.standard();

                    if (!Utils.empty(awsRegion))
                        builder.withRegion(awsRegion);

                    if (!Utils.empty(awsAccessKey) && !Utils.empty(awsSecretKey)) {
                        BasicAWSCredentials creds = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
                        builder.withCredentials(new AWSStaticCredentialsProvider(creds));
                    }

                    firehoseClient = builder.build();
                }
            }
        }

        return firehoseClient;
    }

    public FirehoseDb withAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
        return this;
    }

    public FirehoseDb withAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
        return this;
    }

    public FirehoseDb withAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
        return this;
    }

    public FirehoseDb withIncludeStreams(String includeStreams) {
        this.includeStreams = includeStreams;
        return this;
    }

    public FirehoseDb withBatchMax(int batchMax) {
        this.batchMax = batchMax;
        return this;
    }

    public FirehoseDb withJsonSeparator(String jsonSeparator) {
        this.jsonSeparator = jsonSeparator;
        return this;
    }

    public FirehoseDb withJsonPrettyPrint(boolean jsonPrettyPrint) {
        this.jsonPrettyPrint = jsonPrettyPrint;
        return this;
    }

    public FirehoseDb withJsonLowercaseNames(boolean jsonLowercaseNames) {
        this.jsonLowercaseNames = jsonLowercaseNames;
        return this;
    }
}
