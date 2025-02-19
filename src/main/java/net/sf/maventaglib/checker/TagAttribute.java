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
package net.sf.maventaglib.checker;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Javabean representing a tag attribute.
 * @author Fabrizio Giustina
 * @version $Revision: 217 $ ($Author: fgiust $)
 */
public class TagAttribute
    implements Comparable<TagAttribute>
{

    private String name;

    private String type;

    private String description;

    private boolean required;

    private boolean deprecated;

    private boolean rtexprvalue;

    public String getName()
    {
        return this.name;
    }

    public void setName( String attributeName )
    {
        this.name = attributeName;
    }

    public String getType()
    {
        return this.type;
    }

    public void setType( String attributeType )
    {
        this.type = attributeType;
    }

    public boolean isDeprecated()
    {
        return this.deprecated;
    }

    public void setDeprecated( boolean deprecated )
    {
        this.deprecated = deprecated;
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public boolean isRequired()
    {
        return this.required;
    }

    public void setRequired( boolean required )
    {
        this.required = required;
    }

    public boolean isRtexprvalue()
    {
        return this.rtexprvalue;
    }

    public void setRtexprvalue( boolean rtexprvalue )
    {
        this.rtexprvalue = rtexprvalue;
    }

    /**
     * @see java.lang.Comparable#compareTo(Object)
     */
    public int compareTo( TagAttribute object )
    {
        return new CompareToBuilder().append( this.deprecated, object.deprecated ).append( this.name, object.name )
            .toComparison();
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public boolean equals( Object obj )
    {
        if( obj == null )
        {
            return false;
        }

        if( obj == this )
        {
            return true;
        }

        if( obj.getClass() != getClass() )
        {
            return false;
        }

        TagAttribute rhs = (TagAttribute) obj;
        return new EqualsBuilder().append( this.deprecated, rhs.deprecated ).append( this.name, rhs.name )
                .isEquals();
      }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append( this.deprecated ).append( this.name )
                .toHashCode();
      }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE ).append( "name", this.name ) //$NON-NLS-1$
            .append( "description", this.description ).append( "deprecated", this.deprecated ).append( "type", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                                                                                       this.type )
            .append( "required", this.required ).toString(); //$NON-NLS-1$
    }

}