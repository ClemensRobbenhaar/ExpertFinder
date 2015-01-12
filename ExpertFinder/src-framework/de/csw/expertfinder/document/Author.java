/*******************************************************************************
 * This file is part of the Corporate Semantic Web Project at Freie Universitaet Berlin.
 * 
 * This work has been partially supported by the ``InnoProfile-Corporate Semantic Web" project funded by the German Federal
 * Ministry of Education and Research (BMBF) and the BMBF Innovation Initiative for the New German Laender - Entrepreneurial Regions.
 * 
 * http://www.corporate-semantic-web.de/
 * 
 * Freie Universitaet Berlin
 * Copyright (c) 2007-2013
 * 
 * Institut fuer Informatik
 * Working Group Corporate Semantic Web
 * Koenigin-Luise-Strasse 24-26
 * 14195 Berlin
 * 
 * http://www.mi.fu-berlin.de/en/inf/groups/ag-csw/
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA or see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package de.csw.expertfinder.document;

/**
 * This class represents a document's (or better: a document version's)
 * author.
 * @author ralph
 */
public class Author extends PersistableEntity<Long> {
	private String name;
	private String location;
	
	private boolean isBot;
	private boolean isAnonymous;
	
	/**
	 * Constructs an empty Author (used by persistence framework).
	 */
	Author() {
	}
	
	/**
	 * Constructs an author with name.
	 * @param name the author's name
	 */
	public Author(String name) {
		this.name = name;
	}
	
	/**
	 * @return this author's user name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets this author's user name.
	 * @param name the author's user name.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return this author's location (where he/she lives and/or works).
	 */
	public String getLocation() {
		return location;
	}
	
	/**
	 * Sets this author's location.
	 * @param location
	 */
	public void setLocation(String location) {
		this.location = location;
	}
	
	/**
	 * @see de.csw.expertfinder.document.PersistableEntity#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Author))
			return false;
		
		return this.name.equals(((Author)obj).name);
	}
	
	
	
	/**
	 * @return the isBot
	 */
	public boolean isBot() {
		return isBot;
	}

	/**
	 * @param isBot the isBot to set
	 */
	public void setBot(boolean isBot) {
		this.isBot = isBot;
	}

	/**
	 * @return the isAnonymous
	 */
	public boolean isAnonymous() {
		return isAnonymous;
	}

	/**
	 * @param isAnonymous the isAnonymous to set
	 */
	public void setAnonymous(boolean isAnonymous) {
		this.isAnonymous = isAnonymous;
	}

	/**
	 * @see javade.csw.expertfinder.document.PersistableEntity#hashCode()
	 */
	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
