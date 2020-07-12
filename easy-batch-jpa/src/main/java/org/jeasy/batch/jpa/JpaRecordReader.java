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
package org.jeasy.batch.jpa;

import org.jeasy.batch.core.reader.RecordReader;
import org.jeasy.batch.core.record.GenericRecord;
import org.jeasy.batch.core.record.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

import static org.jeasy.batch.core.util.Utils.checkArgument;
import static org.jeasy.batch.core.util.Utils.checkNotNull;

/**
 * Read records using the Java Persistence API. This reader reads records in
 * pages. You can set the maximum number of records to read for each page
 * using {@link #setMaxResults(int)}.
 *
 * This reader produces {@link GenericRecord} instances with JPA entities as payload.
 *
 * @param <T> the type of objects this reader will read.
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class JpaRecordReader<T> implements RecordReader<T> {

    public static final int DEFAULT_MAX_RESULT = 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaRecordReader.class.getSimpleName());

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private String query;
    private TypedQuery<T> typedQuery;
    private Class<T> type;
    private List<T> records;
    private Iterator<T> iterator;
    private int offset;
    private int maxResults;
    private long currentRecordNumber;

    /**
     * Reader that reads data using the Java Persistence API.
     *
     * This reader produces {@link GenericRecord} instances with JPA entities as payload.
     *
     * @param entityManagerFactory the entity manager factory
     * @param query                the JPQL query to fetch data
     * @param type                 the target type
     */
    public JpaRecordReader(final EntityManagerFactory entityManagerFactory, final String query, final Class<T> type) {
        checkNotNull(entityManagerFactory, "entity manager factory");
        checkNotNull(query, "query");
        checkNotNull(type, "target type");
        this.entityManagerFactory = entityManagerFactory;
        this.query = query;
        this.type = type;
        this.maxResults = DEFAULT_MAX_RESULT;
    }

    @Override
    public void open() {
        currentRecordNumber = 0;
        offset = 0;
        LOGGER.debug("Creating a JPA entity manager");
        entityManager = entityManagerFactory.createEntityManager();
        typedQuery = entityManager.createQuery(query, type);
        typedQuery.setFirstResult(offset);
        typedQuery.setMaxResults(maxResults);
        records = typedQuery.getResultList();
        iterator = records.iterator();
    }

    private boolean hasNextRecord() {
        if (!iterator.hasNext()) {
            typedQuery.setFirstResult(offset += records.size());
            records = typedQuery.getResultList();
            iterator = records.iterator();
        }
        return iterator.hasNext();
    }

    @Override
    public GenericRecord<T> readRecord() {
        Header header = new Header(++currentRecordNumber, getDataSourceName(), LocalDateTime.now());
        if (hasNextRecord()) {
            return new GenericRecord<>(header, iterator.next());
        } else {
            return null;
        }
    }

    private String getDataSourceName() {
        return "Result of JPA query: " + query;
    }

    @Override
    public void close() {
        LOGGER.debug("Closing JPA entity manager");
        entityManager.close();
    }

    /**
     * Set maximum number of records to fetch for each query.
     *
     * @param maxResults the maximum number of records to fetch for each query
     */
    public void setMaxResults(final int maxResults) {
        checkArgument(maxResults >= 1, "max results parameter must be >= 1");
        this.maxResults = maxResults;
    }

}
