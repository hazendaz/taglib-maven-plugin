/*
 * The MIT License
 * Copyright © 2004-2014 Fabrizio Giustina
 * Copyright © 2022-2024 Web-Legacy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.sf.maventaglib;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.doxia.module.xhtml5.Xhtml5Parser;
import org.apache.maven.doxia.parser.ParseException;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.util.HtmlTools;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.reporting.AbstractMavenReportRenderer;

import net.sf.maventaglib.checker.ELFunction;
import net.sf.maventaglib.checker.Tag;
import net.sf.maventaglib.checker.TagAttribute;
import net.sf.maventaglib.checker.TagFile;
import net.sf.maventaglib.checker.TagVariable;
import net.sf.maventaglib.checker.Tld;
import net.sf.maventaglib.checker.TldItem;


/**
 * Generates a tag reference xdoc that can be integrated in a maven generated site.
 *
 * @author Fabrizio Giustina
 * @version $Revision $ ($Author $)
 */
public class TagreferenceRenderer extends AbstractMavenTaglibReportRenderer
{

    private static final String OPEN_DIV = "<div>";

    private static final String CLOSE_DIV = "</div>";

    /**
     * list of Tld to check.
     */
    private Tld[] tlds;

    /**
     * {@code true} to parse html in the description of tld info, tags and attributes.
     */
    private boolean parseHtml = true;

    /**
     * the logger that has been injected into this mojo.
     */
    private Log log;

    /**
     * Class-Constructor
     *
     * @param sink the sink to use.
     * @param locale the wanted locale to return the report's description, could be <code>null</code>.
     * @param tlds list of TLDs to check.
     * @param parseHtml {@code true} to parse html in the description of tld info, tags and attributes.
     * @param log the logger that has been injected into this mojo.
     */
    public TagreferenceRenderer(Sink sink, Locale locale, Tld[] tlds, boolean parseHtml, Log log)
    {
        super(sink, locale);

        this.tlds = tlds;
        this.parseHtml = parseHtml;
        this.log = log;

    }

    /**
     * @see AbstractMavenReportRenderer#getTitle()
     */
    @Override
    public String getTitle()
    {
        return getMessageString("Tagreference.title"); //$NON-NLS-1$
    }

    /**
     * @see AbstractMavenReportRenderer#renderBody()
     */
    @Override
    protected void renderBody()
    {
        sink.body();

        startSection(getMessageString("Tagreference.h1")); //$NON-NLS-1$
        paragraph(getMessageString("Tagreference.intro")); //$NON-NLS-1$

        sink.list();
        for (Tld tld : tlds)
        {
            log.debug("Rendering " + tld.getFilename());
            sink.listItem();
            sink.link("#" + tld.getFilename()); //$NON-NLS-1$
            sink.text(MessageFormat.format(getMessageString("Tagreference.listitem.tld"), //$NON-NLS-1$
                StringUtils.defaultIfEmpty(tld.getName(), tld.getShortname()), tld.getFilename()));
            sink.link_();
            sink.text(getMessageString("Tagreference.listitem.uri") + tld.getUri()); //$NON-NLS-1$
            sink.listItem_();
        }
        sink.list_();

        endSection();

        for (Tld tld : tlds)
        {
            doTld(tld);
            sink.pageBreak();
        }

        sink.body_();
    }

    private String stripTags(String html)
    {
        if (html == null)
        {
            return StringUtils.EMPTY;
        }
        return html.replaceAll("\\<.*?\\>", StringUtils.EMPTY); //$NON-NLS-1$
    }

    /**
     * @param tld Tld
     */
    private void doTld(Tld tld)
    {
        // new section for each tld
        sink.anchor(tld.getFilename());
        sink.anchor_();

        startSection(StringUtils.defaultIfEmpty(tld.getName(), tld.getShortname())
            + " - "
            + MessageFormat.format(getMessageString("Tagreference.tldversion"), tld.getTlibversion()));

        sink.paragraph();
        if (parseHtml)
        {
            parseHtml(tld.getInfo());
        }
        else
        {
            sink.text(tld.getInfo());
        }
        sink.paragraph_();

        sink.paragraph();
        sink.bold();
        sink.text("Namespace definition:");
        sink.bold_();
        sink.text(" xmlns:");
        sink.text(tld.getShortname());
        sink.text("=\"");
        sink.text(tld.getUri());
        sink.text("\"");
        sink.paragraph_();

        TldItem[] tags = tld.getTags();
        ELFunction[] functions = tld.getFunctions();
        TagFile[] tagfiles = tld.getTagfiles();

        printList(tld, tags, "Tags");
        printList(tld, functions, "EL Functions");
        printList(tld, tagfiles, "Tagfiles");

        sink.paragraph();
        sink.text(getMessageString("Tagreference.intro.required") + ' '); //$NON-NLS-1$
        sink.bold();
        sink.text(getMessageString("Tagreference.required.marker")); //$NON-NLS-1$
        sink.bold_();
        sink.paragraph_();

        if (tags != null)
        {
            for (TldItem tag : tags)
            {
                doTag(tld.getShortname(), (Tag) tag);
            }
        }

        if (functions != null)
        {
            for (ELFunction function : functions)
            {
                doFunction(tld.getShortname(), function);
            }
        }
        if (tagfiles != null)
        {
            for (TagFile tagfile : tagfiles)
            {
                doTagFile(tld.getShortname(), tagfile);
            }
        }

        endSection();
    }

    /**
     * @param shortname
     * @param elFunction
     */
    private void doFunction(String prefix, ELFunction tag)
    {

        sink.anchor(prefix + ":" + tag.getName());
        sink.anchor_();

        // new subsection for each tag
        startSection(prefix + ":" + tag.getName() + "(" + tag.getParameters() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        sink.paragraph();
        sink.bold();
        sink.text("Function class: ");
        sink.bold_();
        sink.text(tag.getFunctionClass());
        sink.paragraph_();
        sink.paragraph();
        sink.bold();
        sink.text("Function signature: ");
        sink.bold_();
        sink.text(tag.getFunctionSignature());
        sink.paragraph_();

        if (parseHtml)
        {
            parseHtml(tag.getDescription());
        }
        else
        {
            sink.paragraph();
            sink.text(tag.getDescription());
            sink.paragraph_();
        }

        if (StringUtils.isNotEmpty(tag.getExample()))
        {
            startSection(getMessageString("Tagreference.example")); //$NON-NLS-1$
            verbatimText(tag.getExample());
            endSection();
        }

        endSection();
    }

    /**
     * @param tld
     * @param tags
     */
    private void printList(Tld tld, TldItem[] tags, String intro)
    {
        if (tags != null && tags.length > 0)
        {

            sink.paragraph();
            sink.bold();
            sink.text(intro);
            sink.bold_();
            sink.paragraph_();

            sink.list();

            for (TldItem tag : tags)
            {
                sink.listItem();

                sink.link("#" + tld.getShortname() + ":" + tag.getName()); //$NON-NLS-1$
                if (tag.isDeprecated())
                {
                    sink.italic();
                }
                sink.text(tag.getName());
                if (tag instanceof ELFunction)
                {
                    sink.text("()");
                }

                if (tag.isDeprecated())
                {
                    sink.italic_();
                }
                sink.link_();

                sink.text(" "); //$NON-NLS-1$

                String cleanedDescription = stripTags(StringUtils.substringBefore(tag.getDescription(), ".")) + '.'; //$NON-NLS-1$
                if (parseHtml)
                {
                    cleanedDescription = HtmlTools.unescapeHTML(cleanedDescription);
                }
                sink.text(cleanedDescription);

                sink.listItem_();
            }

            sink.list_();
        }
    }

    /**
     * Checks a single tag and returns validation results.
     * @param tag Tag
     */
    private void doTag(String prefix, Tag tag)
    {
        sink.anchor(prefix + ":" + tag.getName());
        sink.anchor_();

        // new subsection for each tag
        startSection("<" + prefix + ":" + tag.getName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (parseHtml)
        {
            parseHtml(tag.getDescription());
        }
        else
        {
            sink.paragraph();
            sink.text(tag.getDescription());
            sink.paragraph_();
        }

        sink.paragraph();
        sink.text(getMessageString("Tagreference.cancontain") + ' '); //$NON-NLS-1$
        sink.text(tag.getBodycontent());
        sink.paragraph_();

        if (StringUtils.isNotEmpty(tag.getExample()))
        {
            startSection(getMessageString("Tagreference.example")); //$NON-NLS-1$
            verbatimText(tag.getExample());
            endSection();
        }

        // variables

        // attributes
        TagAttribute[] attributes = tag.getAttributes();

        if (attributes != null && attributes.length > 0)
        {
            startSection(getMessageString("Tagreference.attributes")); //$NON-NLS-1$

            startTable();
            tableHeader(new String[]{
                  getMessageString("Tagreference.attribute.name"), getMessageString("Tagreference.attribute.description"), getMessageString("Tagreference.attribute.type")}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            for (TagAttribute attribute : attributes)
            {
                sink.tableRow();

                sink.tableCell();
                if (attribute.isDeprecated())
                {
                    sink.italic();
                }
                if (attribute.isRequired())
                {
                    sink.bold();
                }
                sink.text(attribute.getName());
                if (attribute.isRequired())
                {
                    sink.text(getMessageString("Tagreference.required.marker")); //$NON-NLS-1$
                    sink.bold_();
                }
                if (attribute.isDeprecated())
                {
                    sink.italic_();
                }
                sink.tableCell_();

                sink.tableCell();
                if (attribute.isDeprecated() || StringUtils.isBlank(attribute.getDescription()))
                {
                    sink.italic();
                }

                if (StringUtils.isBlank(attribute.getDescription()))
                {
                    sink.text(getMessageString("Tagreference.required.marker"));
                }
                else if (parseHtml)
                {
                    parseHtml(attribute.getDescription());
                }
                else
                {
                    sink.text(attribute.getDescription());
                }
                if (attribute.isDeprecated() || StringUtils.isBlank(attribute.getDescription()))
                {
                    sink.italic_();
                }
                sink.tableCell_();

                tableCell(StringUtils.defaultIfEmpty(
                    StringUtils.substringBefore(attribute.getType(), "java.lang."), "String")); //$NON-NLS-1$ //$NON-NLS-2$

                sink.tableRow_();

            }

            endTable();

            endSection();
        }
        else
        {
            paragraph(getMessageString("Tagreference.noattributes")); //$NON-NLS-1$
        }

        // attributes
        TagVariable[] variables = tag.getVariables();

        if (variables != null && variables.length > 0)
        {
            startSection(getMessageString("Tagreference.variables")); //$NON-NLS-1$

            startTable();
            tableHeader(new String[]{
                getMessageString("Tagreference.variable.name"), getMessageString("Tagreference.variable.type"), getMessageString("Tagreference.variable.scope"), getMessageString("Tagreference.variable.description")}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

            for (TagVariable variable : variables)
            {
                sink.tableRow();

                sink.tableCell();
                if (variable.isDeprecated())
                {
                    sink.italic();
                }

                if (variable.getNameGiven() != null)
                {
                    sink.text(variable.getNameGiven());
                    sink.text(getMessageString("Tagreference.variable.constant")); //$NON-NLS-1$
                }
                else
                {
                    sink.text(getMessageString("Tagreference.variable.specifiedvia") + ' '); //$NON-NLS-1$
                    sink.text(variable.getNameFromAttribute());
                }

                if (variable.isDeprecated())
                {
                    sink.italic_();
                }
                sink.tableCell_();

                tableCell(StringUtils.defaultIfEmpty(StringUtils.substringBefore(variable.getType(), "java.lang."), //$NON-NLS-1$
                    "String")); //$NON-NLS-1$

                tableCell(variable.getScope());

                sink.tableCell();
                if (variable.isDeprecated())
                {
                    sink.italic();
                }
                if (parseHtml)
                {
                    parseHtml(variable.getDescription());
                }
                else
                {
                    sink.text(variable.getDescription());
                }
                if (variable.isDeprecated())
                {
                    sink.italic_();
                }
                sink.tableCell_();

                sink.tableRow_();

            }

            endTable();

            endSection();
        }

        endSection();

    }

    /**
     * Checks a single tag and returns validation results.
     * @param tag Tag
     */
    private void doTagFile(String prefix, TagFile tag)
    {
        sink.anchor(prefix + ":" + tag.getName());
        sink.anchor_();

        // new subsection for each tag
        startSection("<" + prefix + ":" + tag.getName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (parseHtml)
        {
            parseHtml(tag.getDescription());
        }
        else
        {
            sink.paragraph();
            sink.text(tag.getDescription());
            sink.paragraph_();
        }

        if (StringUtils.isNotEmpty(tag.getExample()))
        {
            startSection(getMessageString("Tagreference.example")); //$NON-NLS-1$
            verbatimText(tag.getExample());
            endSection();
        }

        endSection();

    }

    private void parseHtml(String description)
    {
        try
        {
            new Xhtml5Parser().parse(new StringReader(OPEN_DIV + description + CLOSE_DIV), sink);
        }
        catch (ParseException e)
        {
            log.error(description, e);
        }
    }

}