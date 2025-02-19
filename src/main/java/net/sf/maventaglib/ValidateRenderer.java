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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.logging.Log;

import net.sf.maventaglib.checker.ELFunction;
import net.sf.maventaglib.checker.Tag;
import net.sf.maventaglib.checker.TagAttribute;
import net.sf.maventaglib.checker.Tld;


/**
 * Validates tag handler classes fount in tlds.
 * @author Fabrizio Giustina
 * @version $Revision $ ($Author $)
 */
public class ValidateRenderer extends AbstractMavenTaglibReportRenderer
{

    private static final int ICO_SUCCESS = 0;

    private static final int ICO_INFO = 1;

    private static final int ICO_WARNING = 2;

    private static final int ICO_ERROR = 3;

    private static final String IMAGE_ERROR_SRC = Messages.getString("Validate.image.error"); //$NON-NLS-1$

    private static final String IMAGE_WARNING_SRC = Messages.getString("Validate.image.warning"); //$NON-NLS-1$

    private static final String IMAGE_INFO_SRC = Messages.getString("Validate.image.info"); //$NON-NLS-1$

    private static final String IMAGE_SUCCESS_SRC = Messages.getString("Validate.image.success"); //$NON-NLS-1$

    /**
     * list of Tld to check.
     */
    private Tld[] tlds;

    private Log log;

    private ClassLoader projectClassLoader;

    /**
     * javax.servlet.jsp.tagext.TagSupport class loaded using the project classloader.
     */
    private Class<?> tagSupportClass;

    /**
     * javax.servlet.jsp.tagext.TagExtraInfo class loaded using the project classloader.
     */
    private Class<?> tagExtraInfoClass;

    /**
     * javax.servlet.jsp.tagext.SimpleTag class loaded using the project classloader.
     */
    private Class<?> simpleTagClass;

    /**
     * Class-Constructor
     *
     * @param sink the sink to use.
     * @param locale the wanted locale to return the report's description, could be <code>null</code>.
     * @param tlds list of TLDs to check.
     * @param log the logger that has been injected into this mojo.
     * @param projectClassLoader ClassLoader for all compile-classpaths
     */
    public ValidateRenderer(Sink sink, Locale locale, Tld[] tlds, Log log, ClassLoader projectClassLoader)
    {
        super(sink, locale);
        this.tlds = tlds;
        this.log = log;
        this.projectClassLoader = projectClassLoader;

        try
        {
            tagSupportClass = Class.forName("javax.servlet.jsp.tagext.TagSupport", true, this.projectClassLoader); //$NON-NLS-1$
        }
        catch (ClassNotFoundException e)
        {
            log.error(Messages.getString("Validate.error.unabletoload.TagSupport")); //$NON-NLS-1$
        }
        try
        {
            tagExtraInfoClass = Class.forName("javax.servlet.jsp.tagext.TagExtraInfo", true, this.projectClassLoader); //$NON-NLS-1$
        }
        catch (ClassNotFoundException e)
        {
            log.error(Messages.getString("Validate.error.unabletoload.TagExtraInfo")); //$NON-NLS-1$
        }
        try
        {
            simpleTagClass = Class.forName("javax.servlet.jsp.tagext.SimpleTag", true, this.projectClassLoader); //$NON-NLS-1$
        }
        catch (ClassNotFoundException e)
        {
            log.debug(Messages.getString("Validate.error.unabletoload.SimpleTag")); //$NON-NLS-1$
        }

    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReportRenderer#getTitle()
     */
    @Override
    public String getTitle()
    {
        return getMessageString("Validate.title"); //$NON-NLS-1$
    }

    /**
     * Check the given tld. Assure that:
     * <ul>
     * <li>Any tag class is loadable</li>
     * <li>the tag class has a setter for any of the declared attribute</li>
     * <li>the type declared in the dtd for an attribute (if any) matches the type accepted by the getter</li>
     * </ul>
     * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
     */
    @Override
    protected void renderBody()
    {
        sink.body();
        startSection(getMessageString("Validate.h1")); //$NON-NLS-1$
        paragraph(getMessageString("Validate.into1")); //$NON-NLS-1$
        paragraph(getMessageString("Validate.intro2")); //$NON-NLS-1$

        sink.list();
        for (Tld tld : tlds)
        {

            sink.listItem();
            sink.link("#" + tld.getFilename()); //$NON-NLS-1$
            sink.text(MessageFormat.format(getMessageString("Validate.listitem.tld"), //$NON-NLS-1$
                StringUtils.defaultIfEmpty(tld.getName(), tld.getShortname()), tld.getFilename() ));
            sink.link_();
            sink.text(getMessageString("Validate.listitem.uri") + tld.getUri()); //$NON-NLS-1$

            sink.listItem_();
        }
        sink.list_();

        endSection();

        for (Tld tld : tlds)
        {
            checkTld(tld);
        }

        sink.body_();
    }

    /**
     * Checks a single tld and returns validation results.
     * @param tld Tld
     */
    private void checkTld(Tld tld)
    {
        // new section for each tld
        sink.anchor(tld.getFilename());
        sink.anchor_();
        startSection(StringUtils.defaultIfEmpty(tld.getName(), tld.getShortname()) + " " + tld.getFilename()); //$NON-NLS-1$

        doTags(tld.getTags(), tld.getShortname());
        doFunctions(tld.getFunctions(), tld.getShortname());

        endSection();
    }

    /**
     * @param tld
     */
    private void doTags(Tag[] tags, String shortname)
    {
        if (tags != null && tags.length > 0)
        {
            for (Tag tldItem : tags)
            {
                checkTag(shortname, tldItem);
            }
        }
    }

    private void doFunctions(ELFunction[] tags, String shortname)
    {
        if (tags != null && tags.length > 0)
        {

            startSection("EL functions");

            startTable();

            tableHeader(new String[]{
                getMessageString("Validate.header.validated"), "function", getMessageString("Validate.header.class"), getMessageString("Validate.header.signature") }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

            for (ELFunction tldItem : tags)
            {
                checkFunction(shortname, tldItem);
            }

            endTable();

            endSection();
        }
    }

    /**
     * @param shortname
     * @param tldItem
     */
    private void checkFunction(String prefix, ELFunction tag)
    {

        String className = tag.getFunctionClass();

        boolean found = true;

        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.projectClassLoader);

        try
        {
            Class<?> functionClass = Class.forName(className, true, this.projectClassLoader);

            String fullSignature = tag.getFunctionSignature();
            String paramsString = tag.getParameters();
            String returnvalue = null;

            String methodName = StringUtils.trim(StringUtils.substringBefore(fullSignature, "("));
            if (StringUtils.contains(methodName, " "))
            {
                returnvalue = StringUtils.substringBefore(methodName, " ");
                methodName = StringUtils.substringAfter(methodName, " ");
            }

            String[] params = StringUtils.split(paramsString, ",");

            List<Class<?>> parClasses = new ArrayList<>(params.length);

            for (String stringClass : params)
            {
                parClasses.add(Class.forName(StringUtils.trim(stringClass), true, this.projectClassLoader));
            }

            Method method = functionClass.getMethod(methodName, parClasses.toArray(new Class<?>[0]));

            Class< ? > returnType = method.getReturnType();

            if (!(returnvalue == null || returnType.getCanonicalName().equals(returnvalue)))
            {
                found = false;
            }
        }
        catch (Exception e)
        {
            found = false;
        }

        Thread.currentThread().setContextClassLoader(currentClassLoader);

        sink.tableRow();

        sink.tableCell();
        figure(found ? ICO_SUCCESS : ICO_ERROR);
        sink.tableCell_();

        tableCell(prefix + ":" + tag.getName() + "()");
        tableCell(className);
        tableCell(tag.getFunctionSignature());

        sink.tableRow_();

    }

    /**
     * Checks a single tag and returns validation results.
     * @param tag Tag
     */
    private void checkTag(String prefix, Tag tag)
    {

        // new subsection for each tag
        startSection("<" + prefix + ":" + tag.getName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        String className = tag.getTagClass();

        startTable();

        tableHeader(new String[]{
            getMessageString("Validate.header.found"), getMessageString("Validate.header.loadable"), getMessageString("Validate.header.extends"), getMessageString("Validate.header.class") }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        boolean found = true;
        boolean loadable = true;
        boolean extend = true;

        Object tagObject = null;
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.projectClassLoader);

        try
        {
            Class<?> tagClass = Class.forName(className, true, this.projectClassLoader);

            // extend only true, if tagClass derives from TagSupport or derives from SimpleTag
            extend = tagSupportClass.isAssignableFrom(tagClass)
                || simpleTagClass != null && simpleTagClass.isAssignableFrom(tagClass);

            try
            {
                tagObject = tagClass.getDeclaredConstructor().newInstance();
            }
            catch (Exception e)
            {
                loadable = false;
            }

        }
        catch (ClassNotFoundException | NoClassDefFoundError e)
        {
            found = false;
            loadable = false;
            extend = false;
        }

        Thread.currentThread().setContextClassLoader(currentClassLoader);

        TagAttribute[] attributes = tag.getAttributes();

        sink.tableRow();

        sink.tableCell();
        figure(found ? ICO_SUCCESS : ICO_ERROR);
        sink.tableCell_();

        sink.tableCell();
        figure(loadable ? ICO_SUCCESS : ICO_ERROR);
        sink.tableCell_();

        sink.tableCell();
        figure(extend ? ICO_SUCCESS : ICO_ERROR);
        sink.tableCell_();

        tableCell(className);

        sink.tableRow_();

        if (tag.getTeiClass() != null)
        {
            checkTeiClass(tag.getTeiClass());
        }

        endTable();

        if (tagObject != null && attributes.length > 0)
        {

            startTable();
            tableHeader(new String[]{
                StringUtils.EMPTY,
                getMessageString("Validate.header.attributename"), getMessageString("Validate.header.tlddeclares"), getMessageString("Validate.header.tagdeclares") }); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

            for (TagAttribute attribute : attributes)
            {
                checkAttribute(tagObject, attribute);
            }

            endTable();
        }
        endSection();
    }

    /**
     * Check a declared TagExtraInfo class.
     * @param className TEI class name
     */
    private void checkTeiClass(String className)
    {

        boolean found = true;
        boolean loadable = true;
        boolean extend = true;

        Class<?> teiClass = null;
        try
        {
            teiClass = Class.forName(className, true, this.projectClassLoader);

            if (tagExtraInfoClass == null || !tagExtraInfoClass.isAssignableFrom(teiClass))
            {
                extend = false;
            }

            try
            {
                teiClass.getDeclaredConstructor().newInstance();
            }
            catch (Exception e)
            {
                loadable = false;
            }
        }
        catch (ClassNotFoundException | NoClassDefFoundError e)
        {
            found = false;
            loadable = false;
            extend = false;
        }

        sink.tableRow();

        sink.tableCell();
        figure(found ? ICO_SUCCESS : ICO_ERROR);
        sink.tableCell_();

        sink.tableCell();
        figure(loadable ? ICO_SUCCESS : ICO_ERROR);
        sink.tableCell_();

        sink.tableCell();
        figure(extend ? ICO_SUCCESS : ICO_ERROR);
        sink.tableCell_();

        sink.tableCell();
        sink.text(className);
        sink.tableCell_();

        sink.tableRow_();
    }

    /**
     * Checks a single attribute and returns validation results.
     * @param tag tag handler instance
     * @param attribute TagAttribute
     */
    private void checkAttribute(Object tag, TagAttribute attribute)
    {

        String tldType = attribute.getType();
        String tldName = attribute.getName();
        Class<?> tagType = null;
        String tagTypeName = null;

        List<ValidationError> validationErrors = new ArrayList<>(3);

        if (!PropertyUtils.isWriteable(tag, tldName))
        {
            validationErrors.add(new ValidationError(ValidationError.LEVEL_ERROR,
                getMessageString("Validate.error.setternotfound"))); //$NON-NLS-1$
        }

        // don't check if setter is missing
        if (validationErrors.isEmpty())
        {

            try
            {
                tagType = PropertyUtils.getPropertyType(tag, tldName);
            }
            catch (Exception e)
            {
                // should never happen, since we already checked the writable property
                log.warn(e);
            }
            tagTypeName = tagType == null ? StringUtils.EMPTY : tagType.getName();

            if (tldType != null && tagType != null)
            {
                Class<?> tldTypeClass = getClassFromName(tldType);

                if (!tagType.isAssignableFrom(tldTypeClass))
                {

                    validationErrors.add(new ValidationError(ValidationError.LEVEL_ERROR, MessageFormat.format(
                        getMessageString("Validate.error.attributetypemismatch"), //$NON-NLS-1$
                        tldType, tagType.getName() )));
                }
            }
        }

        // don't check if we already know type is different
        if (validationErrors.isEmpty())
        {

            if (tldType != null && tagType != null && !tldType.equals(tagType.getName()))
            {
                validationErrors.add(new ValidationError(ValidationError.LEVEL_WARNING, MessageFormat.format(
                    getMessageString("Validate.error.attributetypeinexactmatch"), //$NON-NLS-1$
                    tldType, tagType.getName() )));
            }
            else if (tldType == null && !String.class.equals(tagType))
            {
                validationErrors.add(new ValidationError(ValidationError.LEVEL_INFO,
                    getMessageString("Validate.error.attributetype"))); //$NON-NLS-1$
            }
        }

        sink.tableRow();

        sink.tableCell();

        int figure = ICO_SUCCESS;

        for (ValidationError error : validationErrors)
        {
            if (error.getLevel() == ValidationError.LEVEL_ERROR)
            {
                figure = ICO_ERROR;
            }
            else if (figure == ICO_SUCCESS) // warning
            {
                figure = ICO_WARNING;
            }

        }

        figure(figure);
        sink.tableCell_();

        sink.tableCell();
        sink.text(tldName);

        for (ValidationError error : validationErrors)
        {
            sink.lineBreak();
            if (error.getLevel() == ValidationError.LEVEL_ERROR)
            {
                sink.bold();
            }
            sink.text(error.getText());
            if (error.getLevel() == ValidationError.LEVEL_ERROR)
            {
                sink.bold_();

            }
        }

        sink.tableCell_();

        sink.tableCell();
        if (tldType != null)
        {
            sink.text(StringUtils.substringAfter(tldType, "java.lang.")); //$NON-NLS-1$
        }
        sink.tableCell_();

        tableCell(StringUtils.substringAfter(tagTypeName, "java.lang.")); //$NON-NLS-1$

        sink.tableRow_();

    }

    private void figure(int type)
    {
        String text;
        String src;

        switch (type)
        {
            case ICO_ERROR :
                text = getMessageString("Validate.level.error"); //$NON-NLS-1$
                src = IMAGE_ERROR_SRC;
                break;
            case ICO_WARNING :
                text = getMessageString("Validate.level.warning"); //$NON-NLS-1$
                src = IMAGE_WARNING_SRC;
                break;
            case ICO_INFO :
                text = getMessageString("Validate.level.info"); //$NON-NLS-1$
                src = IMAGE_INFO_SRC;
                break;
            default :
                text = getMessageString("Validate.level.success"); //$NON-NLS-1$
                src = IMAGE_SUCCESS_SRC;
                break;
        }

        sink.figure();
        sink.figureGraphics(src);
        sink.figureCaption();
        sink.text(text);
        sink.figureCaption_();
        sink.figure_();
    }

    /**
     * returns a class from its name, handling primitives.
     * @param className clss name
     * @return Class istantiated using Class.forName or the matching primitive.
     */
    private Class<?> getClassFromName(String className)
    {

        Class<?> tldTypeClass = tryGettingPrimitiveClass(className);

        if (tldTypeClass == null)
        {
            // not a primitive type
            try
            {
                if (isArrayClassName(className))
                {
                    tldTypeClass = getArrayClass(className);
                }
                else
                {
                    tldTypeClass = Class.forName(className, true, this.projectClassLoader);
                }
            }
            catch (ClassNotFoundException e)
            {
                log.error(MessageFormat.format(Messages.getString("Validate.error.unabletofindclass"), //$NON-NLS-1$
                    className ));
            }
        }
        return tldTypeClass;
    }

    private Class<?> tryGettingPrimitiveClass(String className)
    {
        if ("int".equals(className)) //$NON-NLS-1$
        {
            return int.class;
        }
        if ("long".equals(className)) //$NON-NLS-1$
        {
            return long.class;
        }
        if ("double".equals(className)) //$NON-NLS-1$
        {
            return double.class;
        }
        if ("boolean".equals(className)) //$NON-NLS-1$
        {
            return boolean.class;
        }
        if ("char".equals(className)) //$NON-NLS-1$
        {
            return char.class;
        }
        if ("byte".equals(className)) //$NON-NLS-1$
        {
            return byte.class;
        }

        return null;
    }

    private boolean isArrayClassName(String className)
    {
        return className.endsWith("[]");
    }

    private Class<?> getArrayClass(String className) throws ClassNotFoundException
    {
        String elementClassName = StringUtils.replace(className, "[]", "");
        Class<?> elementClass = tryGettingPrimitiveClass(elementClassName);
        if (elementClass == null)
        {
            elementClass = Class.forName(elementClassName);
        }
        return Array.newInstance(elementClass, 0).getClass();
    }

    static class ValidationError
    {

        public static final int LEVEL_INFO = 1;

        public static final int LEVEL_WARNING = 2;

        public static final int LEVEL_ERROR = 3;

        private int level;

        private String text;

        /**
         * @param level
         * @param text
         */
        public ValidationError(int level, String text)
        {
            this.level = level;
            this.text = text;
        }

        /**
         * Getter for <code>level</code>.
         * @return Returns the level.
         */
        public int getLevel()
        {
            return this.level;
        }

        /**
         * Setter for <code>level</code>.
         * @param level The level to set.
         */
        public void setLevel(int level)
        {
            this.level = level;
        }

        /**
         * Getter for <code>text</code>.
         * @return Returns the text.
         */
        public String getText()
        {
            return this.text;
        }

        /**
         * Setter for <code>text</code>.
         * @param text The text to set.
         */
        public void setText(String text)
        {
            this.text = text;
        }
    }

}