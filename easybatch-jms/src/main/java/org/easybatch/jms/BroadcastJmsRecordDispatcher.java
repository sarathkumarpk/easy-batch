/*
 *  The MIT License
 *
 *   Copyright (c) 2015, Mahmoud Ben Hassine (mahmoud@benhassine.fr)
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

package org.easybatch.jms;

import org.easybatch.core.dispatcher.AbstractRecordDispatcher;
import org.easybatch.core.dispatcher.RecordDispatchingException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueSender;
import java.util.List;

import static java.lang.String.format;

/**
 * Broadcast records to a list of Jms queues.
 *
 * @author Mahmoud Ben Hassine (mahmoud@benhassine.fr)
 */
public class BroadcastJmsRecordDispatcher extends AbstractRecordDispatcher<Message> {

    /**
     * List of queues to which records should be dispatched.
     */
    private List<QueueSender> queues;

    /**
     * Create a {@link BroadcastJmsRecordDispatcher} instance.
     *
     * @param queues the list of queues to which records should be dispatched
     */
    public BroadcastJmsRecordDispatcher(List<QueueSender> queues) {
        this.queues = queues;
    }

    @Override
    public void dispatchRecord(final Message record) throws RecordDispatchingException {
        for (QueueSender queue : queues) {
            try {
                queue.send(record);
            } catch (JMSException e) {
                String message = format("Unable to dispatch Jms record %s to queue %s", record, queue);
                throw new RecordDispatchingException(message, e);
            }
        }
    }

}
