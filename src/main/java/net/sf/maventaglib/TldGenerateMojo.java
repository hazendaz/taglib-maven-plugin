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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import net.sf.maventaglib.util.XmlHelper;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.jdom2.input.DOMBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.tlddoc.tagfileparser.Attribute;
import com.sun.tlddoc.tagfileparser.Directive;
import com.sun.tlddoc.tagfileparser.javacc.ParseException;
import com.sun.tlddoc.tagfileparser.javacc.TagFile;


/**
 * Generates tld files from directories of jsp 2.0 tag files.
 * @author Fabrizio Giustina
 * @version $Id: TldGenerateMojo.java 217 2014-08-15 20:50:32Z fgiust $
 */
@Mojo(name="tldgenerate", defaultPhase=LifecyclePhase.GENERATE_RESOURCES)
public class TldGenerateMojo extends AbstractMojo
{

    /**
     * Directory containing tag files. Subdirectories are also processed.
     */
    @Parameter(defaultValue="src/main/resources/META-INF/tags/")
    private File tagDir;

    /**
     * Output dir for tld files.
     */
    @Parameter(defaultValue="${project.build.outputDirectory}/META-INF", required=true)
    private File outputDir;

    /**
     * Version added to tld files, defaults to project version.
     */
    @Parameter(property="project.version", required=true)
    private String version;

    /**
     * Detailed configuration for taglibs for tld generation. Starting with version 2.4 you can configure multiple
     * taglibs with this attribute, and for each taglib you can add both tagfiles dir than classes with EL functions
     * (note that EL function support is preliminary, the resulting tld does not include anything in the "description"
     * attribute (that would require parsing javadocs from sources.
     * 
     * <pre>
     *    &lt;taglibs>
     *      &lt;taglib>
     *        &lt;description>A test tld that contains functions&lt;/description>
     *        &lt;shortName>test&lt;/shortName>
     *        &lt;uri>testuri&lt;/uri>
     *        &lt;outputname>testtaglib&lt;/outputname>
     *        &lt;functionClasses>
     *          &lt;functionClass>org.apache.commons.lang.StringUtils&lt;/functionClass>
     *        &lt;/functionClasses>
     *        &lt;tagdir>src/tagfiles&lt;/tagdir>
     *      &lt;/taglib>
     *    &lt;/taglibs>
     * </pre>
     */
    @Parameter
    private List<Taglib> taglibs;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        getLog().debug(MessageFormat.format(Messages.getString("Taglib.generating.tld"), //$NON-NLS-1$
            tagDir.getAbsolutePath() ));

        try
        {
            generateTlds();
        }
        catch (IOException e)
        {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }

    /**
     * @throws IOException
     * @throws MojoExecutionException
     */
    private void generateTlds() throws IOException, MojoExecutionException
    {

        List<Taglib> taglibsList = new ArrayList<>();

        if (taglibs != null)
        {
            for (Taglib taglib : taglibs)
            {
                // a drink to get the old-style embedding tests work,
                // I should migrate to the new IT test infrastructure
                Taglib tlib = new Taglib();
                try
                {
                    PropertyUtils.copyProperties(tlib, taglib);
                }
                catch (Exception e)
                {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
                taglibsList.add(tlib);
            }
        }

        if (taglibsList.isEmpty())
        {
            // old (pre 2.4) behavior, create taglib configurations from dir
            if (tagDir.isDirectory())
            {
                // handle tag files. Add any directory containing .tag or .tagx files
                List<File> tags = FileUtils.getFiles(tagDir, "**/*.tag", null); //$NON-NLS-1$
                tags.addAll(FileUtils.getFiles(tagDir, "**/*.tagx", null)); //$NON-NLS-1$

                if (!tags.isEmpty())
                {
                    Set<File> directories = new HashSet<>();
                    for (File tag : tags)
                    {
                        directories.add(tag.getParentFile());
                    }

                    for (File dir : directories)
                    {
                        Taglib tlib = new Taglib();
                        tlib.setTagdir(dir);
                        tlib.setShortName(dir.getName());
                        tlib.setOutputname(dir.getName() + ".tld");
                        tlib.setUri(dir.getName());
                        tlib.setShortName("Tag library for tag file directory " + dir.getName());
                        taglibsList.add(tlib);
                    }
                }
                else
                {
                    getLog().warn(MessageFormat.format(Messages.getString("Taglib.generating.notfound"), //$NON-NLS-1$
                        tagDir.getAbsolutePath() ));
                }
            }
        }

        try
        {
            for (Taglib taglib : taglibsList)
            {
                doTaglib(taglib);
            }
        }
        catch (Exception e)
        {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * @param taglib
     * @throws MojoExecutionException
     * @throws IOException
     */
    private void doTaglib(Taglib taglib) throws MojoExecutionException, IOException
    {
        Document doc = getTLDDocument(taglib, XmlHelper.getDocumentBuilder());

        if (taglib.getShortName() == null)
        {
            throw new MojoExecutionException("Missing \"shortName\" parameter for taglib " + taglib);
        }

        String tldName = taglib.getOutputname();
        if (tldName == null)
        {
            tldName = taglib.getShortName() + ".tld";
        }
        if (!tldName.endsWith(".tld"))
        {
            tldName = tldName + ".tld";
        }

        File outputFile = new File(outputDir, tldName);
        if (!(outputDir.mkdirs() || outputDir.isDirectory())) {
            throw new IOException("Unable to create output directory " + outputDir.getAbsolutePath());
        }

        getLog().info(MessageFormat.format(Messages.getString("Taglib.generating.file"), //$NON-NLS-1$
            tldName, taglib.getShortName() ));

        try (FileOutputStream fos = new FileOutputStream(outputFile)) 
        {
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            outputter.output(new DOMBuilder().build(doc), fos);
        }
    }

    protected Document getTLDDocument(Taglib taglib, DocumentBuilder documentBuilder)

    {
        Document result = documentBuilder.newDocument();

        Element taglibElement = createRootTaglibNode(result, taglib.getDescription(), taglib.getShortName(), taglib
            .getUri());

        if (taglib.getTagdir() != null && taglib.getTagdir().isDirectory())
        {

            String path = taglib.getTagdir().getAbsolutePath().replace(File.separatorChar, '/');
            int index = path.indexOf("/META-INF/");
            if (index != -1)
            {
                path = path.substring(index);
            }
            else
            {
                path = "unknown";
            }
            if (!path.endsWith("/"))
            {
                path += "/";
            }

            File[] files = taglib.getTagdir().listFiles();
            if (files == null)
            {
                files = new File[0];
            }
            for (File tag : files)
            {
                if (!tag.isDirectory()
                    && (tag.getName().toLowerCase().endsWith(".tag") || tag.getName().toLowerCase().endsWith(".tagx")))
                {
                    String tagName = tag.getName().substring(0, tag.getName().lastIndexOf('.'));
                    String tagPath = path + tag.getName();

                    Element tagFileElement = result.createElement("tag-file");
                    Element nameElement = result.createElement("name");
                    nameElement.appendChild(result.createTextNode(tagName));
                    tagFileElement.appendChild(nameElement);
                    Element pathElement = result.createElement("path");
                    pathElement.appendChild(result.createTextNode(tagPath));
                    tagFileElement.appendChild(pathElement);
                    taglibElement.appendChild(tagFileElement);

                    // tag-file Subelements
                    //
                    // description (optional) A description of the tag.
                    // display-name (optional) Name intended to be displayed by tools.
                    // icon (optional) Icon that can be used by tools.
                    // name The unique tag name.
                    // path Where to find the tag file implementing this tag, relative to the root of the web
                    // application or
                    // the root
                    // of the JAR file for a tag library packaged in a JAR. This must begin with /WEB-INF/tags/ if the
                    // tag
                    // file resides
                    // in the WAR, or /META-INF/tags/ if the tag file resides in a JAR.
                    // example (optional) Informal description of an example use of the tag.
                    // tag-extension (optional) Extensions that provide extra information about the tag for tools.

                    try (InputStream is = new FileInputStream(tag))
                    {
                        TagFile tagFile = TagFile.parse(is);

                        for (Directive directive : tagFile.getDirectives())
                        {
                            if ("tag".equals(directive.getDirectiveName()))
                            {
                                for (Attribute attribute : directive.getAttributes())
                                {
                                    if ("description".equals(attribute.getName())
                                        || "display-name".equals(attribute.getName())
                                        || "example".equals(attribute.getName()))
                                    {
                                        Element element = result.createElement(attribute.getName());
                                        element.appendChild(result.createTextNode(attribute.getValue()));
                                        tagFileElement.appendChild(element);
                                    }

                                }

                            }

                        }

                    }
                    catch (IOException | ParseException e)
                    {
                        getLog().error(e);
                    }
                }

            }
        }

        Map<String, Integer> duplicateFunctions = new HashMap<>();

        if (taglib.getFunctionClasses() != null)
        {
            for (String functionClassString : taglib.getFunctionClasses())
            {
                Class<?> functionClass = null;
                try
                {
                    functionClass = Class.forName(functionClassString);

                }
                catch (Exception e)
                {
                    getLog().error(
                        "Unable to load function class "
                            + functionClassString
                            + ": "
                            + e.getClass().getName()
                            + " "
                            + e.getMessage(),
                        e);
                    continue;
                }
                Method[] declaredMethods = functionClass.getDeclaredMethods();

                for (Method method : declaredMethods)
                {
                    if (!Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers()))
                    {
                        // not a public static method
                        continue;
                    }

                    Element tagFileElement = result.createElement("function");

                    String functionName = method.getName();

                    if (duplicateFunctions.containsKey(functionName))
                    {
                        int currentNum = duplicateFunctions.get(functionName);
                        duplicateFunctions.put(functionName, currentNum + 1);
                        functionName = functionName + currentNum;
                    }
                    else
                    {
                        duplicateFunctions.put(functionName, 1);
                    }

                    Element nameElement = result.createElement("name");
                    nameElement.appendChild(result.createTextNode(functionName));
                    tagFileElement.appendChild(nameElement);

                    Element classElement = result.createElement("function-class");
                    classElement.appendChild(result.createTextNode(method.getDeclaringClass().getName()));
                    tagFileElement.appendChild(classElement);

                    StringBuilder parameterTypesString = new StringBuilder();
                    Class< ? >[] parameterTypes = method.getParameterTypes();

                    for (int p = 0; p < parameterTypes.length; p++)
                    {
                        Class< ? > param = parameterTypes[p];
                        parameterTypesString.append(nonPrimitiveName(param.getCanonicalName()));
                        if (parameterTypes.length - 1 > p)
                        {
                            parameterTypesString.append(", ");
                        }
                    }

                    Element signatureElement = result.createElement("function-signature");
                    signatureElement.appendChild(result.createTextNode(nonPrimitiveName(method
                        .getReturnType()
                        .getCanonicalName())
                        + " "
                        + method.getName()
                        + "("
                        + parameterTypesString.toString()
                        + ")"));
                    tagFileElement.appendChild(signatureElement);

                    taglibElement.appendChild(tagFileElement);

                }

            }
        }

        return result;
    }

    private String nonPrimitiveName(String string)
    {
        if ("int".equals(string))
        {
            return Integer.class.getName();
        }
        else if ("boolean".equals(string))
        {
            return Boolean.class.getName();
        }
        else if ("double".equals(string))
        {
            return Double.class.getName();
        }
        else if ("long".equals(string))
        {
            return Long.class.getName();
        }
        else if ("char".equals(string))
        {
            return Character.class.getName();
        }

        return string;
    }

    protected Element createRootTaglibNode(Document result, String description, String shortName, String uri)
    {
        Element taglibElement = result.createElementNS("http://java.sun.com/xml/ns/javaee", "taglib");
        taglibElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "http://java.sun.com/xml/ns/javaee");
        taglibElement.setAttributeNS(
            "http://www.w3.org/2000/xmlns/",
            "xmlns:xsi",
            "http://www.w3.org/2001/XMLSchema-instance");
        taglibElement.setAttributeNS(
            "http://www.w3.org/2001/XMLSchema-instance",
            "xsi:schemaLocation",
            "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-jsptaglibrary_2_1.xsd");
        taglibElement.setAttribute("version", "2.1");
        result.appendChild(taglibElement);

        Element descriptionElement = result.createElement("description");
        descriptionElement.appendChild(result.createTextNode(description));
        taglibElement.appendChild(descriptionElement);

        Element tlibVersionElement = result.createElement("tlib-version");
        tlibVersionElement.appendChild(result.createTextNode(version));
        taglibElement.appendChild(tlibVersionElement);

        Element shortNameElement = result.createElement("short-name");
        shortNameElement.appendChild(result.createTextNode(shortName));
        taglibElement.appendChild(shortNameElement);

        Element uriElement = result.createElement("uri");
        uriElement.appendChild(result.createTextNode(uri));
        taglibElement.appendChild(uriElement);

        return taglibElement;
    }
}