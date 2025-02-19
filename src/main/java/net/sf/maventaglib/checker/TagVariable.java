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
public class TagVariable
    implements Comparable<TagVariable>
{
    private String nameGiven;

    private String nameFromAttribute;

    private String type;

    private String scope;

    private String description;

    private boolean deprecated;

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getNameGiven()
    {
        return this.nameGiven;
    }

    public void setNameGiven( String name )
    {
        this.nameGiven = name;
    }

    public String getScope()
    {
        return this.scope;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    public String getType()
    {
        return this.type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String getNameFromAttribute()
    {
        return this.nameFromAttribute;
    }

    public void setNameFromAttribute( String nameFromAttribute )
    {
        this.nameFromAttribute = nameFromAttribute;
    }

    public boolean isDeprecated()
    {
        return this.deprecated;
    }

    public void setDeprecated( boolean deprecated )
    {
        this.deprecated = deprecated;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE ).append( "nameGiven", this.nameGiven ) //$NON-NLS-1$
            .append( "description", this.description ).append( "deprecated", this.deprecated ).append( "type", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                                                                                       this.type )
            .append( "scope", this.scope ).append( "nameFromAttribute", this.nameFromAttribute ).toString(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * @see java.lang.Comparable#compareTo(Object)
     */
    public int compareTo( TagVariable object )
    {
        return new CompareToBuilder().append( this.deprecated, object.deprecated ).append( this.nameGiven,
                                                                                           object.nameGiven )
            .append( this.nameFromAttribute, object.nameFromAttribute ).toComparison();
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

        TagVariable rhs = (TagVariable) obj;
        return new EqualsBuilder().append( this.deprecated, rhs.deprecated ).append( this.nameGiven,
                                                                                     rhs.nameGiven )
            .append( this.nameFromAttribute, rhs.nameFromAttribute ).isEquals();
      }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append( this.deprecated ).append( this.nameGiven )
            .append( this.nameFromAttribute ).toHashCode();
      }

}