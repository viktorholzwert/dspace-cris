/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SubscriptionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.Utils;
import org.dspace.eperson.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from Entity CrisLayoutTab to the REST data model
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class SubscriptionConverter implements DSpaceConverter<Subscription, SubscriptionRest> {
    @Autowired
    protected Utils utils;
    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#convert
     * (java.lang.Object, org.dspace.app.rest.projection.Projection)
     */
    @Autowired
    private ConverterService converter;
    @Override
    public SubscriptionRest convert(Subscription subscription, Projection projection) {
        SubscriptionRest rest = new SubscriptionRest();
        rest.setId(subscription.getID());
        rest.setSubscriptionParameterList(subscription.getSubscriptionParameterList());
        rest.setType(subscription.getType());
        return rest;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#getModelClass()
     */
    @Override
    public Class<Subscription> getModelClass() {
        return Subscription.class;
    }

}