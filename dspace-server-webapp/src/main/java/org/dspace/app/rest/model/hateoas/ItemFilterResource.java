/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.ItemFilterRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * ItemFilter Rest HAL Resource. The HAL Resource wraps the REST Resource adding
 * support for the links and embedded resources
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@RelNameDSpaceResource(ItemFilterRest.NAME)
public class ItemFilterResource extends DSpaceResource<ItemFilterRest> {
    public ItemFilterResource(ItemFilterRest data, Utils utils) {
        super(data, utils);
    }
}
