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
package org.openubl.managers;

import org.openubl.keys.KeyProvider;
import org.openubl.models.ComponentProvider;
import org.openubl.models.OrganizationModel;
import org.openubl.models.OrganizationProvider;
import org.openubl.models.OrganizationType;
import org.openubl.models.utils.DefaultKeyProviders;
import org.openubl.models.utils.RepresentationToModel;
import org.openubl.representations.idm.OrganizationRepresentation;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

@Transactional
@ApplicationScoped
public class OrganizationManager {

    @Inject
    ComponentProvider componentProvider;

    @Inject
    DefaultKeyProviders defaultKeyProviders;

    @Inject
    OrganizationProvider organizationProvider;

    public OrganizationModel createOrganization(OrganizationRepresentation representation) {
        OrganizationModel organization = organizationProvider.addOrganization(representation.getName(), OrganizationType.common);
        RepresentationToModel.updateOrganization(representation, organization);

        // Certificate
        if (componentProvider.getComponents(organization, organization.getId(), KeyProvider.class.getName()).isEmpty()) {
            defaultKeyProviders.createProviders(organization);
        }

        return organization;
    }
}
