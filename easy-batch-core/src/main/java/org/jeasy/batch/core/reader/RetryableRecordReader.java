/*
 * The MIT License
 *
 *   Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 */
package org.jeasy.batch.core.reader;

import org.jeasy.batch.core.retry.RetryPolicy;
import org.jeasy.batch.core.retry.RetryTemplate;
import org.jeasy.batch.core.record.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Decorator that makes a {@link RecordReader} retryable whenever the data source is temporarily unavailable.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 * @param <P> type of the record's payload
 */
public class RetryableRecordReader<P> implements RecordReader<P> {

    private RecordReader<P> delegate;
    private RecordReadingCallable<P> recordReadingCallable;
    private RecordReadingTemplate recordReadingTemplate;

    /**
     * Create a new {@link RetryableRecordReader}.
     *
     * @param delegate record reader
     * @param retryPolicy to apply
     */
    public RetryableRecordReader(RecordReader<P> delegate, RetryPolicy retryPolicy) {
        this.delegate = delegate;
        this.recordReadingCallable = new RecordReadingCallable(delegate);
        this.recordReadingTemplate = new RecordReadingTemplate(retryPolicy);
    }

    @Override
    public void open() throws Exception {
        delegate.open();
    }

    @Override
    public Record<P> readRecord() throws Exception {
        return recordReadingTemplate.execute(recordReadingCallable);
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }

    private static class RecordReadingCallable<P> implements Callable<Record<P>> {

        private RecordReader<P> recordReader;

        RecordReadingCallable(RecordReader<P> recordReader) {
            this.recordReader = recordReader;
        }

        @Override
        public Record<P> call() throws Exception {
            return recordReader.readRecord();
        }

    }

    private static class RecordReadingTemplate extends RetryTemplate {

        private final Logger LOGGER = LoggerFactory.getLogger(RecordReadingTemplate.class.getName());

        RecordReadingTemplate(RetryPolicy retryPolicy) {
            super(retryPolicy);
        }

        @Override
        protected void beforeCall() {
            // no op
        }

        @Override
        protected void afterCall(Object result) {
            // no op
        }

        @Override
        protected void onException(Exception e) {
            LOGGER.error("Unable to read next record", e);
        }

        @Override
        protected void onMaxAttempts(Exception e) {
            LOGGER.error("Unable to read next record after {} attempt(s)", retryPolicy.getMaxAttempts());
        }

        @Override
        protected void beforeWait() {
            LOGGER.debug("Waiting for {} {} before retrying to read next record", retryPolicy.getDelay(), retryPolicy.getTimeUnit());
        }

        @Override
        protected void afterWait() {
            // no op
        }

    }
}
