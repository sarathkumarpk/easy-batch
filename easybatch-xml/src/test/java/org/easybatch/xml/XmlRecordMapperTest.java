/**
 * The MIT License
 *
 *   Copyright (c) 2019, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
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
package org.easybatch.xml;

import org.easybatch.core.record.Header;
import org.easybatch.core.record.Record;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class XmlRecordMapperTest {

    @Mock
    private Header header;
    private XmlRecord xmlRecord;

    private XmlRecordMapper<Person> xmlRecordMapper;

    @Before
    public void setUp() throws Exception {
        xmlRecordMapper = new XmlRecordMapper<>(Person.class);
    }

    @Test
    public void testValidXmlPersonMapping() throws Exception {
        // given
        xmlRecord = new XmlRecord(header, getXmlFromFile("/person.xml"));

        // when
        Record<Person> mappedRecord = xmlRecordMapper.processRecord(xmlRecord);

        // then
        Person person = mappedRecord.getPayload();
        assertThat(person).isNotNull();
        assertThat(person.getId()).isEqualTo(1);
        assertThat(person.getFirstName()).isEqualTo("foo");
        assertThat(person.getLastName()).isEqualTo("bar");
        assertThat(person.getBirthDate()).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2000-01-01"));
        assertThat(person.isMarried()).isTrue();
    }

    @Test
    public void testEmptyXmlPersonMapping() throws Exception {
        // given
        xmlRecord = new XmlRecord(header, "<person/>");

        // when
        Record<Person> mappedRecord = xmlRecordMapper.processRecord(xmlRecord);

        // then
        Person person = mappedRecord.getPayload();
        assertThat(person.getId()).isEqualTo(0);
        assertThat(person.getFirstName()).isNull();
        assertThat(person.getLastName()).isNull();
        assertThat(person.getBirthDate()).isNull();
        assertThat(person.isMarried()).isFalse();
    }

    @Test
    public void testPartialXmlPersonMapping() throws Exception {
        // given
        xmlRecord = new XmlRecord(header, getXmlFromFile("/person-partial.xml"));

        // when
        Record<Person> mappedRecord = xmlRecordMapper.processRecord(xmlRecord);

        // then
        Person person = mappedRecord.getPayload();
        assertThat(person).isNotNull();
        assertThat(person.getId()).isEqualTo(1);
        assertThat(person.getFirstName()).isEqualTo("foo");
        assertThat(person.getLastName()).isNull();
        assertThat(person.getBirthDate()).isNull();
        assertThat(person.isMarried()).isFalse();
    }

    @Test
    public void testMappingWithEscapedXmlSpecialCharacter() throws Exception {
        // given
        xmlRecord = new XmlRecord(header, "<website name='google' url='http://www.google.com?query=test&amp;sort=asc'/>");
        XmlRecordMapper<Website> xmlRecordMapper = new XmlRecordMapper<>(Website.class);

        // when
        Record<Website> mappedRecord = xmlRecordMapper.processRecord(xmlRecord);

        // then
        Website website = mappedRecord.getPayload();
        assertThat(website).isNotNull();
        assertThat(website.getName()).isEqualTo("google");
        assertThat(website.getUrl()).isEqualTo("http://www.google.com?query=test&sort=asc");

        // given
        xmlRecord = new XmlRecord(header, "<website name='l&apos;equipe' url='http://www.lequipe.fr'/>");

        // when
        mappedRecord = xmlRecordMapper.processRecord(xmlRecord);

        // then
        website = mappedRecord.getPayload();
        assertThat(website).isNotNull();
        assertThat(website.getName()).isEqualTo("l'equipe");
        assertThat(website.getUrl()).isEqualTo("http://www.lequipe.fr");
    }

    @Test(expected = Exception.class)
    public void testMappingWithUnescapedXmlSpecialCharacter() throws Exception {
        // given
        xmlRecord = new XmlRecord(header, "<website name='google' url='http://www.google.com?query=test&sort=asc'/>");
        XmlRecordMapper<Website> xmlRecordMapper = new XmlRecordMapper<>(Website.class);

        // when
        xmlRecordMapper.processRecord(xmlRecord);

        // then
        // expected exception
    }

    @Test(expected = Exception.class)
    public void testInvalidXmlPersonMapping() throws Exception {
        // given
        xmlRecord = new XmlRecord(header, getXmlFromFile("/person-invalid.xml"));

        // when
        xmlRecordMapper.processRecord(xmlRecord);

        // then
        // expected exception
    }

    @Test(expected = Exception.class)
    public void testMappingOfInvalidXmlAccordingToXsd() throws Exception {
        // given
        xmlRecord = new XmlRecord(header, getXmlFromFile("/person-invalid-xsd.xml"));
        xmlRecordMapper = new XmlRecordMapper<>(Person.class, getFile("/person.xsd"));

        // when
        xmlRecordMapper.processRecord(xmlRecord);

        // then
        // expected exception
    }

    private File getFile(String fileName) {
        return new File(this.getClass().getResource(fileName).getFile());
    }

    private String getXmlFromFile(String file) {
        return new Scanner(this.getClass().getResourceAsStream(file)).useDelimiter("\\A").next();
    }

}
