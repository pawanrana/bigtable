package com.altamiracorp.bigtable.model.accumulo;

import com.altamiracorp.bigtable.model.Row;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;

import java.io.IOException;

public class AccumuloModelOutputFormat extends OutputFormat<Text, Row> {
    private AccumuloOutputFormat accumuloOutputFormat = new AccumuloOutputFormat();

    public static void init(Job job, String accumuloInstanceName, String zookeeperServers, String user, String password, String tableName) throws AccumuloSecurityException {
        ClientConfiguration clientConfig = new ClientConfiguration()
                .withInstance(accumuloInstanceName)
                .withZkHosts(zookeeperServers);
        AccumuloOutputFormat.setZooKeeperInstance(job, clientConfig);
        AuthenticationToken passwordToken = new PasswordToken(password);
        AccumuloOutputFormat.setConnectorInfo(job, user, passwordToken);
        AccumuloOutputFormat.setDefaultTableName(job, tableName);
    }

    @Override
    public RecordWriter<Text, Row> getRecordWriter(TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        return new RowRecordWriter(accumuloOutputFormat.getRecordWriter(taskAttemptContext));
    }

    @Override
    public void checkOutputSpecs(JobContext jobContext) throws IOException, InterruptedException {
        accumuloOutputFormat.checkOutputSpecs(jobContext);
    }

    @Override
    public OutputCommitter getOutputCommitter(TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        return accumuloOutputFormat.getOutputCommitter(taskAttemptContext);
    }

    private class RowRecordWriter extends RecordWriter<Text, Row> {
        private final RecordWriter<Text, Mutation> recordWriter;

        public RowRecordWriter(RecordWriter<Text, Mutation> recordWriter) {
            this.recordWriter = recordWriter;
        }

        @Override
        public void write(Text text, Row row) throws IOException, InterruptedException {
            Mutation mutation = AccumuloSession.createMutationFromRow(row);
            if (mutation != null) {
                this.recordWriter.write(text, mutation);
            }
        }

        @Override
        public void close(TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
            this.recordWriter.close(taskAttemptContext);
        }
    }
}
