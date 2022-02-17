/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.NBSourceRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * NB source Rest resource.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 *
 */
@RelNameDSpaceResource(NBSourceRest.NAME)
public class NBSourceResource extends DSpaceResource<NBSourceRest> {

    public NBSourceResource(NBSourceRest data, Utils utils) {
        super(data, utils);
    }

}