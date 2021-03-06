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
package org.openubl.resources;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.openubl.keys.component.ComponentModel;
import org.openubl.keys.component.ComponentValidationException;
import org.openubl.keys.component.utils.ComponentUtil;
import org.openubl.managers.OrganizationManager;
import org.openubl.models.*;
import org.openubl.models.utils.ModelToRepresentation;
import org.openubl.models.utils.RepresentationToModel;
import org.openubl.representations.idm.OrganizationRepresentation;
import org.openubl.representations.idm.SearchResultsRepresentation;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Transactional
@ApplicationScoped
@Path("organizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "organization")
public class OrganizationsResource {

    private static final Logger logger = Logger.getLogger(OrganizationsResource.class);

    @Context
    UriInfo uriInfo;

    @Inject
    KeyManager keystore;

    @Inject
    ComponentUtil componentUtil;

    @Inject
    ComponentProvider componentProvider;

    @Inject
    OrganizationManager organizationManager;

    @Inject
    OrganizationProvider organizationProvider;

    @POST
    @Path("/")
    public OrganizationRepresentation createOrganization(@Valid OrganizationRepresentation representation) {
        if (organizationProvider.getOrganizationByName(representation.getName()).isPresent()) {
            throw new BadRequestException("Organization with name=" + representation.getName() + " already exists");
        }
        OrganizationModel organization = organizationManager.createOrganization(representation);
        return ModelToRepresentation.toRepresentation(organization, true);
    }

    @GET
    @Path("/")
    public List<OrganizationRepresentation> getOrganizations(
            @QueryParam("organizationId") String organizationId,
            @QueryParam("filterText") String filterText,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("10") int limit
    ) {
        if (organizationId != null) {
            return organizationProvider.getOrganizationById(organizationId)
                    .map(organizationModel -> Collections.singletonList(ModelToRepresentation.toRepresentation(organizationModel, true)))
                    .orElseGet(Collections::emptyList);
        }

        if (filterText != null) {
            return organizationProvider.getOrganizations(filterText, offset, limit)
                    .stream()
                    .map(model -> ModelToRepresentation.toRepresentation(model, true))
                    .collect(Collectors.toList());
        } else {
            return organizationProvider.getOrganizations(offset, limit)
                    .stream()
                    .map(model -> ModelToRepresentation.toRepresentation(model, true))
                    .collect(Collectors.toList());
        }
    }

    @GET
    @Path("/search")
    public SearchResultsRepresentation<OrganizationRepresentation> searchOrganizations(
            @QueryParam("filterText") String filterText,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("10") int pageSize
    ) {
        SearchResultsModel<OrganizationModel> results;
        if (filterText != null && !filterText.trim().isEmpty()) {
            results = organizationProvider.searchOrganizations(filterText, page, pageSize);
        } else {
            results = organizationProvider.searchOrganizations(page, pageSize);
        }

        return new SearchResultsRepresentation<>(
                results.getTotalSize(),
                results.getModels().stream()
                        .map(model -> ModelToRepresentation.toRepresentation(model, true))
                        .collect(Collectors.toList())
        );
    }

    @GET
    @Path("/all")
    public List<OrganizationRepresentation> getAllOrganizations() {
        return organizationProvider.getOrganizations(-1, -1)
                .stream()
                .map(model -> ModelToRepresentation.toRepresentation(model, true))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/id-by-name/{organizationName}")
    public String getOrganizationIdByName(
            @PathParam("organizationName") String organizationName
    ) {
        return organizationProvider.getOrganizationByName(organizationName)
                .map(OrganizationModel::getId)
                .orElse(null);
    }

    @GET
    @Path("/{organizationId}")
    public OrganizationRepresentation getOrganization(
            @PathParam("organizationId") String organizationId
    ) {
        return organizationProvider.getOrganizationById(organizationId)
                .map(organizationModel -> ModelToRepresentation.toRepresentation(organizationModel, true))
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    @PUT
    @Path("/{organizationId}")
    public OrganizationRepresentation updateOrganization(
            @PathParam("organizationId") String organizationId,
            OrganizationRepresentation rep
    ) {
        OrganizationModel organization = organizationProvider.getOrganizationById(organizationId).orElseThrow(() -> new NotFoundException("Organization not found"));
        RepresentationToModel.updateOrganization(rep, organization);
        return ModelToRepresentation.toRepresentation(organization, true);
    }

    @DELETE
    @Path("/{organizationId}")
    public void deleteOrganization(
            @PathParam("organizationId") String organizationId
    ) {
        OrganizationModel organization = organizationProvider.getOrganizationById(organizationId).orElseThrow(() -> new NotFoundException("Organization not found"));
        if (OrganizationModel.MASTER_ID.equals(organization.getId())) {
            throw new BadRequestException("La organización 'master' no puede ser elminada");
        }

        organizationProvider.deleteOrganization(organization);
    }

    @GET
    @Path("/{organizationId}/keys")
    @Produces(MediaType.APPLICATION_JSON)
    public KeysMetadataRepresentation getKeyMetadata(
            @PathParam("organizationId") final String organizationId
    ) {
        OrganizationModel organization = organizationProvider.getOrganizationById(organizationId).orElseThrow(() -> new NotFoundException("Organization not found"));

        KeysMetadataRepresentation keys = new KeysMetadataRepresentation();
        keys.setKeys(new LinkedList<>());
        keys.setActive(new HashMap<>());

        for (KeyWrapper key : keystore.getKeys(organization)) {
            KeysMetadataRepresentation.KeyMetadataRepresentation r = new KeysMetadataRepresentation.KeyMetadataRepresentation();
            r.setProviderId(key.getProviderId());
            r.setProviderPriority(key.getProviderPriority());
            r.setKid(key.getKid());
            r.setStatus(key.getStatus() != null ? key.getStatus().name() : null);
            r.setType(key.getType());
            r.setAlgorithm(key.getAlgorithm());
            r.setPublicKey(key.getPublicKey() != null ? PemUtils.encodeKey(key.getPublicKey()) : null);
            r.setCertificate(key.getCertificate() != null ? PemUtils.encodeCertificate(key.getCertificate()) : null);
            keys.getKeys().add(r);

            if (key.getStatus().isActive()) {
                if (!keys.getActive().containsKey(key.getAlgorithm())) {
                    keys.getActive().put(key.getAlgorithm(), key.getKid());
                }
            }
        }

        return keys;
    }

    @GET
    @Path("/{organizationId}/components")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ComponentRepresentation> getComponents(
            @PathParam("organizationId") final String organizationId,
            @QueryParam("parent") String parent,
            @QueryParam("type") String type,
            @QueryParam("name") String name
    ) {
        OrganizationModel organization = organizationProvider.getOrganizationById(organizationId).orElseThrow(() -> new NotFoundException("Organization not found"));

        List<ComponentModel> components;
        if (parent == null && type == null) {
            components = componentProvider.getComponents(organization);
        } else if (type == null) {
            components = componentProvider.getComponents(organization, parent);
        } else if (parent == null) {
            components = componentProvider.getComponents(organization, organization.getId(), type);
        } else {
            components = componentProvider.getComponents(organization, parent, type);
        }
        List<ComponentRepresentation> reps = new LinkedList<>();
        for (ComponentModel component : components) {
            if (name != null && !name.equals(component.getName())) continue;
            ComponentRepresentation rep = null;
            try {
                rep = ModelToRepresentation.toRepresentation(component, false, componentUtil);
            } catch (Exception e) {
                logger.error("Failed to get component list for component model" + component.getName() + "of organization " + organization.getName());
                rep = ModelToRepresentation.toRepresentationWithoutConfig(component);
            }
            reps.add(rep);
        }
        return reps;
    }

    @POST
    @Path("/{organizationId}/components")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createComponent(
            @PathParam("organizationId") final String organizationId, ComponentRepresentation rep
    ) {
        OrganizationModel organization = organizationProvider.getOrganizationById(organizationId).orElseThrow(() -> new NotFoundException("Organization not found"));

        try {
            ComponentModel model = RepresentationToModel.toModel(rep);
            if (model.getParentId() == null) model.setParentId(organization.getId());

            model = componentProvider.addComponentModel(organization, model);

            return Response.created(uriInfo.getAbsolutePathBuilder().path(model.getId()).build()).build();
        } catch (ComponentValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @GET
    @Path("/{organizationId}/components/{componentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentRepresentation getComponent(
            @PathParam("organizationId") final String organizationId,
            @PathParam("componentId") String componentId
    ) {
        OrganizationModel organization = organizationProvider.getOrganizationById(organizationId).orElseThrow(() -> new NotFoundException("Organization not found"));

        ComponentModel model = componentProvider.getComponent(organization, componentId);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }

        return ModelToRepresentation.toRepresentation(model, false, componentUtil);
    }

    @PUT
    @Path("/{organizationId}/components/{componentId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateComponent(
            @PathParam("organizationId") final String organizationId,
            @PathParam("componentId") String componentId,
            ComponentRepresentation rep
    ) {
        OrganizationModel organization = organizationProvider.getOrganizationById(organizationId).orElseThrow(() -> new NotFoundException("Organization not found"));

        try {
            ComponentModel model = componentProvider.getComponent(organization, componentId);
            if (model == null) {
                throw new NotFoundException("Could not find component");
            }
            RepresentationToModel.updateComponent(rep, model, false, componentUtil);

            componentProvider.updateComponent(organization, model);
            return Response.noContent().build();
        } catch (ComponentValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @DELETE
    @Path("/{organizationId}/components/{componentId}")
    public void removeComponent(
            @PathParam("organizationId") final String organizationId,
            @PathParam("componentId") String componentId
    ) {
        OrganizationModel organization = organizationProvider.getOrganizationById(organizationId).orElseThrow(() -> new NotFoundException("Organization not found"));

        ComponentModel model = componentProvider.getComponent(organization, componentId);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }

        componentProvider.removeComponent(organization, model);
    }

}
