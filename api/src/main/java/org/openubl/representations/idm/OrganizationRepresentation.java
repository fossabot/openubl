/**
 * Copyright 2019 Project OpenUBL, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Eclipse Public License - v 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openubl.representations.idm;

public class OrganizationRepresentation {

    private String id;
    private String name;
    private String description;

    private String type;
    private Boolean useMasterKeys;

    public OrganizationRepresentation() {

    }

    public OrganizationRepresentation(OrganizationRepresentation rep) {
        this.id = rep.getId();
        this.name = rep.getName();
        this.description = rep.getDescription();
        this.type = rep.getType();
        this.useMasterKeys = rep.getUseMasterKeys();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getUseMasterKeys() {
        return useMasterKeys;
    }

    public void setUseMasterKeys(Boolean useMasterKeys) {
        this.useMasterKeys = useMasterKeys;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
