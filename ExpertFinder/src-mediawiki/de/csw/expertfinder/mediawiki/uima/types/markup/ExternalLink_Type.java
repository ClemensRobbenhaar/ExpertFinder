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
/* First created by JCasGen Wed Apr 15 16:11:42 CEST 2009 */
package de.csw.expertfinder.mediawiki.uima.types.markup;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** An external link.
 * Updated by JCasGen Fri Sep 25 16:45:43 CEST 2009
 * @generated */
public class ExternalLink_Type extends Annotation_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (ExternalLink_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = ExternalLink_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new ExternalLink(addr, ExternalLink_Type.this);
  			   ExternalLink_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new ExternalLink(addr, ExternalLink_Type.this);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = ExternalLink.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.csw.expertfinder.mediawiki.uima.types.markup.ExternalLink");
 
  /** @generated */
  final Feature casFeat_destination;
  /** @generated */
  final int     casFeatCode_destination;
  /** @generated */ 
  public String getDestination(int addr) {
        if (featOkTst && casFeat_destination == null)
      jcas.throwFeatMissing("destination", "de.csw.expertfinder.mediawiki.uima.types.markup.ExternalLink");
    return ll_cas.ll_getStringValue(addr, casFeatCode_destination);
  }
  /** @generated */    
  public void setDestination(int addr, String v) {
        if (featOkTst && casFeat_destination == null)
      jcas.throwFeatMissing("destination", "de.csw.expertfinder.mediawiki.uima.types.markup.ExternalLink");
    ll_cas.ll_setStringValue(addr, casFeatCode_destination, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public ExternalLink_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_destination = jcas.getRequiredFeatureDE(casType, "destination", "uima.cas.String", featOkTst);
    casFeatCode_destination  = (null == casFeat_destination) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_destination).getCode();

  }
}



    
