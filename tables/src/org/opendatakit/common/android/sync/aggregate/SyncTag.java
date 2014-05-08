/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.common.android.sync.aggregate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.opendatakit.common.android.utilities.ODKFileUtils;

public class SyncTag {

  private String dataETag;
  private String schemaETag;

  /**
   * Make a copy of the given syncTag
   *
   * @param syncTag
   */
  public SyncTag(SyncTag syncTag) {
    this.dataETag = syncTag.dataETag;
    this.schemaETag = syncTag.schemaETag;
  }

  /**
   * Construct a sync tag with the given parameters
   *
   * @param dataETag
   * @param schemaETag
   */
  public SyncTag(String dataETag, String schemaETag) {
    this.dataETag = dataETag;
    this.schemaETag = schemaETag;
  }

  public String getDataETag() {
    return dataETag;
  }

  public String getSchemaETag() {
    return schemaETag;
  }

  public void setSchemaETag(String schemaETag) {
    this.schemaETag = schemaETag;
  }

  public void setDataETag(String dataETag) {
    this.dataETag = dataETag;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dataETag == null) ? 1 : dataETag.hashCode());
    result = prime * result + ((schemaETag == null) ? 1 : schemaETag.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SyncTag))
      return false;
    SyncTag other = (SyncTag) obj;
    boolean sameDataTag =  dataETag == null ?
        other.dataETag == null : dataETag.equals(other.dataETag);
    boolean sameSchemaTag = schemaETag == null ?
        other.schemaETag == null :
          schemaETag.equals(other.schemaETag);
    return sameDataTag && sameSchemaTag;
  }

  @Override
  public String toString() {
    HashMap<String,String> map = new HashMap<String,String>();
    map.put("dataETag", dataETag);
    map.put("schemaETag", schemaETag);
    try {
      return ODKFileUtils.mapper.writeValueAsString(map);
    } catch (JsonGenerationException e) {
      e.printStackTrace();
      throw new IllegalStateException("failed conversion");
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalStateException("failed conversion");
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException("failed conversion");
    }
  }

  @SuppressWarnings("unchecked")
  public static SyncTag valueOf(String syncTag) {
    if ( syncTag == null || syncTag.length() == 0 ) {
      return new SyncTag(null, null);
    }
    HashMap<String, String> map;
    try {
      map = (HashMap<String, String>) ODKFileUtils.mapper.readValue(syncTag, Map.class);
    } catch (JsonParseException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("failed conversion: " + syncTag);
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("failed conversion: " + syncTag);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("failed conversion: " + syncTag);
    }

    String dataETag = map.get("dataETag");
    String schemaETag = map.get("schemaETag");
    return new SyncTag(dataETag, schemaETag);
  }
}